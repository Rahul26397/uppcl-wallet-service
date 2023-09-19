package com.tequre.wallet.request;

import com.tequre.wallet.enums.DocumentType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

public class DocumentRequest {

    @Enumerated(EnumType.STRING)
    private DocumentType type;

    private String location;

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "DocumentRequest{" +
                "type=" + type +
                ", location='" + location + '\'' +
                '}';
    }
}
