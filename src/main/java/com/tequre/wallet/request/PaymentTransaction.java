package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.enums.PaymentType;
import com.tequre.wallet.enums.SourceType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentTransaction implements Serializable {

    @Enumerated(EnumType.STRING)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private Double amount;

    private String billId;

    private String agentId;

    private String walletId;

    private String consumerAccountId;

    private String consumerName;

    private String discom;

    private String division;

    private String mobile;

    private String vanNo;

    private String referenceTransactionId;

    private String divisionCode;

    private String transactionId;

    private String connectionType;

    private String agencyId;

    private String agencyType;

    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
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

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
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

    public String getVanNo() {
        return vanNo;
    }

    public void setVanNo(String vanNo) {
        this.vanNo = vanNo;
    }

    public String getReferenceTransactionId() {
        return referenceTransactionId;
    }

    public void setReferenceTransactionId(String referenceTransactionId) {
        this.referenceTransactionId = referenceTransactionId;
    }

    public String getDivisionCode() {
        return divisionCode;
    }

    public void setDivisionCode(String divisionCode) {
        this.divisionCode = divisionCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getAgencyType() {
        return agencyType;
    }

    public void setAgencyType(String agencyType) {
        this.agencyType = agencyType;
    }

    @Override
    public String toString() {
        return "PaymentTransaction{" +
                "type=" + type +
                ", sourceType=" + sourceType +
                ", amount=" + amount +
                ", billId='" + billId + '\'' +
                ", agentId='" + agentId + '\'' +
                ", walletId='" + walletId + '\'' +
                ", consumerAccountId='" + consumerAccountId + '\'' +
                ", consumerName='" + consumerName + '\'' +
                ", discom='" + discom + '\'' +
                ", division='" + division + '\'' +
                ", mobile='" + mobile + '\'' +
                ", vanNo='" + vanNo + '\'' +
                ", referenceTransactionId='" + referenceTransactionId + '\'' +
                ", divisionCode='" + divisionCode + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", connectionType='" + connectionType + '\'' +
                ", agencyId='" + agencyId + '\'' +
                ", agencyType='" + agencyType + '\'' +
                '}';
    }
}
