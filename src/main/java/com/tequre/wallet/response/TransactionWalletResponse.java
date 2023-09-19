package com.tequre.wallet.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TransactionWalletResponse implements Serializable{
	
	@JsonProperty("location")
	private String location;
	
	@JsonProperty("retryAfter")
	private Integer retryAfter;

	@Override
	public String toString() {
		return "TransactionWalletResponse [location=" + location + ", retryAfter=" + retryAfter + "]";
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Integer getRetryAfter() {
		return retryAfter;
	}

	public void setRetryAfter(Integer retryAfter) {
		this.retryAfter = retryAfter;
	}
	
	

}
