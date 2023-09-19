package com.tequre.wallet.response.report;

import com.tequre.wallet.data.Agent;

import java.util.Set;

public class AgencyAgents {

    private Agent agency;

    private Set<String> agentVans;

    private Set<String> agentVansPrevious;

    public Agent getAgency() {
        return agency;
    }

    public void setAgency(Agent agency) {
        this.agency = agency;
    }

    public Set<String> getAgentVans() {
        return agentVans;
    }

    public void setAgentVans(Set<String> agentVans) {
        this.agentVans = agentVans;
    }

    public Set<String> getAgentVansPrevious() {
        return agentVansPrevious;
    }

    public void setAgentVansPrevious(Set<String> agentVansPrevious) {
        this.agentVansPrevious = agentVansPrevious;
    }
}
