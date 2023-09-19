package com.tequre.wallet.repository;
import org.springframework.data.domain.Sort;
import java.util.List;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.tequre.wallet.data.BulkRecharge;
import com.tequre.wallet.data.BulkRechargeFile;

public interface BulkRechargeFileRepository extends MongoRepository<BulkRechargeFile,Integer> {
		
//	@Query(value = "{$group: {_id: null, maxJobId: {$max: '$jobId'}}}")
//    public Integer findMaxValue();
	public BulkRechargeFile findTopByOrderByJobIdDesc();
	    
    @Query(value = "{'fileName': ?0}", fields = "{'jobId': 1}")
    public Integer findJobIdByFileName(String fileName);

	public BulkRechargeFile findByJobIdAndArchivedStatus(Integer jobId,String status);
    
    public List<BulkRechargeFile> findByAgencyVanAndArchivedStatus(String agencyVan, String status,Sort sort);
    

}
