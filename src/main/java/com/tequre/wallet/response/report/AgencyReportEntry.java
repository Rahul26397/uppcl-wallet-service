package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"discom", "district", "agencyName", "walletRecharge", "walletRechargePreviousDay", "totalAgents",
        "totalAgentsPreviousDay", "activeAgents", "activeAgentsPreviousDay", "billCount", "billCountPreviousDay",
        "billCollection", "billCollectionPreviousDay"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgencyReportEntry {

    private String discom;

    private String district;

    private String agencyName;

    private double walletRecharge;

    private double walletRechargePreviousDay;

    private int totalAgents;

    private int totalAgentsPreviousDay;

    private int activeAgents;

    private int activeAgentsPreviousDay;

    private int billCount;

    private int billCountPreviousDay;

    private double billCollection;

    private double billCollectionPreviousDay;

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

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public double getWalletRecharge() {
        return walletRecharge;
    }

    public void setWalletRecharge(double walletRecharge) {
        this.walletRecharge = walletRecharge;
    }

    public double getWalletRechargePreviousDay() {
        return walletRechargePreviousDay;
    }

    public void setWalletRechargePreviousDay(double walletRechargePreviousDay) {
        this.walletRechargePreviousDay = walletRechargePreviousDay;
    }

    public int getTotalAgents() {
        return totalAgents;
    }

    public void setTotalAgents(int totalAgents) {
        this.totalAgents = totalAgents;
    }

    public int getTotalAgentsPreviousDay() {
        return totalAgentsPreviousDay;
    }

    public void setTotalAgentsPreviousDay(int totalAgentsPreviousDay) {
        this.totalAgentsPreviousDay = totalAgentsPreviousDay;
    }

    public int getActiveAgents() {
        return activeAgents;
    }

    public void setActiveAgents(int activeAgents) {
        this.activeAgents = activeAgents;
    }

    public int getActiveAgentsPreviousDay() {
        return activeAgentsPreviousDay;
    }

    public void setActiveAgentsPreviousDay(int activeAgentsPreviousDay) {
        this.activeAgentsPreviousDay = activeAgentsPreviousDay;
    }

    public int getBillCount() {
        return billCount;
    }

    public void setBillCount(int billCount) {
        this.billCount = billCount;
    }

    public int getBillCountPreviousDay() {
        return billCountPreviousDay;
    }

    public void setBillCountPreviousDay(int billCountPreviousDay) {
        this.billCountPreviousDay = billCountPreviousDay;
    }

    public double getBillCollection() {
        return billCollection;
    }

    public void setBillCollection(double billCollection) {
        this.billCollection = billCollection;
    }

    public double getBillCollectionPreviousDay() {
        return billCollectionPreviousDay;
    }

    public void setBillCollectionPreviousDay(double billCollectionPreviousDay) {
        this.billCollectionPreviousDay = billCollectionPreviousDay;
    }
}
