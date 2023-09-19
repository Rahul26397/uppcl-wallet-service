package com.tequre.wallet.response.report;

import java.util.Objects;

public class DiscomAgencyReportEntry {

    private String agencyId;

    private String discom;

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getDiscom() {
        return discom;
    }

    public void setDiscom(String discom) {
        this.discom = discom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscomAgencyReportEntry that = (DiscomAgencyReportEntry) o;
        return agencyId.equals(that.agencyId) &&
                discom.equals(that.discom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agencyId, discom);
    }

    @Override
    public String toString() {
        return "DiscomAgencyReportEntry{" +
                "agencyId='" + agencyId + '\'' +
                ", discom='" + discom + '\'' +
                '}';
    }
}
