package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.enums.MeterAgentStatus;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeterAgentStatusUpdateRequest implements Serializable {

    @Enumerated(EnumType.STRING)
    private MeterAgentStatus status;

    public MeterAgentStatus getStatus() {
        return status;
    }

    public void setStatus(MeterAgentStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "MeterAgentStatusUpdateRequest{" +
                "status=" + status +
                '}';
    }
}
