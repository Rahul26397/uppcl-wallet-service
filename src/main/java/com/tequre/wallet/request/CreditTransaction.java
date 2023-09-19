package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.enums.SourceType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditTransaction implements Serializable {

    private String agentId;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private String transactionId;

    private String walletId;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    @Override
    public String toString() {
        return "CreditTransaction{" +
                "agentId='" + agentId + '\'' +
                ", amount=" + amount +
                ", sourceType=" + sourceType +
                ", transactionId='" + transactionId + '\'' +
                ", walletId='" + walletId + '\'' +
                '}';
    }
}
