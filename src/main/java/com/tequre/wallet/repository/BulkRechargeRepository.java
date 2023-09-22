package com.tequre.wallet.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.mongodb.client.result.UpdateResult;
import com.tequre.wallet.data.BulkRecharge;
import com.tequre.wallet.data.BulkRechargeFile;

public interface BulkRechargeRepository extends MongoRepository<BulkRecharge,String>{
	
	public BulkRecharge findTopByOrderByIdDesc();
	
	List<BulkRecharge> findByJobIdAndArchivedStatusAndStatus(String jobId,String archevedstatus, String status);
	
	List<BulkRecharge> findByJobIdAndArchivedStatus(String jobId, String status);
	
	List<BulkRecharge> findByJobIdAndStatus(String jobId,String status);
	
	List<BulkRecharge> findByJobIdAndArchivedStatusAndAgentVanNotNullAndAgencyVanNotNullAndAmountNotNull(String jobId, String archivedStatus);
	
	List<BulkRecharge> findByJobIdAndArchivedStatusAndStatusAndAgentVanNotNullAndAgencyVanNotNullAndAmountNotNull(String jobId, String archivedStatus, String status);
	
	List<BulkRecharge> findByStatus(String status);
	
	List<BulkRecharge> findByJobIdAndStatusAndAgentVanNotNullAndAgencyVanNotNullAndAmountNotNull(String jobId, String status);
	
	List<BulkRecharge> findByArchivedStatusAndStatusAndAgentVanNotNullAndAgencyVanNotNullAndAmountNotNull(String archivedstatus, String status);
}


