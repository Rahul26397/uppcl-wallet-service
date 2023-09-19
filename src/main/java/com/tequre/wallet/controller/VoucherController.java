package com.tequre.wallet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tequre.wallet.data.Agent;
import com.tequre.wallet.data.FailedTransaction;
import com.tequre.wallet.data.Transaction;
import com.tequre.wallet.data.Voucher;
import com.tequre.wallet.enums.EventStatus;
import com.tequre.wallet.enums.SourceType;
import com.tequre.wallet.enums.VoucherStatus;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.repository.EventRepository;
import com.tequre.wallet.repository.FailedTransactionRepository;
import com.tequre.wallet.repository.TransactionRepository;
import com.tequre.wallet.repository.VoucherRepository;
import com.tequre.wallet.request.DebitTransaction;
import com.tequre.wallet.request.Page;
import com.tequre.wallet.request.VoucherRequest;
import com.tequre.wallet.response.AcceptedResponse;
import com.tequre.wallet.response.WalletResponse;
import com.tequre.wallet.service.WalletService;
import com.tequre.wallet.utils.CommonUtils;
import com.tequre.wallet.utils.Constants;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/voucher")
public class VoucherController {

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");

    private final Logger logger = LoggerFactory.getLogger(VoucherController.class);

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FailedTransactionRepository failedTransactionRepository;

    @Autowired
    private Gson gson;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getVoucher(@PathVariable String id) {
        Optional<Voucher> voucherOptional = voucherRepository.findById(id);
        if (voucherOptional.isPresent()) {
            Voucher voucher = voucherOptional.get();
            return ResponseEntity.ok(voucher);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVoucher(@PathVariable String id) {
        voucherRepository.deleteById(id);
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> createVoucher(@RequestBody VoucherRequest voucherRequest) {
        Optional<Agent> optionalAgent = agentRepository.findById(voucherRequest.getAgentId());
        if (!optionalAgent.isPresent()) {
            throw new IllegalArgumentException("No agent exist with id " + voucherRequest.getAgentId());
        }
        Agent agent = optionalAgent.get();
        List<Voucher> voucherList = new ArrayList<>();
        double balance = walletService.getBalance(agent.getVan());
        double voucherAmount = voucherRequest.getAmount() * voucherRequest.getNumber();
        if (balance > voucherAmount) {
            for (int i = 0; i < voucherRequest.getNumber(); i++) {
                Voucher voucher = new Voucher();
                voucher.setId(CommonUtils.generateUUID());
                voucher.setAgentId(agent.getId());
                voucher.setAmount(voucherRequest.getAmount());
                voucher.setName(Constants.VOUCHER_PREFIX + System.currentTimeMillis());
                voucher.setStatus(VoucherStatus.ACTIVE);
                voucherList.add(voucher);
            }
            voucherRepository.saveAll(voucherList);
            DebitTransaction debitTransaction = new DebitTransaction();
            debitTransaction.setAgentId(voucherRequest.getAgentId());
            debitTransaction.setAmount(voucherAmount);
            debitTransaction.setSourceType(SourceType.VOUCHER);
            debitTransaction.setTransactionId(CommonUtils.generateUUID());
            Event event = CommonUtils.createEvent(debitTransaction);
            event.setId(debitTransaction.getTransactionId());
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
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "/redeem/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> redeemVoucher(@PathVariable String id) {
        Optional<Voucher> voucherOptional = voucherRepository.findById(id);
        Voucher voucher = null;
        if (voucherOptional.isPresent()) {
            voucher = voucherOptional.get();
            if (voucher.getStatus() == VoucherStatus.ACTIVE) {
                voucher.setStatus(VoucherStatus.REDEEMED);
                voucherRepository.save(voucher);
                return ResponseEntity.accepted().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getVouchers(@RequestParam(value = "status", required = false) EventStatus status,
                                         @RequestParam(value = "agentId", required = false) String agentId,
                                         @RequestParam(value = "startTime", required = false) Long startTime,
                                         @RequestParam(value = "endTime", required = false) Long endTime,
                                         @RequestParam(value = "pageSize", required = false) String pageSize,
                                         @RequestParam(value = "nextPageToken", required = false) String nextPageToken) throws IOException {
        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("createdAt").descending());

        Query query = new Query();
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (agentId != null) {
            query.addCriteria(Criteria.where("agentId").is(agentId));
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
        List<Voucher> users = mongoTemplate.find(query, Voucher.class);
        JsonElement element;
        JsonObject object = new JsonObject();
        if (users != null) {
            element = gson.toJsonTree(users, new TypeToken<List<Voucher>>() {
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

}
