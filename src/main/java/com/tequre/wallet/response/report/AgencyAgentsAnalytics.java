package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"agencyId", "agencyName", "agencyType", "topAgentsByBillCount", "topAgentsByBillCollection"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgencyAgentsAnalytics {

    private String agencyId;

    private String agencyName;

    private String agencyType;

    private List<AgencyAgentAnalyticsRecord> topAgentsByBillCount;

    private List<AgencyAgentAnalyticsRecord> topAgentsByBillCollection;

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

    public String getAgencyType() {
        return agencyType;
    }

    public void setAgencyType(String agencyType) {
        this.agencyType = agencyType;
    }

    public List<AgencyAgentAnalyticsRecord> getTopAgentsByBillCount() {
        return topAgentsByBillCount;
    }

    public void setTopAgentsByBillCount(List<AgencyAgentAnalyticsRecord> topAgentsByBillCount) {
        this.topAgentsByBillCount = topAgentsByBillCount;
    }

    public List<AgencyAgentAnalyticsRecord> getTopAgentsByBillCollection() {
        return topAgentsByBillCollection;
    }

    public void setTopAgentsByBillCollection(List<AgencyAgentAnalyticsRecord> topAgentsByBillCollection) {
        this.topAgentsByBillCollection = topAgentsByBillCollection;
    }
}
