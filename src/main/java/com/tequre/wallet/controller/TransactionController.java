package com.tequre.wallet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tequre.wallet.data.Agent;
import com.tequre.wallet.data.FailedTransaction;
import com.tequre.wallet.data.ResyncStatus;
import com.tequre.wallet.data.ServiceMessage;
import com.tequre.wallet.data.Transaction;
import com.tequre.wallet.data.Wallet;
import com.tequre.wallet.enums.AgentStatus;
import com.tequre.wallet.enums.Discom;
import com.tequre.wallet.enums.EventStatus;
import com.tequre.wallet.enums.PaymentType;
import com.tequre.wallet.enums.SourceType;
import com.tequre.wallet.enums.TransactionState;
import com.tequre.wallet.enums.TransactionType;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.repository.EventRepository;
import com.tequre.wallet.repository.FailedTransactionRepository;
import com.tequre.wallet.repository.TransactionRepository;
import com.tequre.wallet.repository.WalletRepository;
import com.tequre.wallet.request.BackfillPaymentTransaction;
import com.tequre.wallet.request.CreditTransaction;
import com.tequre.wallet.request.DebitTransaction;
import com.tequre.wallet.request.Page;
import com.tequre.wallet.request.PaymentTransaction;
import com.tequre.wallet.request.PaymentTransactionRequest;
import com.tequre.wallet.request.TransactionCancelRequest;
import com.tequre.wallet.request.TransactionRollbackRequest;
import com.tequre.wallet.request.TransactionUpdateRequest;
import com.tequre.wallet.request.WalletTransaction;
import com.tequre.wallet.request.WalletTransactionRequest;
import com.tequre.wallet.response.AcceptedResponse;
import com.tequre.wallet.response.WalletResponse;
import com.tequre.wallet.response.report.BlockchainWalletEntry;
import com.tequre.wallet.service.BlockchainService;
import com.tequre.wallet.service.BlockchainSyncService;
import com.tequre.wallet.service.SequenceGeneratorService;
import com.tequre.wallet.service.WalletService;
import com.tequre.wallet.service.producer.PaymentNonRapdrpStreamService;
import com.tequre.wallet.service.producer.PaymentRapdrpStreamService;
import com.tequre.wallet.utils.CommonUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static com.tequre.wallet.utils.Constants.UPPCL_VAN;

@RestController
@RequestMapping("/v1/transaction")
public class TransactionController {

    private final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

    @Autowired
    private PaymentNonRapdrpStreamService paymentNonRapdrpStreamService;

    @Autowired
    private PaymentRapdrpStreamService paymentRapdrpStreamService;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private FailedTransactionRepository failedTransactionRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private Gson gson;

    @Autowired
    private BlockchainSyncService blockchainSyncService;

