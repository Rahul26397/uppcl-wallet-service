package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.enums.AreaType;
import com.tequre.wallet.enums.Discom;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateMeterAgentRequest implements Serializable {

    @NotEmpty
    private String uniqueId;

    private String discom;

    @Enumerated(EnumType.STRING)
    private AreaType areaType;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getDiscom() {
        return discom;
    }

    public void setDiscom(String discom) {
        this.discom = discom;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public void setAreaType(AreaType areaType) {
        this.areaType = areaType;
    }
}
