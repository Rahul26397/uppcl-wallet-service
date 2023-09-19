package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoucherRequest implements Serializable {

    private double amount;

    private String agentId;

    private int number;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "VoucherRequest{" +
                "amount=" + amount +
                ", agentId='" + agentId + '\'' +
                ", number=" + number +
                '}';
    }
}
