package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentPayload implements Serializable {

    private String transactionId;

    private Double amount;

    private String billId;

    private String agentId;

    private String consumerAccountId;

    private String consumerName;

    private String vanNo;

    private String discom;

    private String division;

    private String mobile;

    private String agencyVan;

    private String referenceTransactionId;

    private String connectionType;

    private String agencyType;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getConsumerAccountId() {
        return consumerAccountId;
    }

    public void setConsumerAccountId(String consumerAccountId) {
        this.consumerAccountId = consumerAccountId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public String getVanNo() {
        return vanNo;
    }

    public void setVanNo(String vanNo) {
        this.vanNo = vanNo;
    }

    public String getDiscom() {
        return discom;
    }

    public void setDiscom(String discom) {
        this.discom = discom;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAgencyVan() {
        return agencyVan;
    }

    public void setAgencyVan(String agencyVan) {
        this.agencyVan = agencyVan;
    }

    public String getReferenceTransactionId() {
        return referenceTransactionId;
    }

    public void setReferenceTransactionId(String referenceTransactionId) {
        this.referenceTransactionId = referenceTransactionId;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getAgencyType() {
        return agencyType;
    }

    public void setAgencyType(String agencyType) {
        this.agencyType = agencyType;
    }

    @Override
    public String toString() {
        return "PaymentPayload{" +
                "transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", billId='" + billId + '\'' +
                ", agentId='" + agentId + '\'' +
                ", consumerAccountId='" + consumerAccountId + '\'' +
                ", consumerName='" + consumerName + '\'' +
                ", vanNo='" + vanNo + '\'' +
                ", discom='" + discom + '\'' +
                ", division='" + division + '\'' +
                ", mobile='" + mobile + '\'' +
                ", agencyVan='" + agencyVan + '\'' +
                ", referenceTransactionId='" + referenceTransactionId + '\'' +
                ", connectionType='" + connectionType + '\'' +
                ", agencyType='" + agencyType + '\'' +
                '}';
    }
}
