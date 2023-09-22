package com.tequre.wallet.repository;
import org.springframework.data.domain.Sort;


import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.tequre.wallet.data.BulkRechargeFile;

public interface BulkRechargeFileRepository extends MongoRepository<BulkRechargeFile,String> {
		
	public BulkRechargeFile findTopByOrderByJobIdDesc();
	    
    @Query(value = "{'fileName': ?0}", fields = "{'jobId': 1}")
    public Integer findJobIdByFileName(String fileName);

	public BulkRechargeFile findByJobIdAndArchivedStatus(String jobId,String status);
    
    public List<BulkRechargeFile> findByAgencyVanAndArchivedStatus(String agencyVan, String status,Sort sort);
    
    public List<BulkRechargeFile> findByStatus(String status);
    
   // void updateStatusToInQueueByStatus(String status);
    
 
    @Query("{'status' : 'Processing'}")
    void updateStatusToInQueueForProcessingRecords();
    

}
