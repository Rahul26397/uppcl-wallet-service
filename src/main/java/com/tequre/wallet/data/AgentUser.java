package com.tequre.wallet.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.tequre.wallet.enums.AgentStatus;
import com.tequre.wallet.enums.AgentType;
import com.tequre.wallet.enums.AreaType;
import com.tequre.wallet.enums.SubAgentType;
import com.tequre.wallet.enums.SyncStatus;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.List;

@JsonInclude(Include.NON_NULL)
@Document(collection = "agent_user")
public class AgentUser extends BaseEntity {

    @DBRef
    private User user;

    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    private String agentId;

    private String vanId;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getVanId() {
        return vanId;
    }

    public void setVanId(String vanId) {
        this.vanId = vanId;
    }

    @Override
    public String toString() {
        return "AgentUser{" +
                "user=" + user +
                ", agentType=" + agentType +
                ", agentId='" + agentId + '\'' +
                ", vanId='" + vanId + '\'' +
                '}';
    }
}
