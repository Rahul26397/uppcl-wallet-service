package com.tequre.wallet.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tequre.wallet.config.HLFConfig;
import com.tequre.wallet.data.Agent;
import com.tequre.wallet.data.ServiceMessage;
import com.tequre.wallet.data.Transaction;
import com.tequre.wallet.data.Wallet;
import com.tequre.wallet.enums.Mode;
import com.tequre.wallet.enums.PaymentType;
import com.tequre.wallet.enums.SourceType;
import com.tequre.wallet.enums.TransactionType;
import com.tequre.wallet.repository.TransactionRepository;
import com.tequre.wallet.repository.WalletRepository;
import com.tequre.wallet.request.CreateWalletRequest;
import com.tequre.wallet.request.DepositRequest;
import com.tequre.wallet.request.Page;
import com.tequre.wallet.request.TransferRequest;
import com.tequre.wallet.request.WithdrawRequest;
import com.tequre.wallet.utils.CommonUtils;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@Service
public class WalletService {

    private static final SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    private final Logger logger = LoggerFactory.getLogger(WalletService.class);

    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;
    @Autowired
    private HLFConfig hlfConfig;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private Gson gson;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private RestTemplate restTemplate;

    public String deposit(String van, Double amount, String entityId, String entityType,
                          String txnType, String txnId, String externalId, String agencyId) {
        String externalTxnId = sequenceGeneratorService.generateTransactionId();
        Long txnTime = Instant.now().toEpochMilli();
        switch (hlfConfig.getMode()) {
            case LOCAL:
                return depositTransactionLocal(van, amount, entityId, entityType, txnType, txnId, null, externalId, null, null, null, null, null,
                        txnTime, externalTxnId, agencyId, null, null, null, null);
            case REMOTE:
                return depositTransactionRemote(van, amount, entityId, entityType, txnType, txnId, externalId, externalTxnId, agencyId, hlfConfig.getMode());
            case BOTH:
                depositTransactionLocal(van, amount, entityId, entityType, txnType, txnId, null, externalId, null, null, null, null, null,
                        txnTime, externalTxnId, agencyId, null, null, null, null);
                try {
                    depositTransactionRemote(van, amount, entityId, entityType, txnType, txnId, externalId, externalTxnId, agencyId, hlfConfig.getMode());
                    Transaction localTxn = new Transaction();
                    localTxn.setExternalTransactionId(externalTxnId);
                    List<Transaction> txns = transactionRepository.findAll(Example.of(localTxn));
                    txns.forEach(txn -> txn.setBlockchainCommit(true));
                    transactionRepository.saveAll(txns);
                } catch (Exception e) {
                    logger.warn("Error occurred in deposit operation", e);
                    if (!hlfConfig.isPrimary()) {
                        return externalTxnId;
                    }
                    throw e;
                }
                return externalTxnId;
            default:
                throw new IllegalStateException("Invalid Mode");

        }
    }

    public String withdraw(String van, Double amount, String entityId, String entityType,
                           String txnType, String txnId, String externalId, String agencyId) {
        String externalTxnId = sequenceGeneratorService.generateTransactionId();
        Long txnTime = Instant.now().toEpochMilli();
        switch (hlfConfig.getMode()) {
            case LOCAL:
                return withdrawTransactionLocal(van, amount, entityId, entityType, txnType, txnId, null, externalId, null, null, null, null, null,
                        txnTime, externalTxnId, agencyId, null, null, null, null);
            case REMOTE:
                return withdrawTransactionRemote(van, amount, entityId, entityType, txnType, txnId, externalId, externalTxnId, agencyId, hlfConfig.getMode());
            case BOTH:
                withdrawTransactionLocal(van, amount, entityId, entityType, txnType, txnId, null, externalId, null, null, null, null, null,
                        txnTime, externalTxnId, agencyId, null, null, null, null);
                try {
                    withdrawTransactionRemote(van, amount, entityId, entityType, txnType, txnId, externalId, externalTxnId, agencyId, hlfConfig.getMode());
                    Transaction localTxn = new Transaction();
                    localTxn.setExternalTransactionId(externalTxnId);
                    List<Transaction> txns = transactionRepository.findAll(Example.of(localTxn));
                    txns.forEach(txn -> txn.setBlockchainCommit(true));
                    transactionRepository.saveAll(txns);
                } catch (Exception e) {
                    logger.warn("Error occurred in withdraw operation", e);
                    if (!hlfConfig.isPrimary()) {
                        return externalTxnId;
                    }
                    throw e;
                }
                return externalTxnId;
            default:
                throw new IllegalStateException("Invalid Mode");
        }
    }