    @Autowired
    private BlockchainService blockchainService;
    
    
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @RequestMapping(value = "/credit", method = RequestMethod.POST)
    public ResponseEntity<?> credit(@RequestBody CreditTransaction creditTransaction) {
        Optional<Agent> optionalAgent = agentRepository.findById(creditTransaction.getAgentId());
        if (!optionalAgent.isPresent()) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("No wallet exist with id " + creditTransaction.getAgentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        if (creditTransaction.getSourceType() == SourceType.BANK && creditTransaction.getTransactionId() == null) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("No transaction id found for recharge wallet for agent " + creditTransaction.getAgentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        if (isTransactionDuplicate(creditTransaction.getTransactionId(), creditTransaction.getSourceType())) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Payment for transaction id " + creditTransaction.getTransactionId() +
                    " is already committed to agent " + creditTransaction.getAgentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Event event = CommonUtils.createEvent(creditTransaction);
        Agent agent = null;
        try {
            String agentId = creditTransaction.getAgentId();
            agent = getAgent(agentId);
            String transactionId = walletService.deposit(agent.getVan(), creditTransaction.getAmount(), agent.getId(), agent.getAgentType().name(),
                    creditTransaction.getSourceType().name(), creditTransaction.getTransactionId(), creditTransaction.getWalletId(), agent.getAgencyId());
            event.setResponse(createWalletResponse(transactionId, null));
            event.setStatus(EventStatus.SUCCESS);
            logger.info("Credit Transaction Success: " + transactionId);
        } catch (Throwable th) {
            logger.error("Exception occurred in credit operation.", th);
            event.setStatus(EventStatus.FAILED);
            if (th instanceof HttpStatusCodeException) {
                HttpStatusCodeException e = (HttpStatusCodeException) th;
                int statusCode = e.getStatusCode().value();
                String reason = e.getResponseBodyAsString();
                if (reason == null) {
                    reason = e.getMessage();
                }
                event.setReason("Http Code: " + statusCode + " Response: " + reason);
            } else {
                event.setReason(th.getMessage());
            }
            if (agent != null) {
                event.setResponse(createErrorWalletResponse(agent));
            }
            FailedTransaction failedTransaction = new FailedTransaction();
            failedTransaction.setVan(agent != null ? agent.getVan() : null);
            failedTransaction.setPayload(creditTransaction);
            failedTransactionRepository.save(failedTransaction);
        } finally {
            eventRepository.save(event);
            AcceptedResponse response = CommonUtils.eventResponse(event.getId());
            logger.info("Event Processed Successful " + event);
            return ResponseEntity.accepted().body(response);
        }
    }

    @RequestMapping(value = "/debit", method = RequestMethod.POST)
    public ResponseEntity<?> debit(@RequestBody DebitTransaction debitTransaction) {
        Optional<Agent> optionalAgent = agentRepository.findById(debitTransaction.getAgentId());
        if (!optionalAgent.isPresent()) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("No wallet exist with id " + debitTransaction.getAgentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Agent agent = optionalAgent.get();
        if (agent.getStatus() != AgentStatus.ACTIVE) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("The agent is not ACTIVE " + debitTransaction.getAgentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        
        /*
         * Changed By Hotam Singh 
         * Added if condition
         * */
        if(debitTransaction.getSourceType().name() == "COMMISSION") {
        	debitTransaction.setSourceType(debitTransaction.getSourceType());
        } else {
        	debitTransaction.setSourceType(SourceType.BILL);
        }

        //  debitTransaction.setSourceType(SourceType.BILL);
        Event event = CommonUtils.createEvent(debitTransaction);
        try {
            if (!isTransactionDuplicate(debitTransaction.getTransactionId(), debitTransaction.getSourceType())) {
                String transactionId = walletService.withdraw(agent.getVan(), debitTransaction.getAmount(), agent.getId(), agent.getAgentType().name(),
                        debitTransaction.getSourceType().name(), debitTransaction.getTransactionId(), debitTransaction.getWalletId(), agent.getAgencyId());
                event.setResponse(createWalletResponse(transactionId, null));
                event.setStatus(EventStatus.SUCCESS);
                logger.info("Debit Transaction Success: " + transactionId);
            } else {
                logger.info("Transaction Already Exists : " + debitTransaction.getTransactionId());
            }
        } catch (Throwable th) {
            logger.error("Exception occurred in debit operation.", th);
            event.setStatus(EventStatus.FAILED);
            if (th instanceof HttpStatusCodeException) {
                HttpStatusCodeException e = (HttpStatusCodeException) th;
                int statusCode = e.getStatusCode().value();
                String reason = e.getResponseBodyAsString();
                if (reason == null) {
                    reason = e.getMessage();
                }
                event.setReason("Http Code: " + statusCode + " Response: " + reason);
            } else {
                event.setReason(th.getMessage());
            }
            if (agent != null) {
                event.setResponse(createErrorWalletResponse(agent));
            }
            FailedTransaction failedTransaction = new FailedTransaction();
            failedTransaction.setVan(agent != null ? agent.getVan() : null);
            failedTransaction.setPayload(debitTransaction);
            failedTransactionRepository.save(failedTransaction);
        } finally {
            eventRepository.save(event);
            AcceptedResponse response = CommonUtils.eventResponse(event.getId());
            logger.info("Event Processed Successful " + event);
            return ResponseEntity.accepted().body(response);
        }
    }
    
    /*private boolean isEventDuplicate(String billId, SourceType sourceType) {
    	Query query = new Query();
    	query.addCriteria(Criteria.where("payload.billId").is(billId));
    	query.addCriteria(Criteria.where("payload.sourceType").is(sourceType.name()));
    	return mongoTemplate.exists(query, Event.class);
	}*/

	/**
	 * Changed By Hotam
	 * Added new method to prevent duplicate entry in event collection
	 * 
	 */
	
	private boolean isPaymentAllowed(String billId, String consumerId) throws ParseException {
	    LocalDate localDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
	    int length = localDate.lengthOfMonth();
	    int year = localDate.getYear();
	    int month = localDate.getMonthValue();
	    int date = localDate.getDayOfMonth();
	    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("IST"));
	    // Date startTime = DATE_FORMAT.parse(year + "-" + month + "-" + 1);
	    // Date endTime = DATE_FORMAT.parse(year + "-" + month + "-" + length);
	    Date startTime = DATE_FORMAT.parse(year + "-" + month + "-" + date);
        Date endTime = DATE_FORMAT.parse(year + "-" + month + "-" + (date+1));
	    Query query = new Query();
	    List<Criteria> queryConditions = new ArrayList<>();
	    if (startTime != null || endTime != null) {
	        Criteria range = Criteria.where("date");
	        if (startTime != null) {
	            range.gte(startTime);
	        }
	        if (endTime != null) {
	            range.lte(endTime);
	        }
	        queryConditions.add(range);
	    }
	    queryConditions.add(Criteria.where("payload.billId").is(billId));
	    queryConditions.add(Criteria.where("payload.consumerAccountId").is(consumerId));
	    ArrayList<String> eventStatus = new ArrayList(); 
	    eventStatus.add(EventStatus.IN_QUEUE.name());
	    eventStatus.add(EventStatus.SUCCESS.name());
	    queryConditions.add(Criteria.where("status").in(eventStatus));
	    Criteria criteria = new Criteria();
	    criteria.andOperator(queryConditions.stream().toArray(Criteria[]::new));
	    query.addCriteria(criteria);
	    List<Event> transactions = mongoTemplate.find(query, Event.class);
	    // logger.info("transactions: " + transactions);
	    return CollectionUtils.isEmpty(transactions);
	}

    @RequestMapping(value = "/payment", method = RequestMethod.POST)
    public ResponseEntity<?> payment(@RequestBody PaymentTransactionRequest paymentTransactionRequest) throws ParseException {
        /*if(paymentTransactionRequest.getAgencyType() == null || paymentTransactionRequest.getAgencyType() == "") {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("agencyType is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }*/
        if(paymentTransactionRequest.getAmount() == null || paymentTransactionRequest.getAmount() < 1) {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("amount is missing or invalid");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        if(paymentTransactionRequest.getBillId() == null || paymentTransactionRequest.getBillId() == "") {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("billId is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        if(paymentTransactionRequest.getConsumerAccountId() == null || paymentTransactionRequest.getConsumerAccountId() == "") {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("consumerId is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        /*if(paymentTransactionRequest.getConsumerName() == null || paymentTransactionRequest.getConsumerName() == "") {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("consumerName is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }*/
        
        if(paymentTransactionRequest.getDiscom() == null || paymentTransactionRequest.getDiscom() == "") {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("discomName is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        if(paymentTransactionRequest.getDivision() == null || paymentTransactionRequest.getDivision() == "") {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("divisionName is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        if(paymentTransactionRequest.getDivisionCode() == null || paymentTransactionRequest.getDivisionCode() == "") {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("divisionCode is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        if(paymentTransactionRequest.getVanNo() == null || paymentTransactionRequest.getVanNo() == "") {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("vanNo is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
    	Optional<Agent> optionalAgent = agentRepository.findById(paymentTransactionRequest.getAgentId());
        if (!optionalAgent.isPresent()) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("No wallet exist with id " + paymentTransactionRequest.getAgentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Agent agent = optionalAgent.get();
        if (agent.getStatus() != AgentStatus.ACTIVE) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("The agent is not ACTIVE " + paymentTransactionRequest.getAgentId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        if (!agent.getVan().equalsIgnoreCase(paymentTransactionRequest.getVanNo())) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Agent Id and van number do not match.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Double balance = walletService.getBalance(agent.getVan());
        if (balance < paymentTransactionRequest.getAmount()) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Not enough funds in wallet " + agent.getVan());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        if (paymentTransactionRequest.getType() == PaymentType.NON_RAPDRP) {
            paymentTransactionRequest.setSourceType(SourceType.NON_RAPDRP);
        } else {
            paymentTransactionRequest.setSourceType(SourceType.RAPDRP);
        }
        /*
         * Changed By Hotam
         * Added condition to check if transaction to be processed further.
         * */

        /*if(isEventDuplicate(paymentTransactionRequest.getBillId(), paymentTransactionRequest.getSourceType())) {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("A payment was already initiated for " + paymentTransactionRequest.getBillId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }*/
        
        if(!isPaymentAllowed(paymentTransactionRequest.getBillId(), paymentTransactionRequest.getConsumerAccountId())) {
        	ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("A payment was already done for " + paymentTransactionRequest.getBillId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        /*       
        paymentTransactionRequest.setDiscom(paymentTransactionRequest.getDiscom().split("-")[0]);

        if (Discom.PUVNL.name().equals(paymentTransactionRequest.getDiscom())) {
            paymentTransactionRequest.setDiscom(Discom.PUVVNL.name());
        } 
		if (paymentTransactionRequest.getDivisionCode() == null || 
				paymentTransactionRequest.getDivisionCode().trim().length() <= 0) {
			ServiceMessage serviceMessage = new ServiceMessage();
			serviceMessage.setMessage("Division Code cannot be null or empty! ");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
		}        
		*/ 
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setType(paymentTransactionRequest.getType());
        paymentTransaction.setSourceType(paymentTransactionRequest.getSourceType());
        paymentTransaction.setAmount(paymentTransactionRequest.getAmount());
        paymentTransaction.setBillId(paymentTransactionRequest.getBillId());
        paymentTransaction.setAgentId(paymentTransactionRequest.getAgentId());
        paymentTransaction.setWalletId(paymentTransactionRequest.getWalletId());
        paymentTransaction.setConsumerAccountId(paymentTransactionRequest.getConsumerAccountId());
        paymentTransaction.setConsumerName(paymentTransactionRequest.getConsumerName());
        paymentTransaction.setDiscom(paymentTransactionRequest.getDiscom());
        paymentTransaction.setDivision(paymentTransactionRequest.getDivision());
        paymentTransaction.setMobile(paymentTransactionRequest.getMobile());
        paymentTransaction.setVanNo(paymentTransactionRequest.getVanNo());
        paymentTransaction.setReferenceTransactionId(paymentTransactionRequest.getReferenceTransactionId());
        paymentTransaction.setDivisionCode(paymentTransactionRequest.getDivisionCode());
        paymentTransaction.setTransactionId(sequenceGeneratorService.generateTransactionId());
        paymentTransaction.setAgencyType(paymentTransactionRequest.getAgencyType());
        paymentTransaction.setConnectionType(paymentTransactionRequest.getConnectionType());
        paymentTransaction.setAgencyId(agent.getAgencyId());
        Event event = CommonUtils.createEvent(paymentTransaction);
        if (paymentTransactionRequest.getSourceType() == SourceType.NON_RAPDRP) {
            paymentNonRapdrpStreamService.produceEvent(event);
        } else {
            paymentRapdrpStreamService.produceEvent(event);
        }
        AcceptedResponse response = CommonUtils.eventResponse(event.getId());
        return ResponseEntity.accepted().body(response);
    }

    @RequestMapping(value = "/rollback", method = RequestMethod.POST)
    public ResponseEntity<?> rollback(@Valid @RequestBody TransactionRollbackRequest transactionRollbackRequest) {
        if (transactionRollbackRequest.getTransactionId() == null ||
                transactionRollbackRequest.getRemarks() == null) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Invalid Request parameters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        List<Transaction> transactions = findByExternalTransactionId(transactionRollbackRequest.getTransactionId());
        if (CollectionUtils.isEmpty(transactions)) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("No transactions exist with id " + transactionRollbackRequest.getTransactionId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Optional<Transaction> nonSuccessTransaction = transactions.stream()
                .filter(transaction -> TransactionState.SUCCESS != transaction.getTransactionState())
                .findAny();
        if (nonSuccessTransaction.isPresent()) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Operation is only allowed for successful transactions.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Long txnTime = Instant.now().toEpochMilli();
        transactions.forEach(transaction -> {
            String transactionId = transaction.getId();
            Transaction updatedTransaction = reverse(transaction, txnTime, transactionRollbackRequest.getTransactionId());
            Optional<Transaction> optionOldTransaction = transactionRepository.findById(transactionId);
            if (optionOldTransaction.isPresent()) {
                Transaction oldTransaction = optionOldTransaction.get();
                oldTransaction.setTransactionState(TransactionState.ROLLBACK);
                oldTransaction.setRemarks(transactionRollbackRequest.getRemarks());
                oldTransaction.setRevertedTransactionId(updatedTransaction.getId());
                transactionRepository.save(oldTransaction);
            }
        });
        transactions = findByExternalTransactionId(transactionRollbackRequest.getTransactionId());
        transactions.sort(Comparator.comparing(Transaction::getTransactionTime).reversed());
        return ResponseEntity.ok(transactions);
    }

    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    public ResponseEntity<?> cancel(@Valid @RequestBody TransactionCancelRequest transactionCancelRequest) {
        if (transactionCancelRequest.getTransactionId() == null ||
                transactionCancelRequest.getRemarks() == null) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Invalid Request parameters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        List<Transaction> transactions = findByExternalTransactionId(transactionCancelRequest.getTransactionId());
        if (CollectionUtils.isEmpty(transactions)) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("No transactions exist with id " + transactionCancelRequest.getTransactionId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Optional<Transaction> nonSuccessTransaction = transactions.stream()
                .filter(transaction -> TransactionState.SUCCESS != transaction.getTransactionState())
                .findAny();
        if (nonSuccessTransaction.isPresent()) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Operation is only allowed for successful transactions.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Long txnTime = Instant.now().toEpochMilli();
        transactions.forEach(transaction -> {
            String transactionId = transaction.getId();
            Transaction updatedTransaction = reverse(transaction, txnTime, transactionCancelRequest.getTransactionId());
            Optional<Transaction> optionOldTransaction = transactionRepository.findById(transactionId);
            if (optionOldTransaction.isPresent()) {
                Transaction oldTransaction = optionOldTransaction.get();
                oldTransaction.setTransactionState(TransactionState.CANCELLED);
                oldTransaction.setRemarks(transactionCancelRequest.getRemarks());
                oldTransaction.setRevertedTransactionId(updatedTransaction.getId());
                transactionRepository.save(oldTransaction);
            }
        });
        transactions = findByExternalTransactionId(transactionCancelRequest.getTransactionId());
        transactions.sort(Comparator.comparing(Transaction::getTransactionTime).reversed());
        return ResponseEntity.ok(transactions);
    }

    @Deprecated
    @RequestMapping(value = "/replay", method = RequestMethod.POST)
    public ResponseEntity<?> replay(@RequestParam(value = "date") String date,
                                    @RequestParam(value = "PaymentType") PaymentType paymentType) throws ParseException {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is("IN_QUEUE"));
        query.addCriteria(Criteria.where("type").is("PaymentTransaction"));
        query.addCriteria(Criteria.where("payload.sourceType").is(paymentType));
        Criteria range = Criteria.where("createdAt");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date d = dateFormat.parse(date);
        range.gte(d.toInstant().toEpochMilli());
        range.lte(d.toInstant().plusSeconds(24 * 60 * 60).toEpochMilli());
        query.addCriteria(range);
        List<Event> events = mongoTemplate.find(query, Event.class);
        if (events != null) {
            events.forEach(event -> {
                if (paymentType == PaymentType.NON_RAPDRP) {
                    paymentNonRapdrpStreamService.replayEvent(event);
                } else {
                    paymentRapdrpStreamService.replayEvent(event);
                }
            });
        }
        return ResponseEntity.ok().body(events);
    }

    @Deprecated
    @RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart(value = "file") MultipartFile file) {
        try {
            persist(file.getInputStream());
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.warn("Failed to upload CSV data.", e);
            ServiceMessage serviceError = new ServiceMessage();
            serviceError.setMessage("Failed to upload CSV data " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceError);
        }
    }

    @Deprecated
    @RequestMapping(value = "/uploadWallet", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadWallet(@RequestPart(value = "file") MultipartFile file) {
        try {
            persistWallet(file.getInputStream());
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.warn("Failed to upload CSV data.", e);
            ServiceMessage serviceError = new ServiceMessage();
            serviceError.setMessage("Failed to upload CSV data " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceError);
        }
    }

    @RequestMapping(value = "/wallet", method = RequestMethod.POST)
    public ResponseEntity<?> walletTransaction(@RequestBody WalletTransactionRequest walletTransactionRequest) {
        walletTransactionRequest.setSourceType(SourceType.WALLET);
        Optional<Agent> optionalSourceAgent = agentRepository.findById(walletTransactionRequest.getSourceAgentId());
        if (!optionalSourceAgent.isPresent()) {
            throw new IllegalArgumentException("No wallet exist with id " + walletTransactionRequest.getSourceAgentId());
        }
        Optional<Agent> optionalDestinationAgent = agentRepository.findById(walletTransactionRequest.getDestinationAgentId());
        if (!optionalDestinationAgent.isPresent()) {
            throw new IllegalArgumentException("No wallet exist with id " + walletTransactionRequest.getDestinationAgentId());
        }
        Agent srcAgent = optionalSourceAgent.get();
        if (srcAgent.getStatus() != AgentStatus.ACTIVE) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("The agent is not ACTIVE " + srcAgent.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        Agent destAgent = optionalDestinationAgent.get();
        if (destAgent.getStatus() != AgentStatus.ACTIVE) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("The agent is not ACTIVE " + destAgent.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
        }
        double balance = walletService.getBalance(srcAgent.getVan());
        if (balance >= walletTransactionRequest.getAmount()) {
            WalletTransaction walletTransaction = new WalletTransaction();
            walletTransaction.setSourceType(SourceType.WALLET);
            walletTransaction.setAmount(walletTransactionRequest.getAmount());
            walletTransaction.setDestinationAgentId(walletTransactionRequest.getDestinationAgentId());
            walletTransaction.setSourceAgentId(walletTransactionRequest.getSourceAgentId());
            walletTransaction.setTransactionId(CommonUtils.generateWalletTransferTransactionId());
            Event event = CommonUtils.createEvent(walletTransaction);
            Agent sourceAgent = null;
            try {
                if (!isTransactionDuplicate(walletTransaction.getTransactionId(), walletTransaction.getSourceType())) {
                    sourceAgent = getAgent(walletTransaction.getSourceAgentId());
                    Agent destinationAgent = getAgent(walletTransaction.getDestinationAgentId());
                    String transactionId = walletService.transfer(walletTransaction.getTransactionId(), sourceAgent.getVan(), destinationAgent.getVan(), walletTransaction.getAmount(),
                            sourceAgent.getId(), sourceAgent.getAgentType().name(), walletTransaction.getSourceType().name(),
                            walletTransaction.getTransactionId(), null, null, null, null, null, null, null,
                            sourceAgent.getAgencyId(), destinationAgent.getAgencyId(), null, null, null, null, null,null, null);
                    event.setResponse(createWalletResponse(transactionId, null));
                    event.setStatus(EventStatus.SUCCESS);
                    logger.info("Wallet Transaction Success: " + transactionId);
                } else {
                    logger.info("Transaction Already Exists : " + walletTransaction.getTransactionId());
                }
            } catch (Throwable th) {
                logger.error("Exception occurred in wallet transfer operation.", th);
                event.setStatus(EventStatus.FAILED);
                if (th instanceof HttpStatusCodeException) {
                    HttpStatusCodeException e = (HttpStatusCodeException) th;
                    int statusCode = e.getStatusCode().value();
                    String reason = e.getResponseBodyAsString();
                    if (reason == null) {
                        reason = e.getMessage();
                    }
                    event.setReason("Http Code: " + statusCode + " Response: " + reason);
                } else {
                    event.setReason(th.getMessage());
                }
                event.setResponse(createErrorWalletResponse(sourceAgent));
                FailedTransaction failedTransaction = new FailedTransaction();
                failedTransaction.setVan(sourceAgent != null ? sourceAgent.getVan() : null);
                failedTransaction.setPayload(walletTransaction);
                failedTransactionRepository.save(failedTransaction);
            } finally {
                eventRepository.save(event);
                logger.info("Event Processed Successful " + event);
            }
            AcceptedResponse response = CommonUtils.eventResponse(event.getId());
            return ResponseEntity.accepted().body(response);
        } else {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("Not enough funds in wallet " + srcAgent.getVan());
            return ResponseEntity.badRequest().body(serviceMessage);
        }
    }

    @Deprecated
    @RequestMapping(value = "/state", method = RequestMethod.GET)
    public ResponseEntity<?> getTransactionState(@RequestParam(value = "van") String van,
                                                 @RequestParam(value = "type", required = false) String type,
                                                 @RequestParam(value = "status", required = false) EventStatus status,
                                                 @RequestParam(value = "startTime", required = false) Long startTime,
                                                 @RequestParam(value = "endTime", required = false) Long endTime,
                                                 @RequestParam(value = "pageSize", required = false) String pageSize,
                                                 @RequestParam(value = "nextPageToken", required = false) String nextPageToken) throws IOException {

        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("createdAt").descending());

        Query query = new Query();
        if (van != null) {
            query.addCriteria(Criteria.where("response.vanId").is(van));
        }
        if (type != null) {
            query.addCriteria(Criteria.where("type").is(type));
        }
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("createdAt");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            query.addCriteria(range);
        }
        query.with(pageableRequest);
        List<Event> events = mongoTemplate.find(query, Event.class);
        JsonElement element;
        JsonObject object = new JsonObject();
        if (events != null) {
            element = gson.toJsonTree(events, new TypeToken<List<Event>>() {
            }.getType());
            JsonArray array = element.getAsJsonArray();
            if (p.getSize() - array.size() == 0) {
                object.addProperty("nextPageToken", CommonUtils.nextPageToken(gson, p));
            }
            object.addProperty("recordCount", array.size());
            object.add("result", array);
        }
        JsonNode jsonNode = CommonUtils.getMapper().readTree(object.toString());
        return ResponseEntity.status(HttpStatus.OK).body(jsonNode);
    }

    @Deprecated
    @RequestMapping(value = "/migration/{externalTransactionId}", method = RequestMethod.PUT)
    public ResponseEntity<?> migrateAddDataToTransaction(@PathVariable String externalTransactionId,
                                                         @RequestBody TransactionUpdateRequest transactionUpdateRequest) {
        List<Transaction> transactions = findByExternalTransactionId(externalTransactionId);
        if (transactions != null && !transactions.isEmpty()) {
            for (Transaction transaction : transactions) {
                transaction.setConsumerId(transactionUpdateRequest.getConsumerId());
                transaction.setBillId(transactionUpdateRequest.getBillId());
                transactionRepository.save(transaction);
            }
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Deprecated
    @RequestMapping(value = "/backfillPayment", method = RequestMethod.POST)
    public ResponseEntity<?> backfillPayment(@RequestBody BackfillPaymentTransaction paymentTransaction) {
        Agent agent = getAgent(paymentTransaction.getAgentId());
        String txnId = paymentTransaction.getTransactionId();
        String transactionId = walletService.transfer(txnId, agent.getVan(), UPPCL_VAN, paymentTransaction.getAmount(), agent.getId(), agent.getAgentType().name(),
                paymentTransaction.getSourceType().name(), paymentTransaction.getBillId(), paymentTransaction.getConsumerAccountId(), paymentTransaction.getWalletId(),
                paymentTransaction.getDivision(), paymentTransaction.getDiscom(), paymentTransaction.getReferenceTransactionId(), paymentTransaction.getDivisionCode(),
                paymentTransaction.getMobile(), agent.getAgencyId(), null, paymentTransaction.getAgencyVan(), null, null, null, null, paymentTransaction.getAgencyType(),
                paymentTransaction.getConnectionType());
        return ResponseEntity.ok(transactionId);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getTransaction(@PathVariable String id) {
        Optional<Transaction> userOptional = transactionRepository.findById(id);
        if (userOptional.isPresent()) {
            Transaction transaction = userOptional.get();
            return ResponseEntity.ok(transaction);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/resync", method = RequestMethod.POST)
    public ResponseEntity<?> resync(@RequestParam(value = "fromDate") String fromDate,
                                    @RequestParam(value = "toDate") String toDate,
                                    @RequestParam(value = "forced", required = false, defaultValue = "false") boolean forced) {
        ResyncStatus resyncStatus = blockchainSyncService.resync(fromDate, toDate, forced);
        return ResponseEntity.ok().body(resyncStatus);
    }

    @RequestMapping(value = "/walletSyncStatus", method = RequestMethod.GET)
    public ResponseEntity<?> walletSyncStatus() {
        List<Wallet> wallets = walletRepository.findAll();
        List<BlockchainWalletEntry> entries = wallets.stream().map(wallet -> {
            Optional<Wallet> blockchainWalletOpt = blockchainService.getWalletByVan(wallet.getId());
            BlockchainWalletEntry blockchainWalletEntry = new BlockchainWalletEntry();
            blockchainWalletEntry.setBalance(wallet.getBalance());
            blockchainWalletOpt.ifPresent(value -> blockchainWalletEntry.setBalance(value.getBalance()));
            if (blockchainWalletEntry.getBlockchainBalance() == null
                    || (blockchainWalletEntry.getBalance().doubleValue() != blockchainWalletEntry.getBlockchainBalance().doubleValue())) {
                return blockchainWalletEntry;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return ResponseEntity.ok(entries);
    }

    @RequestMapping(value = "/transactionSync", method = RequestMethod.GET)
    public ResponseEntity<?> transactionSync(@RequestParam(value = "toDate") String toDate) throws ParseException {
        Query query = new Query();
        query.addCriteria(Criteria.where("blockchainCommit").is(false));
        Criteria range = Criteria.where("transactionTime");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("IST"));
        Date to = dateFormat.parse(toDate);
        range.lte(to.toInstant().toEpochMilli());
        query.addCriteria(range);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        transactions.sort(Comparator.comparing(Transaction::getTransactionTime));
        logger.info("Transaction Count: {}", transactions.size());
        List<Transaction> updateTxn = new ArrayList<>();
        transactions.forEach(txn -> {
            Optional<Transaction> optTxn = blockchainService.getTransactionById(txn.getId());
            if (optTxn.isPresent()) {
                txn.setBlockchainCommit(true);
                updateTxn.add(txn);
            }
        });
        transactionRepository.saveAll(updateTxn);
        ResyncStatus resyncStatus = new ResyncStatus();
        resyncStatus.setTotalTransactions(updateTxn.size());
        return ResponseEntity.ok(resyncStatus);
    }

    private Agent getAgent(String id) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            return optionalAgent.get();
        } else {
            throw new IllegalArgumentException("No Agent exist with id " + id);
        }
    }

    private void persist(InputStream is) throws IOException {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            int i = 0;
            for (CSVRecord csvRecord : csvRecords) {
                Map<String, String> csvMap = csvRecord.toMap();
                logger.info("Line: {}", csvMap);
                String externalReferenceId = csvMap.get("externalReferenceId");
                String amount = csvMap.get("Amount");
                String vanNo = csvMap.get("vanNo");
                Transaction transaction = new Transaction();
                transaction.setExternalTransactionId(externalReferenceId);
                transaction.setAmount(Double.parseDouble(amount));
                List<Transaction> transactions = transactionRepository.findAll(Example.of(transaction));
                if (!CollectionUtils.isEmpty(transactions)) {
                    Optional<Wallet> walletOpt = walletRepository.findById(vanNo);
                    Optional<Wallet> uppclWalletOpt = walletRepository.findById(UPPCL_VAN);
                    if (walletOpt.isPresent() && uppclWalletOpt.isPresent()) {
                        Wallet wallet = walletOpt.get();
                        Wallet uppclWallet = uppclWalletOpt.get();
                        wallet.setBalance(wallet.getBalance() + transaction.getAmount());
                        uppclWallet.setBalance(uppclWallet.getBalance() - transaction.getAmount());
                        walletRepository.saveAll(Arrays.asList(wallet, uppclWallet));
                        transactionRepository.deleteAll(transactions);
                        logger.info("Processed Id: {} => externalReferenceId = {}, amount = {}, vanNo = {}", i, externalReferenceId, amount, vanNo);
                    }
                } else {
                    logger.info("Skipped Id: {} => externalReferenceId = {}, amount = {}, vanNo = {}", i, externalReferenceId, amount, vanNo);
                }
                i++;
            }
        }
    }

    private void persistWallet(InputStream is) throws IOException {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            int i = 0;
            for (CSVRecord csvRecord : csvRecords) {
                Map<String, String> csvMap = csvRecord.toMap();
                logger.info("Line: {}", csvMap);
                String referenceId = csvMap.get("referenceId");
                logger.info("referenceId = " + referenceId);
                String amount = csvMap.get("amount");
                String agencyVan = csvMap.get("agencyVan");
                String agentVan = csvMap.get("agentVan");
                Transaction transaction = new Transaction();
                transaction.setExternalTransactionId(referenceId);
                transaction.setAmount(Double.parseDouble(amount));
                List<Transaction> transactions = transactionRepository.findAll(Example.of(transaction));
                if (!CollectionUtils.isEmpty(transactions)) {
                    Optional<Wallet> agencyWalletOpt = walletRepository.findById(agencyVan);
                    Optional<Wallet> agentWalletOpt = walletRepository.findById(agentVan);
                    if (agencyWalletOpt.isPresent() && agentWalletOpt.isPresent()) {
                        Wallet agencyWallet = agencyWalletOpt.get();
                        Wallet agentWallet = agentWalletOpt.get();
                        agencyWallet.setBalance(agencyWallet.getBalance() + transaction.getAmount());
                        agentWallet.setBalance(agentWallet.getBalance() - transaction.getAmount());
                        walletRepository.saveAll(Arrays.asList(agentWallet, agencyWallet));
                        transactionRepository.deleteAll(transactions);
                        logger.info("Processed Id: {} => externalReferenceId = {}, amount = {}, agencyVan = {}, agentVan = {}", i, referenceId, amount, agencyVan, agentVan);
                    }
                } else {
                    logger.info("Skipped Id: {} => externalReferenceId = {}, amount = {}, agencyVan = {}, agentVan = {}", i, referenceId, amount, agencyVan, agentVan);
                }
                i++;
            }
        }
    }

    private List<Transaction> findByExternalTransactionId(String externalTransactionId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("externalTransactionId").is(externalTransactionId));
        return mongoTemplate.find(query, Transaction.class);
    }

    private boolean isTransactionDuplicate(String transactionId, SourceType sourceType) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionType(sourceType.name());
        return transactionRepository.exists(Example.of(transaction));
    }

    private List<WalletResponse> createWalletResponse(String transactionId, String receipt) {
        JsonArray array = walletService.transaction(transactionId);
        logger.info("Transaction Response: " + gson.toJson(array));
        List<WalletResponse> walletResponses = new ArrayList<>();
        if (array != null) {
            int size = array.size();
            for (int i = 0; i < size; i++) {
                JsonElement element = array.get(i);
                logger.info("Element Response: " + gson.toJson(element));
                JsonObject object = element.getAsJsonObject();
                WalletResponse walletResponse = new WalletResponse();
                walletResponse.setId(object.get("id") != null ? object.get("id").getAsString() : null);
                walletResponse.setTransactionId(object.get("externalTransactionId") != null ? object.get("externalTransactionId").getAsString() : null);
                walletResponse.setTxnId(object.get("transactionId") != null ? object.get("transactionId").getAsString() : null);
                walletResponse.setTxnType(object.get("transactionType").getAsString() != null ? object.get("transactionType").getAsString() : null);
                walletResponse.setVanId(object.get("vanId").getAsString() != null ? object.get("vanId").getAsString() : null);
                walletResponse.setAmount(object.get("amount") != null ? object.get("amount").getAsDouble() : null);
                walletResponse.setTransactionTime(object.get("transactionTime") != null ? object.get("transactionTime").getAsString() : null);
                walletResponse.setEntityType(object.get("entityType") != null ? object.get("entityType").getAsString() : null);
                walletResponse.setEntityId(object.get("entityId") != null ? object.get("entityId").getAsString() : null);
                walletResponse.setWalletId(object.get("externalId") != null ? object.get("externalId").getAsString() : null);
                walletResponse.setActivity(object.get("activity") != null ? object.get("activity").getAsString() : null);
                walletResponse.setReceiptNo(receipt);
                walletResponses.add(walletResponse);
            }
        }
        return walletResponses;
    }

    private WalletResponse createErrorWalletResponse(Agent agent) {
        WalletResponse walletResponse = new WalletResponse();
        if (agent != null) {
            walletResponse.setVanId(agent.getVan());
        }
        return walletResponse;
    }

    private Transaction reverse(Transaction transaction, Long transactionTime, String remarks) {
        String vanId = transaction.getVanId();
        Optional<Wallet> walletOptional = walletRepository.findById(vanId);
        if (!walletOptional.isPresent()) {
            throw new IllegalStateException("No wallet exist for van " + vanId + " Unable to perform operation");
        }
        Wallet wallet = walletOptional.get();
        Double balance = wallet.getBalance();
        transaction.setBlockchainCommit(false);
        transaction.setTransactionTime(transactionTime);
        transaction.setId(CommonUtils.generateTransactionUUID());
        if (TransactionType.DEBIT.name().equalsIgnoreCase(transaction.getActivity())) {
            transaction.setActivity(TransactionType.CREDIT.name());
            balance = balance + transaction.getAmount();
        } else {
            transaction.setActivity(TransactionType.DEBIT.name());
            balance = balance - transaction.getAmount();
        }
        transaction.setTransactionState(TransactionState.REVERTED);
        transaction.setRemarks(remarks);
        wallet.setBalance(balance);
        walletRepository.save(wallet);
        logger.info("Updated wallet balance for van {}: {}", vanId, balance);
        return transactionRepository.save(transaction);
    }
}
