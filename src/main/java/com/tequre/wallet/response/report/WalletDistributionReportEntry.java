package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"transactionId", "agencyVan", "agentVan", "paymentType", "transactionType", "amount", "transactionTime" })
public class WalletDistributionReportEntry {

    private String transactionId;

    private String agencyVan;

    private String agentVan;

    private String paymentType;

    private String transactionType;

    private Double amount;

    private Long transactionTime;

    private String uniqueId;

    private String agentName;

    public String getUniqueId() {
        return uniqueId;
    }

    public void  setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAgencyVan() {
        return agencyVan;
    }

    public void setAgencyVan(String agencyVan) {
        this.agencyVan = agencyVan;
    }

    public String getAgentVan() {
        return agentVan;
    }

    public void setAgentVan(String agentVan) {
        this.agentVan = agentVan;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Long getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Long transactionTime) {
        this.transactionTime = transactionTime;
    }
}
