package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder( { "agencyType", "agencyName",
        "discom", "zone", "circle", "district", "division",
        "totalActiveAgents", "totalActiveAgentsPreviousDay",
        "totalBillCount", "totalBillCountPreviousDay",
        "totalBillCollection", "totalBillCollectionPreviousDay" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgencyTypeCollectionReportEntry {

    private String agencyType;

    private String agencyName;

    private String discom;

    private String zone;

    private String circle;

    private String district;

    private String division;

    private int totalActiveAgents;

    private int totalActiveAgentsPreviousDay;

    private int totalBillCount;

    private int totalBillCountPreviousDay;

    private double totalBillCollection;

    private double totalBillCollectionPreviousDay;

    public String getAgencyType() {
        return agencyType;
    }

    public void setAgencyType(String agencyType) {
        this.agencyType = agencyType;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public String getDiscom() {
        return discom;
    }

    public void setDiscom(String discom) {
        this.discom = discom;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getCircle() {
        return circle;
    }

    public void setCircle(String circle) {
        this.circle = circle;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public int getTotalActiveAgents() {
        return totalActiveAgents;
    }

    public void setTotalActiveAgents(int totalActiveAgents) {
        this.totalActiveAgents = totalActiveAgents;
    }

    public int getTotalActiveAgentsPreviousDay() {
        return totalActiveAgentsPreviousDay;
    }

    public void setTotalActiveAgentsPreviousDay(int totalActiveAgentsPreviousDay) {
        this.totalActiveAgentsPreviousDay = totalActiveAgentsPreviousDay;
    }

    public int getTotalBillCount() {
        return totalBillCount;
    }

    public void setTotalBillCount(int totalBillCount) {
        this.totalBillCount = totalBillCount;
    }

    public int getTotalBillCountPreviousDay() {
        return totalBillCountPreviousDay;
    }

    public void setTotalBillCountPreviousDay(int totalBillCountPreviousDay) {
        this.totalBillCountPreviousDay = totalBillCountPreviousDay;
    }

    public double getTotalBillCollection() {
        return totalBillCollection;
    }

    public void setTotalBillCollection(double totalBillCollection) {
        this.totalBillCollection = totalBillCollection;
    }

    public double getTotalBillCollectionPreviousDay() {
        return totalBillCollectionPreviousDay;
    }

    public void setTotalBillCollectionPreviousDay(double totalBillCollectionPreviousDay) {
        this.totalBillCollectionPreviousDay = totalBillCollectionPreviousDay;
    }
}
