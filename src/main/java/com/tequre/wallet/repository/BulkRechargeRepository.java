package com.tequre.wallet.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.tequre.wallet.data.BulkRecharge;
import com.tequre.wallet.data.BulkRechargeFile;

public interface BulkRechargeRepository extends MongoRepository<BulkRecharge,String>{
	
//	@Query(value = "{$group: {_id: null, maxId: {$max: '$id'}}}")
//    Integer findMaxId();
	
	public BulkRecharge findTopByOrderByIdDesc();
	
	List<BulkRecharge> findByJobIdAndArchivedStatus(Integer jobId,String status);
	
	List<BulkRecharge> findByJobIdAndStatus(Integer jobId,String status);
	
	//void updateStatusByJobId(Integer jobId, String newStatus);

	

}
