package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.data.Address;
import com.tequre.wallet.enums.AgentType;
import com.tequre.wallet.enums.AreaType;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterAgentRequest implements Serializable {

    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    @Enumerated(EnumType.STRING)
    private AreaType areaType;

    private String agencyType;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    private String agencyId;

    private String agencyName;

    private String empId;

    private String accountNumber;

    private String ifsc;

    @NotNull
    private List<String> discoms;

    @NotNull
    private List<String> divisions;

    @Column(unique = true)
    @NotNull
    private String email;

    @Column(unique = true)
    @NotNull
    private String userName;

    private String mobile;

    private Address address;

    private Address residenceAddress;

    private String registrationNumber;

    private String gstin;

    private String panNumber;

    private String tinNumber;

    private List<DocumentRequest> documents;

    private String imageUrl;

    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public String getAgencyType() {
        return agencyType;
    }

    public void setAgencyType(String agencyType) {
        this.agencyType = agencyType;
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getResidenceAddress() {
        return residenceAddress;
    }

    public void setResidenceAddress(Address residenceAddress) {
        this.residenceAddress = residenceAddress;
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

    public List<DocumentRequest> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentRequest> documents) {
        this.documents = documents;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
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

    public AreaType getAreaType() {
        return areaType;
    }

    public void setAreaType(AreaType areaType) {
        this.areaType = areaType;
    }

    @Override
    public String toString() {
        return "RegisterAgentRequest{" +
                "agentType=" + agentType +
                ", areaType=" + areaType +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", agencyId='" + agencyId + '\'' +
                ", agencyName='" + agencyName + '\'' +
                ", empId='" + empId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", ifsc='" + ifsc + '\'' +
                ", discoms=" + discoms +
                ", divisions=" + divisions +
                ", email='" + email + '\'' +
                ", userName='" + userName + '\'' +
                ", mobile='" + mobile + '\'' +
                ", address=" + address +
                ", residenceAddress=" + residenceAddress +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", gstin='" + gstin + '\'' +
                ", panNumber='" + panNumber + '\'' +
                ", tinNumber='" + tinNumber + '\'' +
                ", documents=" + documents +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
