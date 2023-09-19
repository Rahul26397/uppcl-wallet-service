package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.tequre.wallet.enums.AgentType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@JsonPropertyOrder({"discom", "district", "van", "name", "agentType", "totalAgents", "totalActiveAgents", "agencyId", "currentBalanceAmount", "totalWalletRecharge", "totalBillCollection", "totalBillCount"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscomAgentReportEntry {

    private String discom;

    private String district;

    private String van;

    private String name;

    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    private long totalAgents;

    private long totalActiveAgents;

    @JsonIgnore
    private String agencyId;

    private Double currentBalanceAmount;

    private Double totalWalletRecharge;

    private Double totalBillCollection;

    private int totalBillCount;

    public String getDiscom() {
        return discom;
    }

    public void setDiscom(String discom) {
        this.discom = discom;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getVan() {
        return van;
    }

    public void setVan(String van) {
        this.van = van;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public long getTotalAgents() {
        return totalAgents;
    }

    public void setTotalAgents(long totalAgents) {
        this.totalAgents = totalAgents;
    }

    public long getTotalActiveAgents() {
        return totalActiveAgents;
    }

    public void setTotalActiveAgents(long totalActiveAgents) {
        this.totalActiveAgents = totalActiveAgents;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public Double getCurrentBalanceAmount() {
        return currentBalanceAmount;
    }

    public void setCurrentBalanceAmount(Double currentBalanceAmount) {
        this.currentBalanceAmount = currentBalanceAmount;
    }

    public Double getTotalWalletRecharge() {
        return totalWalletRecharge;
    }

    public void setTotalWalletRecharge(Double totalWalletRecharge) {
        this.totalWalletRecharge = totalWalletRecharge;
    }

    public Double getTotalBillCollection() {
        return totalBillCollection;
    }

    public void setTotalBillCollection(Double totalBillCollection) {
        this.totalBillCollection = totalBillCollection;
    }

    public int getTotalBillCount() {
        return totalBillCount;
    }

    public void setTotalBillCount(int totalBillCount) {
        this.totalBillCount = totalBillCount;
    }
}
