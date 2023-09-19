package com.tequre.wallet.data;


import java.time.LocalDateTime;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;



@Document(collection="BulkRecharge")
public class BulkRecharge {
	
	 @Id
	 @Field("_id")
	 private String id;
	 private Integer jobId;
	 private String agencyName;
	 private String  agencyVan;
	 private String agencyId ;
	 private String discomName;
	 private String agentVan;
	 private String agentId;
	 private String agentLoginId;
	 private String agentUniqueId ;
	 private String amount;
	 private String eventId;
	 private String status;
	 private String  archivedStatus;
	 private String createdBy;
	 private String modifiedBy;
	 
	 
	 private LocalDateTime createdAt;
		
	 private LocalDateTime modifiedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getJobId() {
		return jobId;
	}

	public void setJobId(Integer jobId) {
		this.jobId = jobId;
	}

	public String getAgencyName() {
		return agencyName;
	}

	public void setAgencyName(String agencyName) {
		this.agencyName = agencyName;
	}

	public String getAgencyVan() {
		return agencyVan;
	}

	public void setAgencyVan(String agencyVan) {
		this.agencyVan = agencyVan;
	}

	public String getAgencyId() {
		return agencyId;
	}

	public void setAgencyId(String agencyId) {
		this.agencyId = agencyId;
	}

	public String getDiscomName() {
		return discomName;
	}

	public void setDiscomName(String discomName) {
		this.discomName = discomName;
	}

	public String getAgentVan() {
		return agentVan;
	}

	public void setAgentVan(String agentVan) {
		this.agentVan = agentVan;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getAgentLoginId() {
		return agentLoginId;
	}

	public void setAgentLoginId(String agentLoginId) {
		this.agentLoginId = agentLoginId;
	}

	public String getAgentUniqueId() {
		return agentUniqueId;
	}

	public void setAgentUniqueId(String agentUniqueId) {
		this.agentUniqueId = agentUniqueId;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getArchivedStatus() {
		return archivedStatus;
	}

	public void setArchivedStatus(String archivedStatus) {
		this.archivedStatus = archivedStatus;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(LocalDateTime modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	@Override
	public String toString() {
		return "BulkRecharge [id=" + id + ", jobId=" + jobId + ", agencyName=" + agencyName + ", agencyVan=" + agencyVan
				+ ", agencyId=" + agencyId + ", discomName=" + discomName + ", agentVan=" + agentVan + ", agentId="
				+ agentId + ", agentLoginId=" + agentLoginId + ", agentUniqueId=" + agentUniqueId + ", amount=" + amount
				+ ", eventId=" + eventId + ", status=" + status + ", archivedStatus=" + archivedStatus + ", createdBy="
				+ createdBy + ", modifiedBy=" + modifiedBy + ", createdAt=" + createdAt + ", modifiedAt=" + modifiedAt
				+ "]";
	}
	 
	 
	 
			 

}
