package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.tequre.wallet.enums.AgentType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@JsonPropertyOrder({"van", "name", "agentType", "currentBalanceAmount", "totalRechargeAmount"})
public class WalletReportEntry {

    private String van;

    // For Agents => First Name + Last Name
    // For Agencies ==> Agency Name
    private String name;

    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    private Double currentBalanceAmount;

    private Double totalRechargeAmount;

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

    public Double getCurrentBalanceAmount() {
        return currentBalanceAmount;
    }

    public void setCurrentBalanceAmount(Double currentBalanceAmount) {
        this.currentBalanceAmount = currentBalanceAmount;
    }

    public Double getTotalRechargeAmount() {
        return totalRechargeAmount;
    }

    public void setTotalRechargeAmount(Double totalRechargeAmount) {
        this.totalRechargeAmount = totalRechargeAmount;
    }
}
