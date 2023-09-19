package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"discom", "zone", "district", "circle", "division", "agencyName",
        "agencyDistrict", "urbanActiveAgents", "urbanBillCount", "urbanBillCollection", "ruralActiveAgents",
        "ruralBillCount", "ruralBillCollection"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgencyDivisionReportEntry {

    private String discom;

    private String division;

    private String agencyName;

    private String agencyDistrict;

    private String zone;

    private String district;

    private String circle;

    private int urbanActiveAgents;

    private int urbanBillCount;

    private double urbanBillCollection;

    private int ruralActiveAgents;

    private int ruralBillCount;

    private double ruralBillCollection;

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

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCircle() {
        return circle;
    }

    public void setCircle(String circle) {
        this.circle = circle;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public String getAgencyDistrict() {
        return agencyDistrict;
    }

    public void setAgencyDistrict(String agencyDistrict) {
        this.agencyDistrict = agencyDistrict;
    }

    public int getUrbanActiveAgents() {
        return urbanActiveAgents;
    }

    public void setUrbanActiveAgents(int urbanActiveAgents) {
        this.urbanActiveAgents = urbanActiveAgents;
    }

    public int getUrbanBillCount() {
        return urbanBillCount;
    }

    public void setUrbanBillCount(int urbanBillCount) {
        this.urbanBillCount = urbanBillCount;
    }

    public double getUrbanBillCollection() {
        return urbanBillCollection;
    }

    public void setUrbanBillCollection(double urbanBillCollection) {
        this.urbanBillCollection = urbanBillCollection;
    }

    public int getRuralActiveAgents() {
        return ruralActiveAgents;
    }

    public void setRuralActiveAgents(int ruralActiveAgents) {
        this.ruralActiveAgents = ruralActiveAgents;
    }

    public int getRuralBillCount() {
        return ruralBillCount;
    }

    public void setRuralBillCount(int ruralBillCount) {
        this.ruralBillCount = ruralBillCount;
    }

    public double getRuralBillCollection() {
        return ruralBillCollection;
    }

    public void setRuralBillCollection(double ruralBillCollection) {
        this.ruralBillCollection = ruralBillCollection;
    }
}
