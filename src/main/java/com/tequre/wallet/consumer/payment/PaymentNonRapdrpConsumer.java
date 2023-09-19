package com.tequre.wallet.consumer.payment;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tequre.wallet.config.IntegrationConfig;
import com.tequre.wallet.data.Agent;
import com.tequre.wallet.data.FailedTransaction;
import com.tequre.wallet.data.Transaction;
import com.tequre.wallet.enums.AgentType;
import com.tequre.wallet.enums.EventStatus;
import com.tequre.wallet.enums.PaymentType;
import com.tequre.wallet.enums.SourceType;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.repository.EventRepository;
import com.tequre.wallet.repository.FailedTransactionRepository;
import com.tequre.wallet.repository.TransactionRepository;
import com.tequre.wallet.request.PaymentPayload;
import com.tequre.wallet.request.PaymentRequest;
import com.tequre.wallet.request.PaymentTransaction;
import com.tequre.wallet.response.WalletResponse;
import com.tequre.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Example;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static com.tequre.wallet.utils.Constants.AUTHORIZATION_HEADER;
import static com.tequre.wallet.utils.Constants.UPPCL_VAN;

@Configuration
public class PaymentNonRapdrpConsumer {

    private final Logger logger = LoggerFactory.getLogger(PaymentNonRapdrpConsumer.class);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private Gson gson;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private FailedTransactionRepository failedTransactionRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IntegrationConfig integrationConfig;

    @Autowired
    private WalletService walletService;

    @StreamListener(PaymentNonRapdrpStream.INBOUND)
    public void consumeEvent(@Payload Event event) {
        logger.info("Payment Consumer: {}", event);
        if (PaymentTransaction.class.getSimpleName().equals(event.getType())) {
            Map<String, Object> map = (Map<String, Object>) event.getPayload();
            JsonElement obj = gson.toJsonTree(map);
            PaymentTransaction paymentTransaction = gson.fromJson(obj, PaymentTransaction.class);
            if (paymentTransaction.getType() == PaymentType.NON_RAPDRP) {
                handlePaymentTransaction(event, paymentTransaction);
            } else {
                logger.error("Invalid Transaction Type: {}", paymentTransaction.getType());
            }
        }
    }

    private void handlePaymentTransaction(Event event, PaymentTransaction paymentTransaction) {
        Agent agent = null;
        logger.info("Payment Transaction : {} Event: {}", paymentTransaction, event);
        try {
            if (!isEventProcessed(event.getId())) {
                if (!isTransactionDuplicate(paymentTransaction.getTransactionId(), paymentTransaction.getSourceType())) {
                    if (isPaymentAllowed(paymentTransaction.getBillId(), paymentTransaction.getConsumerAccountId())) {
                        String agentId = paymentTransaction.getAgentId();
                        agent = getAgent(paymentTransaction.getAgentId());

                        String txnId = paymentTransaction.getTransactionId();
                        // Initiate payment transaction
                        PaymentRequest paymentRequest = new PaymentRequest();
                        paymentRequest.setType(paymentTransaction.getType().getName());
                        PaymentPayload paymentPayload = new PaymentPayload();
                        paymentPayload.setTransactionId(txnId);
                        paymentPayload.setAmount(paymentTransaction.getAmount());
                        paymentPayload.setBillId(paymentTransaction.getBillId());
                        paymentPayload.setAgentId(agentId);
                        paymentPayload.setConsumerAccountId(paymentTransaction.getConsumerAccountId());
                        paymentPayload.setConsumerName(paymentTransaction.getConsumerName());
                        paymentPayload.setVanNo(paymentTransaction.getVanNo());
                        paymentPayload.setDivision(paymentTransaction.getDivision());
                        paymentPayload.setDiscom(paymentTransaction.getDiscom());
                        paymentPayload.setMobile(paymentTransaction.getMobile());
                        paymentPayload.setConnectionType(paymentTransaction.getConnectionType());
                        paymentPayload.setAgencyType(paymentTransaction.getAgencyType());
                        paymentPayload.setReferenceTransactionId(paymentTransaction.getReferenceTransactionId());
                        Agent agency = null;
                        if (agent.getAgentType() == AgentType.AGENCY) {
                            agency = agent;
                        } else {
                            agency = getAgency(agent.getAgencyId());
                        }
                        String agencyVan = agency != null ? agency.getVan() : null;
                        String agencyType = agency != null ? agency.getAgencyType() : null;
                        paymentPayload.setAgencyVan(agencyVan);
                        paymentRequest.setPayload(paymentPayload);

                        Double commissionRate = agency != null ? agency.getCommissionRate() : null;
                        Double gstRate = agency != null ? agency.getGstRate() : null;
                        Double tdsRate = agency != null ? agency.getTdsRate() : null;
                        Double gstTdsRate = agency != null ? agency.getGstTdsRate() : null;

                        String accessToken = getToken();
                        HttpHeaders headers = new HttpHeaders();
                        headers.set(AUTHORIZATION_HEADER, "Bearer " + accessToken);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<PaymentRequest> entity = new HttpEntity<>(paymentRequest, headers);

                        // Bill Post
                        long startTime = System.currentTimeMillis();
                        ResponseEntity<String> response = restTemplate
                                .exchange(integrationConfig.getBillPostServer(), HttpMethod.POST, entity, String.class);
                        logger.info("Bill POST Status: {}, Body: {}, Time: {}" + response.getStatusCode(), response.getBody(),
                                System.currentTimeMillis() - startTime);
                        JsonObject jsonObject = new JsonParser().parse(response.getBody()).getAsJsonObject();
                        String receiptNo = jsonObject.get("receiptNo").getAsString();

                        // Debit from wallet
                        String transactionId = walletService.transfer(txnId, agent.getVan(), UPPCL_VAN, paymentTransaction.getAmount(), agent.getId(), agent.getAgentType().name(),
                                paymentTransaction.getSourceType().name(), paymentTransaction.getBillId(), paymentTransaction.getConsumerAccountId(), paymentTransaction.getWalletId(),
                                paymentTransaction.getDivision(), paymentTransaction.getDiscom(), paymentTransaction.getReferenceTransactionId(), paymentTransaction.getDivisionCode(),
                                paymentTransaction.getMobile(), agent.getAgencyId(), null, agencyVan, commissionRate, gstRate, tdsRate, gstTdsRate, agencyType, paymentTransaction.getConnectionType());
                        event.setResponse(createWalletResponse(transactionId, receiptNo));
                        event.setStatus(EventStatus.SUCCESS);
                        logger.info("Payment Transaction Successful: " + transactionId);
                    } else {
                        logger.info("A payment was already done for this month : " + paymentTransaction.getTransactionId());
                        event.setStatus(EventStatus.FAILED);
                        event.setReason("Bill already paid or in progress for this bill number: " + paymentTransaction.getBillId());
                        FailedTransaction failedTransaction = new FailedTransaction();
                        failedTransaction.setVan(agent != null ? agent.getVan() : null);
                        failedTransaction.setPayload(paymentTransaction);
                        failedTransactionRepository.save(failedTransaction);
                    }
                } else {
                    logger.info("Transaction Already Exists : " + paymentTransaction.getTransactionId());
                    event.setStatus(EventStatus.FAILED);
                    event.setReason("Transaction already exists");
                    FailedTransaction failedTransaction = new FailedTransaction();
                    failedTransaction.setVan(agent != null ? agent.getVan() : null);
                    failedTransaction.setPayload(paymentTransaction);
                    failedTransactionRepository.save(failedTransaction);
                }
            } else {
                logger.info("Event Already Processed : {}", event);
            }
        } catch (Throwable th) {
            logger.error("Exception occurred in bill payment operation.", th);
            Optional<Event> eventOptional = eventRepository.findById(event.getId());
            if (eventOptional.isPresent()) {
                Event oldEvent = eventOptional.get();
                if (oldEvent.getStatus() == EventStatus.SUCCESS) {
                    logger.warn("Duplicate Txn as it was already SUCCESS", paymentTransaction.getTransactionId());
                    return;
                }
            }
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
            failedTransaction.setPayload(paymentTransaction);
            failedTransactionRepository.save(failedTransaction);
        } finally {
            Optional<Event> eventOptional = eventRepository.findById(event.getId());
            if (eventOptional.isPresent()) {
                Event oldEvent = eventOptional.get();
                if (oldEvent.getStatus() != EventStatus.SUCCESS) {
                    eventRepository.save(event);
                } else {
                    logger.info("Event status already in Success: {}", event);
                }
            }
            logger.info("Event Processed Successful: {}", event);
        }
    }

