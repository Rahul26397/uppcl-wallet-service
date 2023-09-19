package com.tequre.wallet.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.enums.TransactionState;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "transaction")
public class Transaction {


    /**
     * "nextPageToken": "g1AAAABveJzLYWBgYMpgSmHgKy5JLCrJTq2MT8lPzkzJBYqrJJkmJxpaJCXrGluaJeqaGCYa6yamGFnqGiYmJVmkmCVZmpqbg_RywPTmgEwDaeUuKUrMK05MLsnMzwPxWUMDApx9srIAhMgeJw",
     * "recordCount": 10,
     * "result": [ {
     * "externalTransactionId": "EW1577684005825",
     * "transactionId": "5063161009266318",
     * "vanId": "UPPCL",
     * "amount": 17017,
     * "transactionType": "NON_RAPDRP",
     * "transactionTime": 1577684005830,
     * "entityId": "f186a61b843541748f302e5d78f928fa",
     * "entityType": "AGENCY",
     * "externalId": "vlevtst0001",
     * "id": "8796bd0e-b21f-4614-896f-64a346efcc95",
     * "activity": "CREDIT",
     * "mobile": "",
     * "discom": "",
     * "division": "",
     * "time": "2019-12-30 11:03:25 IST"
     * }
     * ]
     */

    @Id
    private String id;

    private String agencyId;

    private String externalTransactionId;

    private String transactionId;

    private String vanId;

    private Double amount;

    private String transactionType;

    private Long transactionTime;

    private String entityId;

    private String entityType;

    private String externalId;

    private String activity;

    private String mobile;

    private String discom;

    private String division;

    private String billId;

    private String consumerId;

    private String referenceTransactionId;

    private String divisionCode;

    @Enumerated(EnumType.STRING)
    private TransactionState transactionState = TransactionState.SUCCESS;

    private String remarks;

    private String revertedTransactionId;

    private String connectionType;

    private String agencyType;

    private String agencyVan;

    private boolean blockchainCommit;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
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

    public String getVanId() {
        return vanId;
    }

    public void setVanId(String vanId) {
        this.vanId = vanId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Long getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Long transactionTime) {
        this.transactionTime = transactionTime;
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

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getDiscom() {
        return discom;
    }

    public void setDiscom(String discom) {
        this.discom = discom;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
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

    public TransactionState getTransactionState() {
        return transactionState;
    }

    public void setTransactionState(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getRevertedTransactionId() {
        return revertedTransactionId;
    }

    public void setRevertedTransactionId(String revertedTransactionId) {
        this.revertedTransactionId = revertedTransactionId;
    }

    public boolean isBlockchainCommit() {
        return blockchainCommit;
    }

    public void setBlockchainCommit(boolean blockchainCommit) {
        this.blockchainCommit = blockchainCommit;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
