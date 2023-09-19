package com.tequre.wallet.request;

import com.tequre.wallet.enums.DocumentStatus;
import com.tequre.wallet.enums.DocumentType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

public class DocumentStatusRequest {

    @Enumerated(EnumType.STRING)
    private DocumentType type;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DocumentStatusRequest{" +
                "type=" + type +
                ", status=" + status +
                '}';
    }
}
