package com.tequre.wallet.response.report;

public class AgencyAgentsBillEntry {

    private String vanId;

    private String agencyVanId;

    private int billCount;

    private Double billCollection;

    public String getVanId() {
        return vanId;
    }

    public void setVanId(String vanId) {
        this.vanId = vanId;
    }

    public String getAgencyVanId() {
        return agencyVanId;
    }

    public void setAgencyVanId(String agencyVanId) {
        this.agencyVanId = agencyVanId;
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
