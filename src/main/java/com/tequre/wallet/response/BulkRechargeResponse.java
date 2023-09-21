package com.tequre.wallet.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkRechargeResponse implements Serializable {
	
	private String response;
	


	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return "BulkRechargeResponse [response=" + response + "]";
	}
	

}