    public String transfer(String externalTxnId, String sourceVan, String destinationVan, Double amount, String entityId, String entityType,
                           String txnType, String txnId, String consumerId, String externalId, String division, String discom,
                           String referenceTxnId, String divisionCode, String mobile, String srcAgencyId, String destAgencyId,
                           String agencyVan, Double commissionRate, Double gstRate, Double tdsRate, Double gstTdsRate,
                           String agencyType, String connectionType) {
        Long txnTime = Instant.now().toEpochMilli();
        switch (hlfConfig.getMode()) {
            case LOCAL:
                return transferTransactionLocal(sourceVan, destinationVan, amount, entityId, entityType, txnType, txnId,
                        consumerId, externalId, division, discom, mobile, referenceTxnId, divisionCode, txnTime,
                        externalTxnId, srcAgencyId, destAgencyId, agencyVan, commissionRate, gstRate, tdsRate, gstTdsRate,
                        agencyType, connectionType);
            case REMOTE:
                return transferTransactionRemote(sourceVan, destinationVan, amount, entityId, entityType, txnType, txnId,
                        consumerId, externalId, division, discom, mobile, referenceTxnId, divisionCode, externalTxnId,
                        srcAgencyId, destAgencyId, agencyVan, commissionRate, gstRate, tdsRate, gstTdsRate,
                        agencyType, connectionType, hlfConfig.getMode());
            case BOTH:
                transferTransactionLocal(sourceVan, destinationVan, amount, entityId, entityType, txnType, txnId,
                        consumerId, externalId, division, discom, mobile, referenceTxnId, divisionCode, txnTime,
                        externalTxnId, srcAgencyId, destAgencyId, agencyVan, commissionRate, gstRate, tdsRate, gstTdsRate,
                        agencyType, connectionType);
                try {
                    transferTransactionRemote(sourceVan, destinationVan, amount, entityId, entityType, txnType, txnId,
                            consumerId, externalId, division, discom, mobile, referenceTxnId, divisionCode, externalTxnId,
                            srcAgencyId, destAgencyId, agencyVan, commissionRate, gstRate, tdsRate, gstTdsRate,
                            agencyType, connectionType, hlfConfig.getMode());
                    Transaction localTxn = new Transaction();
                    localTxn.setExternalTransactionId(externalTxnId);
                    List<Transaction> txns = transactionRepository.findAll(Example.of(localTxn));
                    txns.forEach(txn -> txn.setBlockchainCommit(true));
                    transactionRepository.saveAll(txns);
                } catch (Exception e) {
                    logger.warn("Error occurred in transaction operation", e);
                    if (!hlfConfig.isPrimary()) {
                        return externalTxnId;
                    }
                    throw e;
                }
                return externalTxnId;
            default:
                throw new IllegalStateException("Invalid Mode");
        }
    }

    public Double getBalance(String walletId) {
        switch (hlfConfig.getMode()) {
            case LOCAL:
                return getBalanceLocal(walletId);
            case REMOTE:
                return getBalanceRemote(walletId);
            case BOTH:
                try {
                    return getBalanceRemote(walletId);
                } catch (Exception e) {
                    logger.warn("Error occurred in get balance operation", e);
                    if (!hlfConfig.isPrimary()) {
                        return getBalanceLocal(walletId);
                    }
                    throw e;
                }
            default:
                return 0d;
        }
    }

    private Double getBalanceLocal(String walletId) {
        Optional<Wallet> walletOpt = walletRepository.findById(walletId);
        if (walletOpt.isPresent()) {
            BigDecimal bd = new BigDecimal(walletOpt.get().getBalance()).setScale(2, RoundingMode.HALF_UP);
            return bd.doubleValue();
        } else {
            throw new IllegalArgumentException("No wallet exist with id " + walletId);
        }
    }

