package com.tequre.wallet.request;

public class AgencyLimitsRequest {

    private int agentsLimit;

    public int getAgentsLimit() {
        return agentsLimit;
    }

    public void setAgentsLimit(int agentsLimit) {
        this.agentsLimit = agentsLimit;
    }

    @Override
    public String toString() {
        return "AgencyLimitsRequest{" +
                "agentsLimit=" + agentsLimit +
                '}';
    }
}
