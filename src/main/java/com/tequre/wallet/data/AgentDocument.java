package com.tequre.wallet.data;

import com.tequre.wallet.enums.DocumentStatus;
import com.tequre.wallet.enums.DocumentType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

public class AgentDocument {

    @Enumerated(EnumType.STRING)
    private DocumentType type;

    private String location;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

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

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "AgentDocument{" +
                "type=" + type +
                ", location='" + location + '\'' +
                ", status=" + status +
                '}';
    }
}
