package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentUpdateRequest implements Serializable {

    private String accountNumber;

    private String ifsc;

    private List<String> discoms;

    private List<String> divisions;

    private String empId;

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

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    @Override
    public String toString() {
        return "AgentUpdateRequest{" +
                "accountNumber='" + accountNumber + '\'' +
                ", ifsc='" + ifsc + '\'' +
                ", discoms=" + discoms +
                ", divisions=" + divisions +
                ", empId='" + empId + '\'' +
                '}';
    }
}
