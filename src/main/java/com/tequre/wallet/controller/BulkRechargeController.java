package com.tequre.wallet.controller;
import com.tequre.wallet.response.BulkRechargeResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tequre.wallet.data.BulkRecharge;
import com.tequre.wallet.data.BulkRechargeFile;
import com.tequre.wallet.request.BulkRechargeRequest;
import com.tequre.wallet.service.BulkRechargeService;
import com.tequre.wallet.service.ReportService;

@RestController
@RequestMapping("/v1/bulkrecharge")
public class BulkRechargeController {
	
	@Autowired
	private BulkRechargeService bulkRechargeService;
	
	@Autowired
	private ReportService reportService;

	@RequestMapping(value = "/uploadFile", method = RequestMethod.POST, consumes = {"multipart/form-data"})
	public ResponseEntity<?> storeRecords(@RequestParam("file") MultipartFile file,
	        @RequestParam("agencyName") String agencyName,
	        @RequestParam("agencyVan") String agencyVan) throws IOException {

	    BulkRechargeRequest request = new BulkRechargeRequest();
	    request.setAgencyName(agencyName);
	    request.setAgencyVan(agencyVan);

	    String result = bulkRechargeService.storeRecords(file, request);
	    
	    BulkRechargeResponse response=new BulkRechargeResponse();
	    response.setResponse(result);

	    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}

	
	@RequestMapping(value = "/getBulkRechargeFile", method = RequestMethod.GET)
    public ResponseEntity<?> getBulkRechargeFile(@RequestParam("van") String van) throws IOException {
		
		List<BulkRechargeFile> result=bulkRechargeService.getBulkRechargeFileRecord(van);
       
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }
	
	@RequestMapping(value = "/getBulkRecharge", method = RequestMethod.GET)
    public ResponseEntity<?> getBulkRecharge(@RequestParam("jobId") String jobId,
    		@RequestParam(value="flag",required=false)String flag) throws IOException {
		
		List<BulkRecharge> result=bulkRechargeService.getBulkRechargeRecord(jobId,flag);
       
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }
	
	@RequestMapping(value = "/deleteBulkRecords", method = RequestMethod.GET)
    public ResponseEntity<?> deleteBulkRecords(@RequestParam("jobId") String jobId) throws IOException {
		
		String result=bulkRechargeService.deleteBulkRecords(jobId);
		
		 BulkRechargeResponse response=new BulkRechargeResponse();
		    response.setResponse(result);
       
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
	
	@RequestMapping(value = "/processRecharge", method = RequestMethod.GET)
    public ResponseEntity<?> processRecharge(@RequestParam("jobId") String jobId, 
    		@RequestParam(value="EmailId", required=false) String emailId) throws IOException {
		
		String result=bulkRechargeService.processRecharge(jobId,emailId);
		
		 BulkRechargeResponse response=new BulkRechargeResponse();
		    response.setResponse(result);
       
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
	
	@RequestMapping(value = "/downloadBulkRecharge", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> downloadBulkRechargeFile(@RequestParam("jobId") String jobId) throws IOException {
		return bulkRechargeService.downloadBulkRechargeFile(jobId);
  }	
	
}