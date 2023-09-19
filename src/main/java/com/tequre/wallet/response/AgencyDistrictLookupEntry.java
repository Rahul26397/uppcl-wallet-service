package com.tequre.wallet.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgencyDistrictLookupEntry implements Serializable {

    private String agencyId;

    private String agencyName;

    private String van;

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public String getVan() {
        return van;
    }

    public void setVan(String van) {
        this.van = van;
    }
}
