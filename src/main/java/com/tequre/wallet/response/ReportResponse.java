package com.tequre.wallet.response;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportResponse implements Serializable{
	
	private Integer recordCount;
	
	private List<TransactionQueryResponse> result;

	public Integer getRecordCount() {
		return recordCount;
	}

	public void setRecordCount(Integer recordCount) {
		this.recordCount = recordCount;
	}

	public List<TransactionQueryResponse> getResult() {
		return result;
	}

	public void setResult(List<TransactionQueryResponse> result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "ReportResponse [recordCount=" + recordCount + ", result=" + result + "]";
	}

	
	
	

}