    private Double getBalanceRemote(String walletId) {
        ResponseEntity<String> response
                = restTemplate.getForEntity(hlfConfig.getUrl() + "/wallet/" + walletId, String.class);
        logger.info("Remote Wallet Balance: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("Unable to fetch balance from blockchain.");
        }
        JsonObject jsonWallet = new Gson().fromJson(response.getBody(), JsonObject.class);
        JsonElement jsonBalance = jsonWallet.get("balance");
        if (jsonBalance != null) {
            BigDecimal bd = new BigDecimal(jsonBalance.getAsDouble()).setScale(2, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
        return 0d;
    }

    public Optional<Wallet> getWallet(String vanId, Mode mode) {
        switch (mode) {
            case LOCAL:
                return walletRepository.findById(vanId);
            case REMOTE:
                String url = hlfConfig.getUrl() + "/wallet/" + vanId;
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                logger.info("Fetch Remote Wallet: " + response.getStatusCode() + " Body: " + response.getBody());
                if (response.getStatusCode() == HttpStatus.OK) {
                    JsonObject jsonWallet = gson.fromJson(response.getBody(), JsonObject.class);
                    if (!jsonWallet.keySet().isEmpty()) {
                        Wallet wallet = gson.fromJson(response.getBody(), Wallet.class);
                        return Optional.of(wallet);
                    }
                }
                return Optional.empty();
            default:
                return Optional.empty();
        }
    }


    public JsonArray transaction(String transactionId) {
        switch (hlfConfig.getMode()) {
            case LOCAL:
                return getTransactionLocal(transactionId);
            case REMOTE:
                return getTransactionRemote(transactionId);
            case BOTH:
                try {
                    return getTransactionRemote(transactionId);
                } catch (Exception e) {
                    logger.warn("Error occurred in get transaction operation", e);
                    if (!hlfConfig.isPrimary()) {
                        return getTransactionLocal(transactionId);
                    }
                    throw e;
                }
            default:
                throw new IllegalStateException("Invalid Mode");
        }
    }

    public ResponseEntity<Object> createWallet(CreateWalletRequest createWalletRequest) {
        switch (hlfConfig.getMode()) {
            case LOCAL:
                Wallet obj = createWalletLocal(createWalletRequest);
                return ResponseEntity.ok(obj);
            case REMOTE:
                return createWalletRemote(createWalletRequest);
            case BOTH:
                Wallet walletLocal = createWalletLocal(createWalletRequest);
                try {
                    ResponseEntity<Object> walletRemote = createWalletRemote(createWalletRequest);
                    walletLocal.setBlockchainCommit(true);
                    walletRepository.save(walletLocal);
                    return walletRemote;
                } catch (Exception e) {
                    logger.warn("Error occurred when creating remote wallet", e);
                    if (!hlfConfig.isPrimary()) {
                        return ResponseEntity.ok(walletLocal);
                    }
                    throw e;
                }
            default:
                throw new IllegalStateException("Invalid Mode");
        }
    }

    public ResponseEntity<Object> getWallet(String vanId) {
        switch (hlfConfig.getMode()) {
            case LOCAL:
                return getWalletLocal(vanId);
            case REMOTE:
                return getWalletRemote(vanId);
            case BOTH:
                try {
                    ResponseEntity<Object> remoteWallet = getWalletRemote(vanId);
                    if (remoteWallet.getStatusCode() != HttpStatus.OK && !hlfConfig.isPrimary()) {
                        return getWalletLocal(vanId);
                    }
                    return remoteWallet;
                } catch (Exception e) {
                    logger.warn("Error occurred in get remote wallet", e);
                    if (!hlfConfig.isPrimary()) {
                        return getWalletLocal(vanId);
                    }
                    throw e;
                }
            default:
                throw new IllegalStateException("Invalid Mode");
        }
    }

    private ResponseEntity<Object> getWalletLocal(String vanId) {
        Optional<Wallet> obj = walletRepository.findById(vanId);
        if (obj.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(obj.get());
        } else {
            ServiceMessage error = new ServiceMessage();
            error.setMessage("No wallet exist with VAN : " + vanId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    private ResponseEntity<Object> getWalletRemote(String vanId) {
        String url = hlfConfig.getUrl() + "/wallet/" + vanId;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        logger.info("Fetch Remote Wallet: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonObject jsonWallet = new Gson().fromJson(response.getBody(), JsonObject.class);
            if (jsonWallet.keySet().isEmpty()) {
                ServiceMessage error = new ServiceMessage();
                error.setMessage("No wallet exist with VAN : " + vanId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            } else {
                return ResponseEntity.status(HttpStatus.OK).body(response.getBody());
            }
        } else {
            ServiceMessage error = new ServiceMessage();
            error.setMessage("Error occurred when retrieving wallet info.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    public ResponseEntity<?> getTransactions(String vanId, String discom, String division, String billId, String consumerId, Long startTime, Long endTime, String type,
                                             Boolean commission, String pageSize, String nextPageToken, PaymentType transactionType, String entityType) {
        switch (hlfConfig.getMode()) {
            case LOCAL:
                return getTransactionsLocal(vanId, discom, division, billId, consumerId, startTime, endTime, type, commission, pageSize, nextPageToken, transactionType, entityType);
            case REMOTE:
                return getTransactionsRemote(vanId, discom, division, billId, consumerId, startTime, endTime, type, commission, pageSize, nextPageToken, transactionType);
            case BOTH:
                try {
                    ResponseEntity<?> response = getTransactionsRemote(vanId, discom, division, billId, consumerId, startTime, endTime, type, commission, pageSize, nextPageToken, transactionType);
                    if (response.getStatusCode() != HttpStatus.OK && !hlfConfig.isPrimary()) {
                        return getTransactionsLocal(vanId, discom, division, billId, consumerId, startTime, endTime, type, commission, pageSize, nextPageToken, transactionType, entityType);
                    }
                    return response;
                } catch (Exception e) {
                    logger.warn("Error occurred in get transactions operation", e);
                    if (!hlfConfig.isPrimary()) {
                        return getTransactionsLocal(vanId, discom, division, billId, consumerId, startTime, endTime, type, commission, pageSize, nextPageToken, transactionType,entityType);
                    }
                    throw e;
                }
            default:
                throw new IllegalStateException("Invalid Mode");
        }
    }

    private ResponseEntity<?> getTransactionsRemote(String vanId, String discom, String division, String billId,
                                                    String consumerId, Long startTime, Long endTime, String type,
                                                    Boolean commission, String pageSize, String nextPageToken, PaymentType transactionType) {
        String queryParams = "?search=vanId==" + vanId;
        if (discom != null) {
            queryParams = queryParams + " and discom==" + discom;
        }
        if (division != null) {
            queryParams = queryParams + " and division==" + division;
        }
        if (billId != null) {
            queryParams = queryParams + " and billId==" + billId;
        }
        if (consumerId != null) {
            queryParams = queryParams + " and consumerId==" + consumerId;
        }
        if (startTime != null) {
            queryParams = queryParams + " and transactionTime>=" + startTime.longValue();
        }
        if (endTime != null) {
            queryParams = queryParams + " and transactionTime<=" + endTime.longValue();
        }
        if (type != null) {
            queryParams = queryParams + " and activity==" + type.toUpperCase();
        }
        if (pageSize != null) {
            queryParams = queryParams + "&pageSize=" + pageSize;
        }
        if (nextPageToken != null) {
            queryParams = queryParams + "&nextPageToken=" + nextPageToken;
        }
        if (transactionType != null) {
            queryParams = queryParams + " and transactionType==" + transactionType.name();
        }
        String url = hlfConfig.getUrl() + "/transaction" + queryParams;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        logger.info("Remote Transactions: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonObject jsonWallet = new Gson().fromJson(response.getBody(), JsonObject.class);
            if (jsonWallet.keySet().isEmpty()) {
                ServiceMessage error = new ServiceMessage();
                error.setMessage("No wallet exist for owner");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            } else {
                JsonArray resultArr = jsonWallet.getAsJsonArray("result");
                jdf.setTimeZone(TimeZone.getTimeZone("IST"));
                Iterator<JsonElement> elements = resultArr.iterator();
                JsonArray newArray = new JsonArray();
                while (elements.hasNext()) {
                    JsonElement element = elements.next();
                    JsonObject obj = element.getAsJsonObject();
                    //long time = obj.get("transactionTime").getAsLong();
                    //obj.addProperty("time", getTime(time));
                    if (commission != null && commission) {
                        String txnType = obj.get("transactionType").getAsString();
                        if (SourceType.RAPDRP.name().equals(txnType.toUpperCase()) ||
                                SourceType.NON_RAPDRP.name().equals(txnType.toUpperCase())) {
                            double txnAmt = obj.get("amount").getAsDouble();
                            Double commissionValue = getCommission(txnType, txnAmt);
                            if (commissionValue != null) {
                                obj.addProperty("commission", commissionValue.doubleValue());
                            }
                            newArray.add(obj);
                        }
                    } else {
                        newArray.add(obj);
                    }
                }
                jsonWallet.add("result", newArray);
                return ResponseEntity.status(HttpStatus.OK).body(jsonWallet.toString());
            }
        } else {
            ServiceMessage error = new ServiceMessage();
            error.setMessage("Error occurred when retrieving transaction details.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private ResponseEntity<?> getTransactionsLocal(String vanId, String discom, String division, String billId,
                                                   String consumerId, Long startTime, Long endTime, String type,
                                                   Boolean commission, String pageSize, String nextPageToken, PaymentType transactionType, String entityType) {
        Query query = new Query();
        if (entityType == "DEPARTMENT") {
            query.addCriteria(Criteria.where("entityType").is(entityType));
        }
        if (vanId != null) {
            query.addCriteria(Criteria.where("vanId").is(vanId));
        }
        if (vanId == null) {
        	query.addCriteria(Criteria.where("vanId").ne("UPPCL"));
        }
        // 
        if (discom != null) {
            query.addCriteria(Criteria.where("discom").regex("^" + discom + "$", "i"));
        }
        if (division != null) {
            query.addCriteria(Criteria.where("division").regex("^" + division + "$", "i"));
        }
        if (billId != null) {
            query.addCriteria(Criteria.where("billId").is(billId));
        }
        if (consumerId != null) {
            query.addCriteria(Criteria.where("consumerId").is(consumerId));
        }
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("transactionTime");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            query.addCriteria(range);
        }
        if (type != null) {
            query.addCriteria(Criteria.where("activity").is(type.toUpperCase()));
        }
        if (transactionType != null) {
            query.addCriteria(Criteria.where("transactionType").is(transactionType.name()));
        }
        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("transactionTime").descending());
        query.with(pageableRequest);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        JsonObject object = new JsonObject();
        JsonArray array = new JsonArray();
        transactions.forEach(txn -> {
            JsonElement element = gson.toJsonTree(txn);
            JsonObject obj = element.getAsJsonObject();
            Query query2 = new Query();
            // logger.info("TEST:"+obj.get("vanId").getAsString());
            query2.addCriteria(Criteria.where("van").is(obj.get("vanId").getAsString()));
            List<Agent> agent = mongoTemplate.find(query2, Agent.class);
            if(agent.size() !=0 ) {
            	Agent agentData = agent.get(0);
                String agencyName = agentData.getAgencyName();
                String agencyCode = agentData.getEmpId();
                obj.addProperty("agencyName", agencyName);
                obj.addProperty("agencyCode", agencyCode);
            }
            
            //long time = obj.get("transactionTime").getAsLong();
            //obj.addProperty("time", getTime(time));
            if (commission != null && commission) {
                String txnType = obj.get("transactionType").getAsString();
                if (SourceType.RAPDRP.name().equals(txnType.toUpperCase()) ||
                        SourceType.NON_RAPDRP.name().equals(txnType.toUpperCase())) {
                    double txnAmt = obj.get("amount").getAsDouble();
                    Double commissionValue = getCommission(txnType, txnAmt);
                    if (commissionValue != null) {
                        obj.addProperty("commission", commissionValue.doubleValue());
                    }
                    array.add(obj);
                }
            } else {
                array.add(obj);
            }
        });
        object.addProperty("nextPageToken", CommonUtils.nextPageToken(gson, p));
        object.addProperty("recordCount", Integer.valueOf(array.size()));
        object.add("result", array);
        return ResponseEntity.status(HttpStatus.OK).body(object.toString());
    }

    /* private String getTime(long longTime) {
        Date date = new Date(longTime);
        return jdf.format(date);
    } */

    private String withdrawTransactionLocal(String van, Double amount, String entityId, String entityType,
                                            String txnType, String txnId, String consumerId, String externalId,
                                            String mobile, String referenceTxnId, String divisionCode,
                                            String discom, String division, Long txnTime, String externalTxnId,
                                            String agencyId, String agencyVan, Double commission,
                                            String agencyType, String connectionType) {
        Optional<Wallet> walletOpt = walletRepository.findById(van);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            if (wallet.getBalance() - amount < 0) {
                throw new IllegalStateException("Not enough funds in wallet " + van);
            }
            Transaction debitTransaction = new Transaction();
            debitTransaction.setId(CommonUtils.generateTransactionUUID());
            debitTransaction.setAgencyId(agencyId);
            debitTransaction.setExternalTransactionId(externalTxnId);
            debitTransaction.setTransactionType(txnType);
            debitTransaction.setTransactionId(txnId);
            debitTransaction.setVanId(van);
            debitTransaction.setAmount(amount);
            debitTransaction.setTransactionTime(txnTime);
            debitTransaction.setEntityId(entityId);
            debitTransaction.setEntityType(entityType);
            debitTransaction.setActivity(TransactionType.DEBIT.name());
            debitTransaction.setExternalId(externalId);
            debitTransaction.setBlockchainCommit(false);
            debitTransaction.setMobile(mobile);
            debitTransaction.setDiscom(discom);
            debitTransaction.setDivision(division);
            debitTransaction.setReferenceTransactionId(referenceTxnId);
            debitTransaction.setDivisionCode(divisionCode);
            debitTransaction.setAgencyVan(agencyVan);
            debitTransaction.setAgencyType(agencyType);
            debitTransaction.setConnectionType(connectionType);
            if (consumerId != null) {
                debitTransaction.setBillId(txnId);
                debitTransaction.setConsumerId(consumerId);
            }
            transactionRepository.save(debitTransaction);
            Double newBalance = wallet.getBalance() - amount;
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);

            creditCommission(commission, debitTransaction, agencyType, agencyVan, van);
            return debitTransaction.getExternalTransactionId();
        } else {
            throw new IllegalArgumentException("No Wallet exist with vanId " + van);
        }
    }

    private void creditCommission(Double commission, Transaction debitTransaction, String agencyType, String agencyVan, String agentVan) {
        String van = null;
        if (commission != null) {
            // SHG, PDS, Others -> agency's wallet
            // MR, PACS -> van making payment
        	/*
        	 * Changed By Hotam
        	 * Avoid commission in agent's wallet
        	 * */
            /* if ("MR".equalsIgnoreCase(agencyType) || "PACS".equalsIgnoreCase(agencyType)) {
                van = agentVan;
                agencyVan = agentVan;
            } else {
                van = agencyVan;
            }*/
        	       	
        	van = agencyVan;
            Transaction commissionTransaction = new Transaction();
            commissionTransaction.setId(CommonUtils.generateTransactionUUID());
            commissionTransaction.setAgencyId(debitTransaction.getAgencyId());
            commissionTransaction.setExternalTransactionId(debitTransaction.getExternalTransactionId());
            commissionTransaction.setTransactionType(SourceType.COMMISSION.name());
            commissionTransaction.setTransactionId(debitTransaction.getTransactionId());
            commissionTransaction.setVanId(van);
            commissionTransaction.setAmount(commission);
            commissionTransaction.setTransactionTime(Instant.now().toEpochMilli());
            commissionTransaction.setEntityId(debitTransaction.getEntityId());
            commissionTransaction.setEntityType(debitTransaction.getEntityType());
            commissionTransaction.setActivity(TransactionType.CREDIT.name());
            commissionTransaction.setExternalId(debitTransaction.getExternalId());
            commissionTransaction.setBlockchainCommit(false);
            commissionTransaction.setMobile(debitTransaction.getMobile());
            commissionTransaction.setDiscom(debitTransaction.getDiscom());
            commissionTransaction.setDivision(debitTransaction.getDivision());
            commissionTransaction.setReferenceTransactionId(debitTransaction.getReferenceTransactionId());
            commissionTransaction.setDivisionCode(debitTransaction.getDivisionCode());
            commissionTransaction.setAgencyVan(agencyVan);
            commissionTransaction.setAgencyType(debitTransaction.getAgencyType());
            commissionTransaction.setConnectionType(debitTransaction.getConnectionType());
            commissionTransaction.setBillId(debitTransaction.getBillId());
            commissionTransaction.setConsumerId(debitTransaction.getConsumerId());
            transactionRepository.save(commissionTransaction);

            Optional<Wallet> walletOpt = walletRepository.findById(van);
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                Double newBalance = wallet.getBalance() + commission;
                wallet.setBalance(newBalance);
                walletRepository.save(wallet);
            } else {
                throw new IllegalArgumentException("No Wallet exist with vanId " + van);
            }
            logger.info("Amount {} commission credited to wallet {} for bill id and external id {}", commission, van,
                    debitTransaction.getBillId(), debitTransaction.getExternalTransactionId());
        } else {
            logger.info("No commission found for payment {}", debitTransaction.getExternalTransactionId());
        }
    }

    private String withdrawTransactionRemote(String van, Double amount, String entityId, String entityType,
                                             String txnType, String txnId, String externalId, String externalTxnId,
                                             String agencyId, Mode mode) {
        HttpHeaders headers = new HttpHeaders();
        WithdrawRequest withdrawRequest = new WithdrawRequest();
        withdrawRequest.setSourceVanId(van);
        withdrawRequest.setAmount(amount);
        withdrawRequest.setAgencyId(agencyId);
        withdrawRequest.setEntityId(entityId);
        withdrawRequest.setEntityType(entityType);
        withdrawRequest.setTransactionType(txnType);
        withdrawRequest.setTransactionId(txnId);
        withdrawRequest.setExternalId(externalId);
        withdrawRequest.setExternalTransactionId(externalTxnId);
        HttpEntity<WithdrawRequest> entity = new HttpEntity<>(withdrawRequest, headers);
        ResponseEntity<String> response = restTemplate
                .exchange(hlfConfig.getUrl() + "/transaction/withdraw", HttpMethod.POST, entity, String.class);
        logger.info("Withdraw Remote: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("Unable to persist transaction in blockchain.");
        }
        return withdrawRequest.getExternalTransactionId();
    }


    private String depositTransactionLocal(String van, Double amount, String entityId, String entityType,
                                           String txnType, String txnId, String consumerId, String externalId,
                                           String mobile, String referenceTxnId, String divisionCode, String discom,
                                           String division, Long txnTime, String externalTxnId, String agencyId,
                                           String agencyVan, Double commission, String agencyType, String connectionType) {
        Optional<Wallet> walletOpt = walletRepository.findById(van);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            Transaction transaction = new Transaction();
            transaction.setId(CommonUtils.generateTransactionUUID());
            transaction.setAgencyId(agencyId);
            transaction.setExternalTransactionId(externalTxnId);
            transaction.setTransactionType(txnType);
            transaction.setTransactionId(txnId);
            transaction.setVanId(van);
            transaction.setAmount(amount);
            transaction.setTransactionTime(txnTime);
            transaction.setEntityId(entityId);
            transaction.setEntityType(entityType);
            transaction.setActivity(TransactionType.CREDIT.name());
            transaction.setExternalId(externalId);
            transaction.setBlockchainCommit(false);
            transaction.setMobile(mobile);
            transaction.setDiscom(discom);
            transaction.setDivision(division);
            transaction.setDivisionCode(divisionCode);
            transaction.setReferenceTransactionId(referenceTxnId);
            transaction.setAgencyVan(agencyVan);
            transaction.setAgencyType(agencyType);
            transaction.setConnectionType(connectionType);
            if (consumerId != null) {
                transaction.setBillId(txnId);
                transaction.setConsumerId(consumerId);
            }
            transactionRepository.save(transaction);
            Double newBalance = wallet.getBalance() + amount;
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);
            return transaction.getExternalTransactionId();
        } else {
            throw new IllegalArgumentException("No Wallet exist with vanId " + van);
        }
    }

    private String depositTransactionRemote(String van, Double amount, String entityId, String entityType,
                                            String txnType, String txnId, String externalId, String externalTxnId, String agencyId, Mode mode) {
        HttpHeaders headers = new HttpHeaders();
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setDestinationVanId(van);
        depositRequest.setAmount(amount);
        depositRequest.setAgencyId(agencyId);
        depositRequest.setEntityId(entityId);
        depositRequest.setEntityType(entityType);
        depositRequest.setTransactionType(txnType);
        depositRequest.setTransactionId(txnId);
        depositRequest.setExternalId(externalId);
        depositRequest.setExternalTransactionId(externalTxnId);
        HttpEntity<DepositRequest> entity = new HttpEntity<>(depositRequest, headers);
        ResponseEntity<String> response = restTemplate
                .exchange(hlfConfig.getUrl() + "/transaction/deposit", HttpMethod.POST, entity, String.class);
        logger.info("Deposit Remote: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("Unable to persist transaction in blockchain.");
        }
        return depositRequest.getExternalTransactionId();
    }

    private String transferTransactionLocal(String sourceVan, String destinationVan, Double amount, String entityId, String entityType,
                                            String txnType, String txnId, String consumerId, String externalId, String division, String discom,
                                            String mobile, String referenceTxnId, String divisionCode, Long txnTime, String externalTxnId,
                                            String srcAgencyId, String destAgencyId, String agencyVan,
                                            Double commissionRate, Double gstRate, Double tdsRate, Double gstTdsRate,
                                            String agencyType, String connectionType) {
        Double commission = calculateCommission(amount, commissionRate, gstRate, tdsRate, gstTdsRate);
        withdrawTransactionLocal(sourceVan, amount, entityId, entityType, txnType, txnId, consumerId, externalId, mobile, referenceTxnId, divisionCode, discom, division, txnTime, externalTxnId, srcAgencyId, agencyVan, commission, agencyType, connectionType);
        depositTransactionLocal(destinationVan, amount, entityId, entityType, txnType, txnId, consumerId, externalId, mobile, referenceTxnId, divisionCode, discom, division, txnTime, externalTxnId, destAgencyId, agencyVan, commission, agencyType, connectionType);
        return externalTxnId;
    }

    private Double calculateCommission(Double amount, Double rate, Double gstRate, Double tdsRate, Double gstTdsRate) {
        if (rate != null) {
            // commission = ((amount * rate /100) + GST (18% of the Commission)) – ((TDS (2% of commission)) – ( 2% GST on TDS)
            double commission = amount * (rate / 100);
            
            final DecimalFormat df = new DecimalFormat("0.00");
            df.setRoundingMode(RoundingMode.DOWN);
            BigDecimal commissionBD = new BigDecimal(commission).setScale(2, RoundingMode.HALF_UP);
            // BigDecimal commissionBD = new BigDecimal(df.format(commission));
            if (gstRate == null) {
                gstRate = 0d;
            }
            double gst = commissionBD.doubleValue() * (gstRate / 100);
            BigDecimal gstBD = new BigDecimal(gst).setScale(2, RoundingMode.HALF_UP);
            // BigDecimal gstBD = new BigDecimal(df.format(gst));
            if (tdsRate == null) {
                tdsRate = 0d;
            }
            double tds = commissionBD.doubleValue() * (tdsRate / 100);
            BigDecimal tdsBD = new BigDecimal(tds).setScale(2, RoundingMode.HALF_UP);
            // BigDecimal tdsBD = new BigDecimal(df.format(tds));
            if (gstTdsRate == null) {
                gstTdsRate = 0d;
            }
            // double gstTds = tdsBD.doubleValue() * (gstTdsRate / 100);
            double gstTds = commissionBD.doubleValue() * (gstTdsRate / 100);
            BigDecimal gstTdsBD = new BigDecimal(gstTds).setScale(2, RoundingMode.HALF_UP);
            // BigDecimal gstTdsBD = new BigDecimal(df.format(gstTds));
            // logger.info("Commission Rates Amount: " + commissionBD.doubleValue() +"+" + gstBD.doubleValue() +"-" + tdsBD.doubleValue() +"-" + gstTdsBD.doubleValue());
            String totalCommission = String.format("%.2f", commissionBD.doubleValue() + gstBD.doubleValue() - tdsBD.doubleValue() - gstTdsBD.doubleValue());
            double finalCommission = Double.parseDouble(totalCommission);
            // logger.info("Total Commission: "+totalCommission);
            // logger.info("Final Commission: "+finalCommission);
            // logger.info("Amount: "+amount);
            
            BigDecimal bd = new BigDecimal(finalCommission).setScale(2, RoundingMode.HALF_UP);
            // BigDecimal bd = new BigDecimal(df.format(finalCommission));
            return bd.doubleValue();
        }
        return null;
    }

    private String transferTransactionRemote(String sourceVan, String destinationVan, Double amount, String entityId, String entityType,
                                             String txnType, String txnId, String consumerId, String externalId, String division, String discom, String mobile,
                                             String referenceTxnId, String divisionCode, String externalTxnId, String srcAgencyId, String destAgencyId,
                                             String agencyVan, Double commissionRate, Double gstRate, Double tdsRate, Double gstTdsRate,
                                             String agencyType, String connectionType, Mode mode) {
        HttpHeaders headers = new HttpHeaders();
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSourceVanId(sourceVan);
        transferRequest.setDestinationVanId(destinationVan);
        transferRequest.setAmount(amount);
        transferRequest.setSourceAgencyId(srcAgencyId);
        transferRequest.setDestinationAgencyId(destAgencyId);
        transferRequest.setEntityId(entityId);
        transferRequest.setEntityType(entityType);
        transferRequest.setTransactionType(txnType);
        transferRequest.setTransactionId(txnId);
        transferRequest.setExternalId(externalId);
        transferRequest.setDivision(division);
        transferRequest.setDiscom(discom);
        transferRequest.setMobile(mobile);
        transferRequest.setExternalTransactionId(externalTxnId);
        transferRequest.setBillId(txnId);
        transferRequest.setConsumerId(consumerId);
        transferRequest.setAgencyType(agencyType);
        transferRequest.setConnectionType(connectionType);
        transferRequest.setAgencyVan(agencyVan);
        transferRequest.setReferenceTransactionId(referenceTxnId);
        transferRequest.setDivisionCode(divisionCode);
        HttpEntity<TransferRequest> entity = new HttpEntity<>(transferRequest, headers);
        ResponseEntity<String> response = restTemplate
                .exchange(hlfConfig.getUrl() + "/transaction/transfer", HttpMethod.POST, entity, String.class);
        logger.info("Transfer Remote: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("Unable to persist transaction in blockchain.");
        }

        if (commissionRate != null) {
            Double commission = calculateCommission(amount, commissionRate, gstRate, tdsRate, gstTdsRate);
            // SHG, PDS, Others -> agency's wallet
            // MR, PACS -> van making payment
            String van = null;
            if ("MR".equalsIgnoreCase(agencyType) || "PACS".equalsIgnoreCase(agencyType)) {
                van = sourceVan;
            } else {
                van = agencyVan;
            }
            depositTransactionRemote(van, commission, entityId, entityType, SourceType.COMMISSION.name(),
                    txnId, externalId, externalTxnId, srcAgencyId, mode);
        }

        return transferRequest.getExternalTransactionId();
    }

    private Wallet createWalletLocal(CreateWalletRequest createWalletRequest) {
        Wallet wallet = new Wallet();
        wallet.setBalance(createWalletRequest.getBalance());
        wallet.setId(createWalletRequest.getVanId());
        wallet.setWalletStatus(createWalletRequest.getWalletStatus());
        wallet.setWalletType(createWalletRequest.getWalletType());
        return walletRepository.save(wallet);
    }

    private ResponseEntity<Object> createWalletRemote(CreateWalletRequest createWalletRequest) {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<CreateWalletRequest> walletEntity = new HttpEntity<>(createWalletRequest, headers);
        ResponseEntity<Object> response = restTemplate
                .exchange(hlfConfig.getUrl() + "/wallet", HttpMethod.POST, walletEntity, Object.class);
        logger.info("Create Remote Wallet: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("Unable to create wallet in blockchain.");
        }
        return response;
    }


    public boolean createWallet(Wallet wallet) {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Wallet> walletEntity = new HttpEntity<>(wallet, headers);
        ResponseEntity<Object> response = restTemplate
                .exchange(hlfConfig.getUrl() + "/wallet", HttpMethod.POST, walletEntity, Object.class);
        logger.info("Create Remote Wallet: " + response.getStatusCode() + " Body: " + response.getBody());
        return response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK;
    }

    private JsonArray getTransactionRemote(String transactionId) {
        String url = hlfConfig.getUrl() + "/transaction?search=externalTransactionId==" + transactionId;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        logger.info("Remote Transactions: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonObject jsonObject = new Gson().fromJson(response.getBody(), JsonObject.class);
            return jsonObject.getAsJsonArray("result");
        } else {
            throw new IllegalStateException("Unable to get transactions from blockchain");
        }
    }

    private JsonArray getTransactionLocal(String transactionId) {
        List<Transaction> transactions = findByExternalTransactionId(transactionId);
        logger.info("Transaction Response: " + transactions);
        JsonArray array = new JsonArray();
        transactions.forEach(txn -> {
            JsonElement element = gson.toJsonTree(txn);
            logger.info("Transaction Element Before: " + gson.toJson(element));
            element.getAsJsonObject().remove("id");
            logger.info("Transaction Element After: " + gson.toJson(element));
            array.add(element);
        });
        logger.info("Transaction Response: " + gson.toJson(array));
        return array;
    }

    private Double getCommission(String txnType, double txnAmt) {
        if (SourceType.RAPDRP.name().equals(txnType.toUpperCase())) {
            // Up to 3000 -> Rs. 12 (per transaction)
            // >3000 and bill amount < 49999 -> 0.4 % of transaction amount
            double commissionValue = 12.0;
            if (txnAmt > 3000) {
                commissionValue = 0.004 * txnAmt;
            }
            BigDecimal bd = new BigDecimal(commissionValue).setScale(2, RoundingMode.HALF_UP);
            return bd.doubleValue();
        } else if (SourceType.NON_RAPDRP.name().equals(txnType.toUpperCase())) {
            // Up to 2000 -> Rs. 20 (per transaction)
            // >2000 and < 49999 -> 1 % of transaction amount
            double commissionValue = 20.0;
            if (txnAmt > 2000) {
                commissionValue = 0.01 * txnAmt;
            }
            BigDecimal bd = new BigDecimal(commissionValue).setScale(2, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
        return null;
    }

    private List<Transaction> findByExternalTransactionId(String externalTransactionId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("externalTransactionId").is(externalTransactionId));
        List<Transaction> result = mongoTemplate.find(query, Transaction.class);
        logger.info("findByExternalTransactionId Resource: " + gson.toJson(result));
        return result;
    }

}
