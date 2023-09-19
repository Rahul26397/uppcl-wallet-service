package com.tequre.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tequre.wallet.config.HLFConfig;
import com.tequre.wallet.data.Transaction;
import com.tequre.wallet.data.Wallet;
import com.tequre.wallet.request.CreateTransactionRequest;
import com.tequre.wallet.request.CreateWalletRequest;
import com.tequre.wallet.request.UpdateWalletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BlockchainService {

    private final Logger logger = LoggerFactory.getLogger(BlockchainService.class);

    @Autowired
    private HLFConfig hlfConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Gson gson;

    private static ObjectMapper MAPPER = new ObjectMapper();

    // Wallet-Controller
    public Optional<Wallet> getWalletByVan(String van) {
        String url = hlfConfig.getUrl() + "/wallet/" + van;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        logger.info("Get Wallet by van {} status {} body : {}", van, response.getStatusCode(), response.getBody());
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonObject jsonWallet = gson.fromJson(response.getBody(), JsonObject.class);
            if (!jsonWallet.keySet().isEmpty()) {
                Wallet wallet = gson.fromJson(response.getBody(), Wallet.class);
                wallet.setId(van);
                logger.info("Wallet Created: {}", gson.toJson(wallet));
                return Optional.of(wallet);
            }
        }
        return Optional.empty();
    }

    public void updateWallet(String van, UpdateWalletRequest updateWalletRequest) {
        String url = hlfConfig.getUrl() + "/wallet/" + van;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<UpdateWalletRequest> updateEntity = new HttpEntity<>(updateWalletRequest, headers);
        ResponseEntity<String> response = restTemplate
                .exchange(url, HttpMethod.PUT, updateEntity, String.class);
        logger.info("Update Wallet by van {} status {} body : {}", van, response.getStatusCode(), response.getBody());
        if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.NO_CONTENT) {
            throw new IllegalStateException("Unable to update wallet.");
        }
    }

    public Wallet createWallet(Wallet wallet) {
        String url = hlfConfig.getUrl() + "/wallet";
        HttpHeaders headers = new HttpHeaders();
        CreateWalletRequest createWalletRequest = transform(wallet);
        HttpEntity<CreateWalletRequest> walletEntity = new HttpEntity<>(createWalletRequest, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, walletEntity, String.class);
        logger.info("Create Wallet status {} body : {}", response.getStatusCode(), response.getBody());
        if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.CREATED) {
            throw new IllegalStateException("Unable to create wallet.");
        } else {
            Wallet createdWallet = gson.fromJson(response.getBody(), Wallet.class);
            createdWallet.setId(wallet.getId());
            logger.info("Wallet Updated: {}", gson.toJson(wallet));
            return createdWallet;
        }
    }

    public Optional<Transaction> getTransactionById(String id) {
        String url = hlfConfig.getUrl() + "/transaction/" + id;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        logger.info("Get Transaction by id {} status {} body : {}", id, response.getStatusCode(), response.getBody());
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonObject jsonWallet = new Gson().fromJson(response.getBody(), JsonObject.class);
            if (!jsonWallet.keySet().isEmpty()) {
                JsonArray resultArr = jsonWallet.getAsJsonArray("result");
                if (resultArr.size() > 0) {
                    JsonElement element = resultArr.get(0);
                    Transaction transaction = gson.fromJson(element, Transaction.class);
                    if (transaction != null && id.equals(transaction.getId())) {
                        return Optional.of(transaction);
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Transaction createTransaction(Transaction transaction) {
        Optional<Transaction> txnOpt = getTransactionById(transaction.getId());
        if (!txnOpt.isPresent()) {
            String url = hlfConfig.getUrl() + "/transaction";
            HttpHeaders headers = new HttpHeaders();
            CreateTransactionRequest createTransactionRequest = transform(transaction);
            HttpEntity<CreateTransactionRequest> transactionEntity = new HttpEntity<>(createTransactionRequest, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, transactionEntity, String.class);
            logger.info("Create Transaction status {} body : {}", response.getStatusCode(), response.getBody());
            if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.CREATED) {
                throw new IllegalStateException("Unable to create transaction.");
            } else {
                Transaction createdTransaction = gson.fromJson(response.getBody(), Transaction.class);
                return createdTransaction;
            }
        } else {
            throw new IllegalArgumentException("Transaction already exists");
        }
    }

    public void deleteTransaction(String id) {
        String url = hlfConfig.getUrl() + "/transaction/" + id;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        logger.info("Delete Transaction by id {} status {} body : {}", id, response.getStatusCode(), response.getBody());
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("Unable to delete transaction.");
        }
    }

    private CreateTransactionRequest transform(Transaction transaction) {
        String txnString = gson.toJson(transaction);
        Map<String, Object> map = gson.fromJson(txnString, Map.class);
        //map.remove("referenceTransactionId");
        //map.remove("connectionType");
        //map.remove("agencyType");
        //map.remove("agencyVan");
        map.remove("divisionCode");
        map.remove("remarks");
        map.remove("transactionState");
        map.remove("revertedTransactionId");
        map.remove("blockchainCommit");
        JsonElement obj = gson.toJsonTree(map);
        CreateTransactionRequest createTransactionRequest = gson.fromJson(obj, CreateTransactionRequest.class);
        logger.info("CreateTransactionRequest = {}", gson.toJson(createTransactionRequest));
        return createTransactionRequest;
    }

    private CreateWalletRequest transform(Wallet wallet) {
        String walletString = gson.toJson(wallet);
        Map<String, Object> map = gson.fromJson(walletString, Map.class);
        map.remove("blockchainCommit");
        JsonElement obj = gson.toJsonTree(map);
        CreateWalletRequest createWalletRequest = gson.fromJson(obj, CreateWalletRequest.class);
        createWalletRequest.setBalance(0d);
        createWalletRequest.setVanId(wallet.getId());
        logger.info("CreateWalletRequest = {}", gson.toJson(createWalletRequest));
        return createWalletRequest;
    }

    public List<Transaction> findByExternalTransactionId(String externalTransactionId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("externalTransactionId").is(externalTransactionId));
        List<Transaction> result = mongoTemplate.find(query, Transaction.class);
        logger.info("findByExternalTransactionId Resource: {}", gson.toJson(result));
        return result;
    }
}
