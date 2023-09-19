package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentRequest implements Serializable {

    private String type;

    private PaymentPayload payload;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PaymentPayload getPayload() {
        return payload;
    }

    public void setPayload(PaymentPayload payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "type=" + type +
                ", payload=" + payload +
                '}';
    }
}
