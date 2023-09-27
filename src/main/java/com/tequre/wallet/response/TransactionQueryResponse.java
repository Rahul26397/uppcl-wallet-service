package com.tequre.wallet.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TransactionQueryResponse {
	@JsonProperty("_id")
    private Id id;
	
	@JsonProperty("count")
    private Long count;
	
	@JsonProperty("totalAmount")
    private Double totalAmount;

    // Getters and setters for the fields
    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    // Inner class for the "_id" object
    public static class Id {
    	
    	@JsonProperty("agencyType")
        private String agencyType;
    	@JsonProperty("discom")
        private String discom;
    	@JsonProperty("division")
        private String division;

        // Getters and setters for the "_id" fields
        public String getAgencyType() {
            return agencyType;
        }

        public void setAgencyType(String agencyType) {
            this.agencyType = agencyType;
        }

        public String getDiscom() {
            return discom;
        }

        public void setDiscom(String discom) {
            this.discom = discom;
        }

        public String getDivision() {
            return division;
        }

        public void setDivision(String division) {
            this.division = division;
        }
    }
}

