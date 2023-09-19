package com.tequre.wallet.request;

import java.sql.Timestamp;





public class BulkRechargeRequest {
	
	private String agencyVan;
	
	private String agencyName;
	
	private String agencyId;

	public String getAgencyVan() {
		return agencyVan;
	}

	public void setAgencyVan(String agencyVan) {
		this.agencyVan = agencyVan;
	}

	public String getAgencyName() {
		return agencyName;
	}

	public void setAgencyName(String agencyName) {
		this.agencyName = agencyName;
	}

	public String getAgencyId() {
		return agencyId;
	}

	public void setAgencyId(String agencyId) {
		this.agencyId = agencyId;
	}

	@Override
	public String toString() {
		return "BulkRechargeRequest [agencyVan=" + agencyVan + ", agencyName=" + agencyName + ", agencyId=" + agencyId
				+ "]";
	}
	

}
