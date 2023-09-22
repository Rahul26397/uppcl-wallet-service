package com.tequre.wallet.data;


import java.time.LocalDateTime;



import org.springframework.data.annotation.Id;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.mongodb.core.mapping.Document;




@Document(collection="BulkRechargeFile")
public class BulkRechargeFile {
	
	@Id
	private String jobId;
	
	private String agencyVan;
	
	private String agencyName;
	
	private String fileName;
	
	private Integer totalCount;
	
	private Integer sucessCount;
	
	private Integer errorCount;
	
	private String status;
	
	private String archivedStatus;
	
	private LocalDateTime createdAt;
	
	 private LocalDateTime modifiedAt;

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

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

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Integer totalCount) {
		this.totalCount = totalCount;
	}

	public Integer getSucessCount() {
		return sucessCount;
	}

	public void setSucessCount(Integer sucessCount) {
		this.sucessCount = sucessCount;
	}

	public Integer getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(Integer errorCount) {
		this.errorCount = errorCount;
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
		return "BulkRechargeFile [jobId=" + jobId + ", agencyVan=" + agencyVan + ", agencyName=" + agencyName
				+ ", fileName=" + fileName + ", totalCount=" + totalCount + ", sucessCount=" + sucessCount
				+ ", errorCount=" + errorCount + ", status=" + status + ", archivedStatus=" + archivedStatus
				+ ", createdAt=" + createdAt + ", modifiedAt=" + modifiedAt + "]";
	}

}
