package com.tequre.wallet.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.tequre.wallet.enums.ResyncType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;
import java.util.List;

@JsonPropertyOrder({ "id", "resyncType", "dateRange", "totalTransactions", "totalFailedTransactions", "transactions", "invalidVanIds", "failedRecords"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "resync_status")
public class ResyncStatus implements Serializable {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private ResyncType resyncType;

    private String dateRange;

    private int totalTransactions;

    private int totalFailedTransactions;

    private List<Transaction> transactions;

    private List<ResyncWalletEntry> failedRecords;

    private List<String> invalidVanIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ResyncType getResyncType() {
        return resyncType;
    }

    public void setResyncType(ResyncType resyncType) {
        this.resyncType = resyncType;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public int getTotalFailedTransactions() {
        return totalFailedTransactions;
    }

    public void setTotalFailedTransactions(int totalFailedTransactions) {
        this.totalFailedTransactions = totalFailedTransactions;
    }

    public List<ResyncWalletEntry> getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(List<ResyncWalletEntry> failedRecords) {
        this.failedRecords = failedRecords;
    }

    public List<String> getInvalidVanIds() {
        return invalidVanIds;
    }

    public void setInvalidVanIds(List<String> invalidVanIds) {
        this.invalidVanIds = invalidVanIds;
    }
}
