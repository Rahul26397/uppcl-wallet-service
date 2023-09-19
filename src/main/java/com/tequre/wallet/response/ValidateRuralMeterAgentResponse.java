package com.tequre.wallet.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.enums.AreaType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateRuralMeterAgentResponse implements Serializable {

    private String firstName;

    private String lastName;

    private String discom;

    private String agentVan;

    @Enumerated(EnumType.STRING)
    private AreaType areaType;

    private String agentId;

    private String agencyId;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDiscom() {
        return discom;
    }

    public void setDiscom(String discom) {
        this.discom = discom;
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
