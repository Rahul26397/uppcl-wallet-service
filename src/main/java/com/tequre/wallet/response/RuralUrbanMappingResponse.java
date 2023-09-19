package com.tequre.wallet.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuralUrbanMappingResponse implements Serializable {

    private String status;

    private String discomName;

    private String zoneName;

    private String circleName;

    private String districtName;

    private String divisionName;

    private String divisionCode;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDiscomName() {
        return discomName;
    }

    public void setDiscomName(String discomName) {
        this.discomName = discomName;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getCircleName() {
        return circleName;
    }

    public void setCircleName(String circleName) {
        this.circleName = circleName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getDivisionName() {
        return divisionName;
    }

    public void setDivisionName(String divisionName) {
        this.divisionName = divisionName;
    }

    public String getDivisionCode() {
        return divisionCode;
    }

    public void setDivisionCode(String divisionCode) {
        this.divisionCode = divisionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuralUrbanMappingResponse that = (RuralUrbanMappingResponse) o;
        return status.equals(that.status) &&
                discomName.equals(that.discomName) &&
                zoneName.equals(that.zoneName) &&
                circleName.equals(that.circleName) &&
                districtName.equals(that.districtName) &&
                divisionName.equals(that.divisionName) &&
                divisionCode.equals(that.divisionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, discomName, zoneName, circleName, districtName, divisionName, divisionCode);
    }
}
