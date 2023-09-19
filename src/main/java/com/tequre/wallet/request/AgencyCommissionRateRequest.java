package com.tequre.wallet.request;

public class AgencyCommissionRateRequest {

    private Double commissionRate;

    private Double gstRate;

    private Double tdsRate;

    private Double gstTdsRate;

    public Double getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(Double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public Double getGstRate() {
        return gstRate;
    }

    public void setGstRate(Double gstRate) {
        this.gstRate = gstRate;
    }

    public Double getTdsRate() {
        return tdsRate;
    }

    public void setTdsRate(Double tdsRate) {
        this.tdsRate = tdsRate;
    }

    public Double getGstTdsRate() {
        return gstTdsRate;
    }

    public void setGstTdsRate(Double gstTdsRate) {
        this.gstTdsRate = gstTdsRate;
    }

    @Override
    public String toString() {
        return "AgencyCommissionRateRequest{" +
                "commissionRate=" + commissionRate +
                ", gstRate=" + gstRate +
                ", tdsRate=" + tdsRate +
                ", gstTdsRate=" + gstTdsRate +
                '}';
    }
}
