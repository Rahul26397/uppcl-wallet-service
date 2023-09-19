package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder( { "agencyType", "discom", "division", "divisionCode",
        "totalActiveAgents", "totalBillCollection", "totalBillCount" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgencyTypeDivisionReportEntry {

    private String agencyType;

    private String discom;

    private String division;

    private String divisionCode;

    private int totalActiveAgents;

    private double totalBillCollection;

    private int totalBillCount;

    public String getAgencyType() {
        return agencyType;
    }

    public void setAgencyType(String agencyType) {
        this.agencyType = agencyType;
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

    public String getDivisionCode() {
        return divisionCode;
    }

    public void setDivisionCode(String divisionCode) {
        this.divisionCode = divisionCode;
    }

    public int getTotalActiveAgents() {
        return totalActiveAgents;
    }

    public void setTotalActiveAgents(int totalActiveAgents) {
        this.totalActiveAgents = totalActiveAgents;
    }

    public double getTotalBillCollection() {
        return totalBillCollection;
    }

    public void setTotalBillCollection(double totalBillCollection) {
        this.totalBillCollection = totalBillCollection;
    }

    public int getTotalBillCount() {
        return totalBillCount;
    }

    public void setTotalBillCount(int totalBillCount) {
        this.totalBillCount = totalBillCount;
    }
}
