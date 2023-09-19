package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"discom", "division", "totalAgents", "totalAgencies", "totalExternalAgents", "totalBillCollection", "totalBillCount"})
public class DiscomDivisionReportEntry {

    private String discom;

    private String division;

    private int totalAgents;

    private int totalAgencies;

    private int totalExternalAgents;

    private Double totalBillCollection;

    private int totalBillCount;

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

    public int getTotalAgents() {
        return totalAgents;
    }

    public void setTotalAgents(int totalAgents) {
        this.totalAgents = totalAgents;
    }

    public int getTotalAgencies() {
        return totalAgencies;
    }

    public void setTotalAgencies(int totalAgencies) {
        this.totalAgencies = totalAgencies;
    }

    public int getTotalExternalAgents() {
        return totalExternalAgents;
    }

    public void setTotalExternalAgents(int totalExternalAgents) {
        this.totalExternalAgents = totalExternalAgents;
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
