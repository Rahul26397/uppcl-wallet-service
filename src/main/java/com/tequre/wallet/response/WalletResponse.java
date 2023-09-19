package com.tequre.wallet.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletResponse implements Serializable {

    private String id;

    private String transactionId;

    private String vanId;

    private String txnType;

    private String txnId;

    private Double amount;

    private String transactionTime;

    private String entityId;

    private String entityType;

    private String walletId;

    private String activity;

    private String receiptNo;

    private String mobile;

    private String discom;

    private String division;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getVanId() {
        return vanId;
    }

    public void setVanId(String vanId) {
        this.vanId = vanId;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
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

    @Override
    public String toString() {
        return "WalletResponse{" +
                "id='" + id + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", vanId='" + vanId + '\'' +
                ", txnType='" + txnType + '\'' +
                ", txnId='" + txnId + '\'' +
                ", amount=" + amount +
                ", transactionTime='" + transactionTime + '\'' +
                ", entityId='" + entityId + '\'' +
                ", entityType='" + entityType + '\'' +
                ", walletId='" + walletId + '\'' +
                ", activity='" + activity + '\'' +
                ", receiptNo='" + receiptNo + '\'' +
                ", mobile='" + mobile + '\'' +
                ", discom='" + discom + '\'' +
                ", division='" + division + '\'' +
                '}';
    }
}
