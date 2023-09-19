package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"agentId", "agentName", "agentVan", "agencyId", "agencyName", "agencyVan", "billCount", "billCollection"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgencyAgentAnalyticsRecord {

    private String agentId;

    private String agentName;

    private String agentVan;

    private String agencyId;

    private String agencyName;

    private String agencyVan;

    private int billCount;

    private Double billCollection;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentVan() {
        return agentVan;
    }

    public void setAgentVan(String agentVan) {
        this.agentVan = agentVan;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public String getAgencyVan() {
        return agencyVan;
    }

    public void setAgencyVan(String agencyVan) {
        this.agencyVan = agencyVan;
    }

    public int getBillCount() {
        return billCount;
    }

    public void setBillCount(int billCount) {
        this.billCount = billCount;
    }

    public Double getBillCollection() {
        return billCollection;
    }

    public void setBillCollection(Double billCollection) {
        this.billCollection = billCollection;
    }
}
