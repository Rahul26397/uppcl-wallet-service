package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.enums.AgentStatus;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentStatusRequest implements Serializable {

    @Enumerated(EnumType.STRING)
    private AgentStatus status;

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "AgentStatusRequest{" +
                "status=" + status +
                '}';
    }
}
