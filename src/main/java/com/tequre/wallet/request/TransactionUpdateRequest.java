package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionUpdateRequest implements Serializable {

    private String billId;

    private String consumerId;

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    @Override
    public String toString() {
        return "TransactionUpdateRequest{" +
                "billId='" + billId + '\'' +
                ", consumerId='" + consumerId + '\'' +
                '}';
    }
}
