package com.tequre.wallet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tequre.wallet.data.Agent;
import com.tequre.wallet.data.FailedTransaction;
import com.tequre.wallet.data.ServiceMessage;
import com.tequre.wallet.enums.PaymentType;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.request.CreateWalletRequest;
import com.tequre.wallet.request.Page;
import com.tequre.wallet.service.WalletService;
import com.tequre.wallet.utils.CommonUtils;
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
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tequre.wallet.utils.Constants.UPPCL_VAN;

@RestController
@RequestMapping("/api/wallet")
public class HLFController {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Gson gson;

    @RequestMapping(value = "/{van}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getWallet(@PathVariable String van) {
        return walletService.getWallet(van);
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> createWallet(@RequestBody CreateWalletRequest createWalletRequest) {
        return walletService.createWallet(createWalletRequest);
    }

    @RequestMapping(value = "/owner", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getWalletForOwner() {
        return walletService.getWallet(UPPCL_VAN);
    }

    @RequestMapping(value = "/transactions/{van}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getTransactionsForWallet(@PathVariable String van,
                                                      @RequestParam(value = "type", required = false) String type,
                                                      @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                      @RequestParam(value = "discom", required = false) String discom,
                                                      @RequestParam(value = "division", required = false) String division,
                                                      @RequestParam(value = "billId", required = false) String billId,
                                                      @RequestParam(value = "consumerId", required = false) String consumerId,
                                                      @RequestParam(value = "commission", required = false) Boolean commission,
                                                      @RequestParam(value = "startTime", required = false) Long startTime,
                                                      @RequestParam(value = "endTime", required = false) Long endTime,
                                                      @RequestParam(value = "pageSize", required = false) String pageSize,
                                                      @RequestParam(value = "nextPageToken", required = false) String nextPageToken) {
        return walletService.getTransactions(van, discom, division, billId, consumerId, startTime, endTime, type, commission, pageSize, nextPageToken,transactionType,"");
    }
    
    @RequestMapping(value = "/v2/transactions", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getTransactionsForWalletV2(
    												  @RequestParam(value = "van", required = false) String van,
                                                      @RequestParam(value = "type", required = false) String type,
                                                      @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                      @RequestParam(value = "discom", required = false) String discom,
                                                      @RequestParam(value = "division", required = false) String division,
                                                      @RequestParam(value = "billId", required = false) String billId,
                                                      @RequestParam(value = "consumerId", required = false) String consumerId,
                                                      @RequestParam(value = "commission", required = false) Boolean commission,
                                                      @RequestParam(value = "startTime", required = true) Long startTime,
                                                      @RequestParam(value = "endTime", required = true) Long endTime,
                                                      @RequestParam(value = "pageSize", required = false) String pageSize,
                                                      @RequestParam(value = "nextPageToken", required = false) String nextPageToken) {
        return walletService.getTransactions(van, discom, division, billId, consumerId, startTime, endTime, type, commission, pageSize, nextPageToken,transactionType, "DEPARTMENT");
    }

    @RequestMapping(value = "/owner/transactions", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getTransactionsForOwner(@RequestParam(value = "type", required = false) String type,
    												 @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                     @RequestParam(value = "discom", required = false) String discom,
                                                     @RequestParam(value = "division", required = false) String division,
                                                     @RequestParam(value = "billId", required = false) String billId,
                                                     @RequestParam(value = "consumerId", required = false) String consumerId,
                                                     @RequestParam(value = "startTime", required = false) Long startTime,
                                                     @RequestParam(value = "endTime", required = false) Long endTime,
                                                     @RequestParam(value = "pageSize", required = false) String pageSize,
                                                     @RequestParam(value = "nextPageToken", required = false) String nextPageToken) {
        return walletService.getTransactions(UPPCL_VAN, discom, division, billId, consumerId, startTime, endTime, type, Boolean.FALSE, pageSize, nextPageToken,transactionType,"");
    }

    @RequestMapping(value = "/transactions/failed/{van}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getFailedTransactions(@PathVariable String van,
                                                   @RequestParam(value = "agentId", required = false) String agentId,
                                                   @RequestParam(value = "consumerId", required = false) String consumerId,
                                                   @RequestParam(value = "paymentType", required = false) String paymentType,
                                                   @RequestParam(value = "billId", required = false) String billId,
                                                   @RequestParam(value = "startTime", required = false) Long startTime,
                                                   @RequestParam(value = "endTime", required = false) Long endTime,
                                                   @RequestParam(value = "pageSize", required = false) String pageSize,
                                                   @RequestParam(value = "nextPageToken", required = false) String nextPageToken) throws IOException {

        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("createdAt").descending());

        Query query = new Query();
        if (van != null) {
            Criteria payloadVan = Criteria.where("payload.vanNo").is(van);
            Criteria failedVan = Criteria.where("van").is(van);
            Criteria cr = new Criteria().orOperator(payloadVan, failedVan);
            query.addCriteria(cr);
        }
        if (agentId != null) {
            query.addCriteria(Criteria.where("payload.agentId").is(agentId));
        }
        if (consumerId != null) {
            query.addCriteria(Criteria.where("payload.consumerAccountId").is(consumerId));
        }
        if (paymentType != null) {
            query.addCriteria(Criteria.where("payload.type").is(paymentType));
        }
        if (billId != null) {
            query.addCriteria(Criteria.where("payload.billId").is(billId));
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
        List<FailedTransaction> failedTransactions = mongoTemplate.find(query, FailedTransaction.class);
        JsonElement element;
        JsonObject object = new JsonObject();
        if (failedTransactions != null) {
            element = gson.toJsonTree(failedTransactions, new TypeToken<List<FailedTransaction>>() {
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

    @RequestMapping(value = "/transaction/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getTransactionById(@PathVariable String id) {
        JsonArray array = walletService.transaction(id);
        if (array.size() > 0) {
            return ResponseEntity.status(HttpStatus.OK).body(array.toString());
        } else {
            ServiceMessage error = new ServiceMessage();
            error.setMessage("No transaction found for id " + id);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @RequestMapping(value = "/agent/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getWalletForAgent(@PathVariable String id) {
        String van = getWalletId(id);
        return walletService.getWallet(van);
    }

    @RequestMapping(value = "/agency/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getWalletForAgency(@PathVariable String id) {
        String van = getWalletId(id);
        return walletService.getWallet(van);
    }

    @RequestMapping(value = "/agency/{id}/agent", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> getAgentWalletForAgency(@PathVariable String id) {
        List<String> ids = getWalletIdsForAgency(id);
        List<Object> walletInfo = new ArrayList<>();
        for (String walletVan : ids) {
            ResponseEntity<Object> response = walletService.getWallet(walletVan);
            if (response.getStatusCode() == HttpStatus.OK) {
                walletInfo.add(response.getBody());
            }
        }
        if (walletInfo.isEmpty()) {
            ServiceMessage error = new ServiceMessage();
            error.setMessage("Error occurred when retrieving user's wallet info.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(walletInfo);
        }
    }

    private String getWalletId(String id) {
        Optional<Agent> optionalAgent = agentRepository.findById(id);
        if (optionalAgent.isPresent()) {
            return optionalAgent.get().getVan();
        } else {
            throw new IllegalArgumentException("No agent exist with id " + id);
        }
    }

    private List<String> getWalletIdsForAgency(String id) {
        Agent filterAgent = new Agent();
        filterAgent.setAgencyId(id);
        List<Agent> agents = agentRepository.findAll(Example.of(filterAgent));
        return agents != null ? agents.stream().map(Agent::getVan).collect(Collectors.toList()) : Collections.emptyList();
    }

}
