package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.enums.SourceType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletTransactionRequest implements Serializable {

    private String destinationAgentId;

    private String sourceAgentId;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    public String getDestinationAgentId() {
        return destinationAgentId;
    }

    public void setDestinationAgentId(String destinationAgentId) {
        this.destinationAgentId = destinationAgentId;
    }

    public String getSourceAgentId() {
        return sourceAgentId;
    }

    public void setSourceAgentId(String sourceAgentId) {
        this.sourceAgentId = sourceAgentId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    @Override
    public String toString() {
        return "WalletTransactionRequest{" +
                "destinationAgentId='" + destinationAgentId + '\'' +
                ", sourceAgentId='" + sourceAgentId + '\'' +
                ", amount=" + amount +
                ", sourceType=" + sourceType +
                '}';
    }
}
