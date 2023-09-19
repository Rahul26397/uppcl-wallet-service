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
@Document(collection = "agent")
public class Agent extends BaseEntity {

    @DBRef
    private User user;

    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    @Enumerated(EnumType.STRING)
    private SubAgentType subAgentType;

    @Enumerated(EnumType.STRING)
    private AreaType areaType;

    @Enumerated(EnumType.STRING)
    private AgentStatus status;

    private String agencyType;

    private String van;

    private String agencyId;

    private String agencyName;

    private String empId;

    private String uniqueId;

    private String accountNumber;

    private String ifsc;

    private List<String> discoms;

    private List<String> divisions;

    private String district;

    private Double walletLimit;

    private Double commissionRate;

    private Double gstRate;

    private Double tdsRate;

    private Double gstTdsRate;

    private Double balanceAmount;

    private List<AgentDocument> documents;

    private String registrationNumber;

    private String gstin;

    private String panNumber;

    private String tinNumber;

    private Integer agentsLimit;

    private Long totalAgents;

    private String firstName;

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

    private String lastName;

    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus;

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

    public SubAgentType getSubAgentType() {
        return subAgentType;
    }

    public void setSubAgentType(SubAgentType subAgentType) {
        this.subAgentType = subAgentType;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public void setAreaType(AreaType areaType) {
        this.areaType = areaType;
    }

    public String getVan() {
        return van;
    }

    public void setVan(String van) {
        this.van = van;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public String getAgencyType() {
        return agencyType;
    }

    public void setAgencyType(String agencyType) {
        this.agencyType = agencyType;
    }

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

    public List<String> getDiscoms() {
        return discoms;
    }

    public void setDiscoms(List<String> discoms) {
        this.discoms = discoms;
    }

    public List<String> getDivisions() {
        return divisions;
    }

    public void setDivisions(List<String> divisions) {
        this.divisions = divisions;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public Double getWalletLimit() {
        return walletLimit;
    }

    public void setWalletLimit(Double walletLimit) {
        this.walletLimit = walletLimit;
    }

    public Double getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(Double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public Double getGstRate() {
        return gstRate;
    }

    public void setGstRate(Double gstRate) {
        this.gstRate = gstRate;
    }

    public Double getTdsRate() {
        return tdsRate;
    }

    public void setTdsRate(Double tdsRate) {
        this.tdsRate = tdsRate;
    }

    public Double getGstTdsRate() {
        return gstTdsRate;
    }

    public void setGstTdsRate(Double gstTdsRate) {
        this.gstTdsRate = gstTdsRate;
    }

    public Double getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(Double balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    public List<AgentDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<AgentDocument> documents) {
        this.documents = documents;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getTinNumber() {
        return tinNumber;
    }

    public void setTinNumber(String tinNumber) {
        this.tinNumber = tinNumber;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfsc() {
        return ifsc;
    }

    public void setIfsc(String ifsc) {
        this.ifsc = ifsc;
    }

    public Integer getAgentsLimit() {
        if (agentsLimit == null && agentType == AgentType.AGENCY) {
            return 20;
        }
        return agentsLimit;
    }

    public void setAgentsLimit(Integer agentsLimit) {
        this.agentsLimit = agentsLimit;
    }

    public Long getTotalAgents() {
        return totalAgents;
    }

    public void setTotalAgents(Long totalAgents) {
        this.totalAgents = totalAgents;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    @Override
    public String toString() {
        return "Agent{" +
                "user=" + user +
                ", agentType=" + agentType +
                ", subAgentType=" + subAgentType +
                ", areaType=" + areaType +
                ", status=" + status +
                ", agencyType=" + agencyType +
                ", van='" + van + '\'' +
                ", agencyId='" + agencyId + '\'' +
                ", agencyName='" + agencyName + '\'' +
                ", empId='" + empId + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", ifsc='" + ifsc + '\'' +
                ", discoms=" + discoms +
                ", divisions=" + divisions +
                ", district='" + district + '\'' +
                ", walletLimit=" + walletLimit +
                ", balanceAmount=" + balanceAmount +
                ", documents=" + documents +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", gstin='" + gstin + '\'' +
                ", panNumber='" + panNumber + '\'' +
                ", tinNumber='" + tinNumber + '\'' +
                ", agentsLimit=" + agentsLimit +
                ", totalAgents=" + totalAgents +
                ", syncStatus=" + syncStatus +
                '}';
    }
}
