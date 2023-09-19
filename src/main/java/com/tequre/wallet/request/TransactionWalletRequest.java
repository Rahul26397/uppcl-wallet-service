package com.tequre.wallet.request;

import java.io.Serializable;

public class TransactionWalletRequest implements Serializable{
	
	private Integer amount;
    private String destinationAgentId;
    private String sourceAgentId;
    private String sourceType;
	public Integer getAmount() {
		return amount;
	}
	public void setAmount(Integer amount) {
		this.amount = amount;
	}
	public String getDestinationAgentId() {
		return destinationAgentId;
	}
	public void setDestinationAgentId(String destinationAgentId) {
		this.destinationAgentId = destinationAgentId;
	}
	public String getSourceAgentId() {
		return sourceAgentId;
	}
	public void setSourceAgentId(String sourceAgentId) {
		this.sourceAgentId = sourceAgentId;
	}
	public String getSourceType() {
		return sourceType;
	}
	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}
	@Override
	public String toString() {
		return "TransactionWalletRequest [amount=" + amount + ", destinationAgentId=" + destinationAgentId
				+ ", sourceAgentId=" + sourceAgentId + ", sourceType=" + sourceType + "]";
	}
    

}
