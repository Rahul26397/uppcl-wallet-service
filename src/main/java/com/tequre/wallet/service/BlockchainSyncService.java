package com.tequre.wallet.service;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.tequre.wallet.config.HLFConfig;
import com.tequre.wallet.data.Lock;
import com.tequre.wallet.data.ResyncStatus;
import com.tequre.wallet.data.ResyncWalletEntry;
import com.tequre.wallet.data.Transaction;
import com.tequre.wallet.data.Wallet;
import com.tequre.wallet.enums.ResyncType;
import com.tequre.wallet.enums.TransactionType;
import com.tequre.wallet.repository.LockRepository;
import com.tequre.wallet.repository.ResyncStatusRepository;
import com.tequre.wallet.repository.TransactionRepository;
import com.tequre.wallet.repository.WalletRepository;
import com.tequre.wallet.request.UpdateWalletRequest;
import com.tequre.wallet.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlockchainSyncService {

    private final Logger logger = LoggerFactory.getLogger(BlockchainSyncService.class);

    @Autowired
    private HLFConfig hlfConfig;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LockRepository lockRepository;

    @Autowired
    private ResyncStatusRepository resyncStatusRepository;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private Gson gson;

    private static String BLOCKCHAIN_PARTIAL_SYNC_JOB = "BLOCKCHAIN_PARTIAL_SYNC_JOB";
    private static String BLOCKCHAIN_FULL_SYNC_JOB = "BLOCKCHAIN_FULL_SYNC_JOB";

    @Scheduled(cron = "${scheduler.blockchainSyncPartial:-}", zone = "Asia/Kolkata")
    public void partialResync() {
        String date = CommonUtils.getCurrentDate();
        List<Long> ranges = CommonUtils.range(date, 24);
        boolean isLocked = createLock(BLOCKCHAIN_PARTIAL_SYNC_JOB);
        try {
            if (isLocked) {
                Query query = new Query();
                query.addCriteria(Criteria.where("blockchainCommit").is(false));
                Criteria range = Criteria.where("transactionTime");
                range.gte(ranges.get(0));
                range.lte(ranges.get(1));
                query.addCriteria(range);
                List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
                ResyncStatus resyncStatus = resync(transactions, date);
                resyncStatus.setId(CommonUtils.generateUUID());
                resyncStatus.setResyncType(ResyncType.AUTOMATED);
                resyncStatusRepository.save(resyncStatus);
            }
        } finally {
            if (isLocked) {
                releaseLock(BLOCKCHAIN_PARTIAL_SYNC_JOB);
            }
        }
    }

    @Scheduled(cron = "${scheduler.blockchainSyncFull:-}", zone = "Asia/Kolkata")
    public void fullResync() {
        String date = CommonUtils.getCurrentDate();
        List<Long> ranges = CommonUtils.range(date, -30);
        boolean isLocked = createLock(BLOCKCHAIN_FULL_SYNC_JOB);
        try {
            if (isLocked) {
                Query query = new Query();
                query.addCriteria(Criteria.where("blockchainCommit").is(false));
                Criteria range = Criteria.where("transactionTime");
                range.gte(ranges.get(0));
                range.lte(ranges.get(1));
                query.addCriteria(range);
                List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
                ResyncStatus resyncStatus = resync(transactions, date);
                resyncStatus.setId(CommonUtils.generateUUID());
                resyncStatus.setResyncType(ResyncType.AUTOMATED);
                cleanupResyncReport();
                resyncStatusRepository.save(resyncStatus);
            }
        } finally {
            if (isLocked) {
                releaseLock(BLOCKCHAIN_FULL_SYNC_JOB);
            }
        }
    }

    private void cleanupResyncReport() {
        Query query = new Query();
        query.addCriteria(Criteria.where("resyncType").is(ResyncType.AUTOMATED.name()));
        mongoTemplate.remove(query, ResyncStatus.class);
    }

    public ResyncStatus resync(String fromDate, String toDate, boolean isForced) {
        String dateRange = fromDate + "-to-" + toDate;
        Query query = new Query();
        if (!isForced) {
            query.addCriteria(Criteria.where("blockchainCommit").is(false));
        }
        Criteria range = Criteria.where("transactionTime");
        range.gte(CommonUtils.toEpochMilli(fromDate));
        range.lte(CommonUtils.toEpochMilli(toDate));
        query.addCriteria(range);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        ResyncStatus resyncStatus = resync(transactions, dateRange);
        resyncStatus.setId(CommonUtils.generateUUID());
        resyncStatus.setResyncType(ResyncType.MANUAL);
        resyncStatusRepository.save(resyncStatus);
        return resyncStatus;
    }

    private ResyncStatus resync(List<Transaction> transactions, String dateRange) {
        ResyncStatus resyncStatus = new ResyncStatus();
        logger.info("Transaction Count: {}", transactions.size());
        List<Transaction> invalidTxns = new ArrayList<>();
        transactions.sort(Comparator.comparing(Transaction::getTransactionTime));
        transactions.forEach(txn -> {
            if (TransactionType.DEBIT.name().equals(txn.getActivity())) {
                try {
                    blockchainService.createTransaction(txn);
                    txn.setBlockchainCommit(true);
                    transactionRepository.save(txn);
                } catch (IllegalArgumentException ex) {
                    logger.error("Transaction already exist in blockchain: {}", gson.toJson(txn));
                    txn.setBlockchainCommit(true);
                    transactionRepository.save(txn);
                } catch (Exception ex) {
                    logger.error("Failed to persist Transaction: {}", gson.toJson(txn));
                    invalidTxns.add(txn);
                }
            } else if (TransactionType.CREDIT.name().equals(txn.getActivity())) {
                try {
                    blockchainService.createTransaction(txn);
                    txn.setBlockchainCommit(true);
                    transactionRepository.save(txn);
                } catch (IllegalArgumentException ex) {
                    logger.error("Transaction already exist in blockchain: {}", gson.toJson(txn));
                    txn.setBlockchainCommit(true);
                    transactionRepository.save(txn);
                } catch (Exception ex) {
                    logger.error("Failed to persist Transaction: {}", gson.toJson(txn));
                    invalidTxns.add(txn);
                }
            } else {
                logger.error("Invalid Transaction Activity: {}", gson.toJson(txn));
                invalidTxns.add(txn);
            }
        });
        if (!invalidTxns.isEmpty()) {
            resyncStatus.setTransactions(invalidTxns);
            resyncStatus.setTotalFailedTransactions(invalidTxns.size());
        }
        resyncStatus.setDateRange(dateRange);
        resyncStatus.setTotalTransactions(transactions.size());
        logger.info("Resync Status: {}", gson.toJson(resyncStatus));
        return resyncStatus;
    }

    private ResyncStatus resyncOptimized(List<Transaction> transactions, String dateRange) {
        ResyncStatus resyncStatus = new ResyncStatus();
        logger.info("Transaction Count: {}", transactions.size());
        Map<String, List<Transaction>> mappingByVan = transactions.stream()
                .filter(txn -> txn.getVanId() != null)
                .collect(Collectors.groupingBy(Transaction::getVanId));
        logger.info("Van Count: {}", mappingByVan.keySet().size());
        Set<String> invalidVanIds = new HashSet<>();
        List<ResyncWalletEntry> resyncWalletEntries = new ArrayList<>();
        mappingByVan.forEach((vanId, transactionForVan) -> {
            logger.info("Transactions for Van {}: {}", vanId, transactionForVan.size());
            Optional<Wallet> walletOptional = walletRepository.findById(vanId);
            List<Transaction> invalidTxns = new ArrayList<>();
            ResyncWalletEntry resyncEntry = new ResyncWalletEntry();
            resyncEntry.setVanId(vanId);
            if (walletOptional.isPresent()) {
                transactionForVan.sort(Comparator.comparing(Transaction::getTransactionTime));
                AtomicDouble creditAmt = new AtomicDouble(0d);
                AtomicDouble debitAmt = new AtomicDouble(0d);
                transactionForVan.forEach(txn -> {
                    if (TransactionType.DEBIT.name().equals(txn.getActivity())) {
                        try {
                            Transaction createdTxn = blockchainService.createTransaction(txn);
                            txn.setBlockchainCommit(true);
                            transactionRepository.save(txn);
                            debitAmt.addAndGet(createdTxn.getAmount());
                        } catch (IllegalArgumentException ex) {
                            logger.error("Transaction already exist in blockchain: {}", gson.toJson(txn));
                            txn.setBlockchainCommit(true);
                            transactionRepository.save(txn);
                        } catch (Exception ex) {
                            logger.error("Failed to persist Transaction: {}", gson.toJson(txn));
                            invalidTxns.add(txn);
                        }
                    } else if (TransactionType.CREDIT.name().equals(txn.getActivity())) {
                        try {
                            Transaction createdTxn = blockchainService.createTransaction(txn);
                            txn.setBlockchainCommit(true);
                            transactionRepository.save(txn);
                            creditAmt.addAndGet(createdTxn.getAmount());
                        } catch (IllegalArgumentException ex) {
                            logger.error("Transaction already exist in blockchain: {}", gson.toJson(txn));
                            txn.setBlockchainCommit(true);
                            transactionRepository.save(txn);
                        } catch (Exception ex) {
                            logger.error("Failed to persist Transaction: {}", gson.toJson(txn));
                            invalidTxns.add(txn);
                        }
                    } else {
                        logger.error("Invalid Transaction Activity: {}", gson.toJson(txn));
                        invalidTxns.add(txn);
                    }
                });
            } else {
                logger.error("No Wallet exist for van: {}", vanId);
                invalidVanIds.add(vanId);
                invalidTxns.addAll(transactionForVan);
            }
            if (!invalidTxns.isEmpty()) {
                resyncEntry.setTransactions(invalidTxns);
                resyncWalletEntries.add(resyncEntry);
            }
        });
        resyncStatus.setDateRange(dateRange);
        resyncStatus.setTotalTransactions(transactions.size());
        resyncStatus.setInvalidVanIds(new ArrayList<>(invalidVanIds));
        resyncStatus.setFailedRecords(resyncWalletEntries);
        logger.info("Resync Status: {}", gson.toJson(resyncStatus));
        return resyncStatus;
    }

    private boolean createLock(String lockId) {
        Boolean status = null;
        try {
            Optional<Lock> lockOptional = lockRepository.findById(lockId);
            if (lockOptional.isPresent()) {
                status = false;
            } else {
                Lock lock = new Lock();
                lock.setId(lockId);
                Lock savedLock = lockRepository.save(lock);
                if (savedLock != null) {
                    status = true;
                }
            }
        } finally {
            if (status != null) {
                return status;
            }
            return false;
        }
    }

    private void releaseLock(String lockId) {
        Optional<Lock> lockOptional = lockRepository.findById(lockId);
        if (lockOptional.isPresent()) {
            lockRepository.deleteById(lockId);
        }
    }

    private void updateWallet(Wallet wallet, Double balance) {
        Optional<Wallet> remoteOptionalWallet = blockchainService.getWalletByVan(wallet.getId());
        if (remoteOptionalWallet.isPresent()) {
            logger.info("Wallet Exist: {}", wallet.getId());
            Wallet remoteWallet = remoteOptionalWallet.get();
            // Update wallet balance
            updateWalletBalance(remoteWallet, balance);
        } else {
            // Create wallet
            logger.info("Creating Wallet: {}", wallet.getId());
            Wallet createdWallet = blockchainService.createWallet(wallet);
            logger.info("Created Wallet: {}", createdWallet.getId());
            // Update wallet status in mongo
            wallet.setBlockchainCommit(true);
            walletRepository.save(wallet);
            // Update wallet balance
            updateWalletBalance(createdWallet, balance);
        }
    }

    private void updateWalletBalance(Wallet wallet, Double balance) {
        UpdateWalletRequest updateWalletRequest = new UpdateWalletRequest();
        updateWalletRequest.setBalance(wallet.getBalance() + balance);
        updateWalletRequest.setWalletStatus(wallet.getWalletStatus());
        updateWalletRequest.setWalletType(wallet.getWalletType());
        logger.info("UpdateWalletRequest: {}", gson.toJson(updateWalletRequest));
        blockchainService.updateWallet(wallet.getId(), updateWalletRequest);
        logger.info("Wallet Balance updated to {}", updateWalletRequest.getBalance());
    }
}
