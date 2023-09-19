package com.tequre.wallet.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.enums.AreaType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateUrbanMeterAgentResponse implements Serializable {

    private String agentName;

    private String mobile;

    private String email;

    private String agencyName;

    private String division;

    private String agentVan;

    @Enumerated(EnumType.STRING)
    private AreaType areaType;

    private String agentId;

    private String agencyId;

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getAgentVan() {
        return agentVan;
    }

    public void setAgentVan(String agentVan) {
        this.agentVan = agentVan;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public void setAreaType(AreaType areaType) {
        this.areaType = areaType;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }
}
