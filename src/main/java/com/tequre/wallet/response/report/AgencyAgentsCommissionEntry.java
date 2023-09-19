package com.tequre.wallet.response.report;

public class AgencyAgentsCommissionEntry {

    public String transactionId;

    public String agencyType;

    public String agentAgencyVan;

    public String agentAgencyName;

    public String source;

    public String consumerId;

    public String billId;

    public Double amount;

    public String type;

    public Double gst;

    public Double tds;

    public Double gstOnTds;

    public Double commission;

    public Double netCommission;

    public String discom;

    public String division;

    public Long billTime;

    public Long commissionTime;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAgencyType() {
        return agencyType;
    }

    public void setAgencyType(String agencyType) {
        this.agencyType = agencyType;
    }

    public String getAgentAgencyVan() {
        return agentAgencyVan;
    }

    public void setAgentAgencyVan(String agentAgencyVan) {
        this.agentAgencyVan = agentAgencyVan;
    }

    public String getAgentAgencyName() {
        return agentAgencyName;
    }

    public void setAgentAgencyName(String agentAgencyName) {
        this.agentAgencyName = agentAgencyName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getGst() {
        return gst;
    }

    public void setGst(Double gst) {
        this.gst = gst;
    }

    public Double getTds() {
        return tds;
    }

    public void setTds(Double tds) {
        this.tds = tds;
    }

    public Double getGstOnTds() {
        return gstOnTds;
    }

    public void setGstOnTds(Double gstOnTds) {
        this.gstOnTds = gstOnTds;
    }

    public Double getCommission() {
        return commission;
    }

    public void setCommission(Double commission) {
        this.commission = commission;
    }

    public Double getNetCommission() {
        return netCommission;
    }

    public void setNetCommission(Double netCommission) {
        this.netCommission = netCommission;
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

    public Long getBillTime() {
        return billTime;
    }

    public void setBillTime(Long billTime) {
        this.billTime = billTime;
    }

    public Long getCommissionTime() {
        return commissionTime;
    }

    public void setCommissionTime(Long commissionTime) {
        this.commissionTime = commissionTime;
    }
}
