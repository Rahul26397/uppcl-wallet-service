package com.tequre.wallet.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.mongodb.core.mapping.Document;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "failed_transaction")
public class FailedTransaction extends BaseEntity {

    private String van;

    private Object payload;

    public String getVan() {
        return van;
    }

    public void setVan(String van) {
        this.van = van;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