    private boolean isTransactionDuplicate(String transactionId, SourceType sourceType) {
        Query query = new Query();
        query.addCriteria(Criteria.where("transactionId").is(transactionId));
        query.addCriteria(Criteria.where("transactionType").is(sourceType.name()));
        return mongoTemplate.exists(query, Transaction.class);
    }

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
            Criteria range = Criteria.where("transactionTime");
            if (startTime != null) {
                range.gte(startTime.toInstant().toEpochMilli());
            }
            if (endTime != null) {
                range.lte(endTime.toInstant().toEpochMilli());
            }
            queryConditions.add(range);
        }
        
        /*
         * Change By Hotam
         * Bug Fix: Change transactionId to billId
         * */
        
        queryConditions.add(Criteria.where("billId").is(billId));
        queryConditions.add(Criteria.where("consumerId").is(consumerId));
        ArrayList<String> eventStatus = new ArrayList(); 
	    eventStatus.add(EventStatus.IN_QUEUE.name());
	    eventStatus.add(EventStatus.SUCCESS.name());
	    queryConditions.add(Criteria.where("status").in(eventStatus));
        Criteria criteria = new Criteria();
        criteria.andOperator(queryConditions.stream().toArray(Criteria[]::new));
        query.addCriteria(criteria);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        return CollectionUtils.isEmpty(transactions);
    }

    private WalletResponse createErrorWalletResponse(Agent agent) {
        WalletResponse walletResponse = new WalletResponse();
        if (agent != null) {
            walletResponse.setVanId(agent.getVan());
        }
        return walletResponse;
    }

    private Agent getAgent(String id) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            return optionalAgent.get();
        } else {
            throw new IllegalArgumentException("No Agent exist with id " + id);
        }
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

    private String getToken() {
        String key = integrationConfig.getBillPostKey() + ":" + integrationConfig.getBillPostSecret();
        String base64Encoded = Base64.getEncoder().encodeToString(key.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, "Basic " + base64Encoded);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getTokenServer() + "?grant_type=client_credentials",
                        HttpMethod.POST, entity, String.class);
        logger.info("Auth Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonBody = new Gson().fromJson(response.getBody(), JsonObject.class);
            return jsonBody.get("access_token").getAsString();
        } else {
            throw new IllegalStateException("Unable to obtain token");
        }
    }

    private Agent getAgency(String agencyId) {
        if (agencyId != null) {
            Optional<Agent> optionalAgent = agentRepository.findById(agencyId);
            if (optionalAgent.isPresent()) {
                return optionalAgent.get();
            }
        }
        return null;
    }

    private boolean isEventProcessed(String eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();
            return event.getStatus() == EventStatus.SUCCESS || event.getStatus() == EventStatus.FAILED;
        }
        return false;
    }
}
