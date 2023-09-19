package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferRequest implements Serializable {

    private Double amount;

    private String destinationVanId;

    private String sourceVanId;

    private String sourceAgencyId;

    private String destinationAgencyId;

    private String entityId;

    private String entityType;

    private String externalId;

    private String externalTransactionId;

    private String transactionId;

    private String transactionType;

    private String division;

    private String discom;

    private String mobile;

    private String billId;

    private String consumerId;

    private String referenceTransactionId;

    private String divisionCode;

    private String connectionType;

    private String agencyType;

    private String agencyVan;

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDestinationVanId() {
        return destinationVanId;
    }

    public void setDestinationVanId(String destinationVanId) {
        this.destinationVanId = destinationVanId;
    }

    public String getSourceVanId() {
        return sourceVanId;
    }

    public void setSourceVanId(String sourceVanId) {
        this.sourceVanId = sourceVanId;
    }

    public String getSourceAgencyId() {
        return sourceAgencyId;
    }

    public void setSourceAgencyId(String sourceAgencyId) {
        this.sourceAgencyId = sourceAgencyId;
    }

    public String getDestinationAgencyId() {
        return destinationAgencyId;
    }

    public void setDestinationAgencyId(String destinationAgencyId) {
        this.destinationAgencyId = destinationAgencyId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getDiscom() {
        return discom;
    }

    public void setDiscom(String discom) {
        this.discom = discom;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

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

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getAgencyType() {
        return agencyType;
    }

    public void setAgencyType(String agencyType) {
        this.agencyType = agencyType;
    }

    public String getAgencyVan() {
        return agencyVan;
    }

    public void setAgencyVan(String agencyVan) {
        this.agencyVan = agencyVan;
    }
}
