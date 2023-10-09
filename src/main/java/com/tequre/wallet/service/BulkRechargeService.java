package com.tequre.wallet.service;
import java.text.SimpleDateFormat;


import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;



import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.repository.BulkRechargeFileRepository;
import com.tequre.wallet.repository.BulkRechargeRepository;
import com.tequre.wallet.repository.EventRepository;
import com.tequre.wallet.request.BulkRechargeRequest;
import com.tequre.wallet.request.TransactionWalletRequest;
import com.tequre.wallet.response.TokenResponse;
import com.tequre.wallet.data.BulkRechargeFile;
import com.tequre.wallet.data.Wallet;
import com.tequre.wallet.event.Event;
import com.tequre.wallet.data.BulkRecharge;
import com.tequre.wallet.response.TransactionWalletResponse;
import com.tequre.wallet.utils.Constants;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
@Service
public class BulkRechargeService {
		
	private final Logger logger = LoggerFactory.getLogger(BulkRechargeService.class);
	
	 @Autowired 
	 private JavaMailSender javaMailSender;
	 
	
	@Autowired
	private BulkRechargeFileRepository bulkRechargeFileRepo;
	
	@Autowired
	private BulkRechargeRepository bulkRechargeRepo;
	
	@Autowired
	AgentRepository agentRepo;
	
	@Autowired
	EventRepository eventRepo;
	
	private final MongoTemplate mongoTemplate;

    @Autowired
    public BulkRechargeService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
	
	public String storeRecords(MultipartFile file,BulkRechargeRequest request) throws IOException{
		String extension=null;
		String originalFilename = file.getOriginalFilename();
	    if (originalFilename != null) {
	        int lastIndex = originalFilename.lastIndexOf('.');
	        if (lastIndex != -1) {
	            extension= originalFilename.substring(lastIndex + 1);
	        }
	        
	        else {
	        	return "Invalid file format.";
	        }
	    }
	        else {
	        	return "File not found.";
	        }
	    
	    if(extension.equalsIgnoreCase("xlsx")) {
		Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = dateFormat.format(currentDate);
		BulkRechargeFile record =bulkRechargeFileRepo.findTopByOrderByJobIdDesc();
		//logger.info("formattedDate: " + formattedDate);
		formattedDate = formattedDate.replaceAll("-", "").trim();
		
		String jobId=null;
		if(record==null) {
			jobId = formattedDate.replaceAll("-", "").concat("1");
		}
		else {
			Integer totalRecord=((int)bulkRechargeFileRepo.count())+1;
			String nextDigit=totalRecord.toString();
			jobId = formattedDate.concat(nextDigit);	
		}
		logger.info("jobId generated "+jobId);

		logger.info("Storing jobId :"+jobId+ " details");
		BulkRechargeFile bulkRechargeFile=new BulkRechargeFile();
		String fileName=request.getAgencyVan()+"_"+jobId;
		bulkRechargeFile.setFileName(fileName);
		bulkRechargeFile.setAgencyName(request.getAgencyName());
		bulkRechargeFile.setAgencyVan(request.getAgencyVan());
		bulkRechargeFile.setErrorCount(0);
		bulkRechargeFile.setSucessCount(0);
		bulkRechargeFile.setStatus("Not Started");
		bulkRechargeFile.setArchivedStatus("N");
		bulkRechargeFile.setJobId(jobId);
		
		
		try (InputStream inputStream = file.getInputStream()) {
            logger.info("Reading file data for job: "+jobId);
            Workbook workbook = new XSSFWorkbook(inputStream);

            Sheet sheet = workbook.getSheetAt(0); 
            Iterator<Row> rowIterator = sheet.iterator();
            
            //header validation
            int headerIndex=0;
            if (rowIterator.hasNext()) {
               Row row= rowIterator.next();
               for(Cell cell:row) {
            	  
            	   headerIndex++;
            	   if(headerIndex==1) {
            		   if(!cell.getStringCellValue().toString().equalsIgnoreCase(Constants.DISCOM_NAME)) {
            			   bulkRechargeFile.setTotalCount(0);
            			   bulkRechargeFile.setStatus("Rejected");
            			   bulkRechargeFile.setRemark("Invalid file format");
            			   bulkRechargeFile.setCreatedAt(LocalDateTime.now());
            	           bulkRechargeFile.setModifiedAt(LocalDateTime.now());
            	           bulkRechargeFileRepo.save(bulkRechargeFile);
            			  return "Invalid file format"; 
            		   }
            		   
            	   }
            	   else if(headerIndex==2) {
            		   if(!cell.getStringCellValue().toString().equalsIgnoreCase(Constants.AGENCY_NAME)) {
            			   bulkRechargeFile.setTotalCount(0);
            			   bulkRechargeFile.setStatus("Rejected");
            			   bulkRechargeFile.setRemark("Invalid file format");
            			   bulkRechargeFile.setCreatedAt(LocalDateTime.now());
            	           bulkRechargeFile.setModifiedAt(LocalDateTime.now());
            	           bulkRechargeFileRepo.save(bulkRechargeFile);
             			  return "Invalid file format"; 
             		   }
            	   }
            	   else if(headerIndex==3) {
            		   if(!cell.getStringCellValue().toString().equalsIgnoreCase(Constants.AGENCY_VAN)) {
            			   bulkRechargeFile.setTotalCount(0);
            			   bulkRechargeFile.setStatus("Rejected");
            			   bulkRechargeFile.setRemark("Invalid file format");
            			   bulkRechargeFile.setCreatedAt(LocalDateTime.now());
            	           bulkRechargeFile.setModifiedAt(LocalDateTime.now());
            	           bulkRechargeFileRepo.save(bulkRechargeFile);
              			  return "Invalid file format"; 
              		   }
            	   }
            	   else if(headerIndex==4) {
            		   if(!cell.getStringCellValue().toString().equalsIgnoreCase(Constants.AGENT_VAN)) {
            			   bulkRechargeFile.setTotalCount(0);
            			   bulkRechargeFile.setStatus("Rejected");
            			   bulkRechargeFile.setRemark("Invalid file format");
            			   bulkRechargeFile.setCreatedAt(LocalDateTime.now());
            	           bulkRechargeFile.setModifiedAt(LocalDateTime.now());
            	           bulkRechargeFileRepo.save(bulkRechargeFile);
               			  return "Invalid file format"; 
               		   }
            	   }
            	   else if(headerIndex==5) {
            		   if(!cell.getStringCellValue().toString().equalsIgnoreCase(Constants.AGENT_LOGIN_ID)) {
            			   bulkRechargeFile.setTotalCount(0);
            			   bulkRechargeFile.setStatus("Rejected");
            			   bulkRechargeFile.setRemark("Invalid file format");
            			   bulkRechargeFile.setCreatedAt(LocalDateTime.now());
            	           bulkRechargeFile.setModifiedAt(LocalDateTime.now());
            	           bulkRechargeFileRepo.save(bulkRechargeFile);
               			  return "Invalid file format"; 
               		   }
            	   }
            	   else if(headerIndex==6) {
            		   if(!cell.getStringCellValue().toString().equalsIgnoreCase(Constants.AGENT_UNIQUE_ID)) {
            			   bulkRechargeFile.setTotalCount(0);
            			   bulkRechargeFile.setStatus("Rejected");
            			   bulkRechargeFile.setRemark("Invalid file format");
            			   bulkRechargeFile.setCreatedAt(LocalDateTime.now());
            	           bulkRechargeFile.setModifiedAt(LocalDateTime.now());
            	           bulkRechargeFileRepo.save(bulkRechargeFile);
               			  return "Invalid file format"; 
               		   }
            	   }
            	   else if(headerIndex==7) {
            		   String resultString = cell.getStringCellValue().toString().replaceAll("\\s*\\(.*\\)\\s*", "");
            		   if(!resultString.equalsIgnoreCase(Constants.RECHARGEABLE_AMOUNT)) {
            			   bulkRechargeFile.setTotalCount(0);
            			   bulkRechargeFile.setStatus("Rejected");
            			   bulkRechargeFile.setRemark("Invalid file format");
            			   bulkRechargeFile.setCreatedAt(LocalDateTime.now());
            	           bulkRechargeFile.setModifiedAt(LocalDateTime.now());
            	           bulkRechargeFileRepo.save(bulkRechargeFile);
               			  return "Invalid file format"; 
               		   }
            	   }
            	   
               }
            }
            logger.info("storing data in database");
            while (rowIterator.hasNext()) {
            	 
            	BulkRecharge bulkRecharge=new BulkRecharge();
            	int recordFlag=1;
                Row row = rowIterator.next();
                boolean isRowBlank = isRowBlank(row);
                if(isRowBlank) {
                	continue;
                }
                int columnIndex=0;
                // Iterate through each cell in the row
                for (Cell cell : row) {
                	columnIndex++;
                    // Get the cell value based on the cell type
                    switch (cell.getCellType()) {
                        case STRING:
                            System.out.print(cell.getStringCellValue() + "\t");
                            logger.info("cell content "+cell.getStringCellValue());
                            if(columnIndex==1) {
                            	bulkRecharge.setDiscomName(cell.getStringCellValue());
                            }
                            if(columnIndex==2) {
                            	String agencyName=cell.getStringCellValue().trim();
                            	if(agencyName.equalsIgnoreCase(request.getAgencyName())) {
                            		System.out.println("inside loop");
                            		bulkRecharge.setAgencyName(cell.getStringCellValue());
                            	}
                            	else {
                            		bulkRecharge.setAgencyName(cell.getStringCellValue());
                            		bulkRecharge.setStatus("FAILED");
                            		bulkRecharge.setRemark("Agency Name doesn't matched with login Agency");
                            		recordFlag=0;
                            		
                            	}
                            	
                            }
                            if(columnIndex==3) {
                            	String agencyVan=cell.getStringCellValue().trim();
                            	if(agencyVan.equalsIgnoreCase(request.getAgencyVan())) {
                            		System.out.println("i am inside the good");
                            	bulkRecharge.setAgencyVan(agencyVan);
                            	 ObjectMapper objectMapper = new ObjectMapper();
                            	 String agencyId=agentRepo.findAgentIdByVan(agencyVan);
                            	 if(agencyId!=null) {
                                 JsonNode jsonNode = objectMapper.readTree(agencyId);
                                 String idValue = jsonNode.get("_id").asText();
                                 logger.info("agencyId based on van "+idValue);
                                // System.out.println("current agencyId "+idValue);
                                 bulkRecharge.setAgencyId(idValue);
                            	 }
                            }
                            	else {
                            		if(recordFlag!=0) {
                            	bulkRecharge.setAgencyVan(agencyVan);	
                            	bulkRecharge.setStatus("FAILED");
                        		bulkRecharge.setRemark("Agency Van doesn't matched with login Agency");
                        		recordFlag=0;
                            		}
                        		
                            }
                            	
                            }
                            if(columnIndex==4) {
                            	bulkRecharge.setAgentVan(cell.getStringCellValue());
                            	
                            	//String agentId=agentRepo.findAgentIdByVan(cell.getStringCellValue());	
                            }
                            if(columnIndex==5) {
                            	bulkRecharge.setAgentLoginId(cell.getStringCellValue());
                            	
                            	
                            }
                            if(columnIndex==6) {
                            	String agentUniqueId=cell.getStringCellValue().trim();
                            	bulkRecharge.setAgentUniqueId(agentUniqueId);                          	
                            	String agentId=agentRepo.findAgentIdByVanAndAgencyIdAndUniqueId(bulkRecharge.getAgentVan(), bulkRecharge.getAgencyId(),agentUniqueId);
                            	if(agentId!=null) {
                               	 ObjectMapper objectMapper = new ObjectMapper();
                                    JsonNode jsonNode = objectMapper.readTree(agentId);
                                    String idValue = jsonNode.get("_id").asText();
                                    logger.info("agentId based on van "+idValue);
                               	bulkRecharge.setAgentId(idValue);
                               	}
                            	else {
                            		if(recordFlag!=0) {
                                	bulkRecharge.setStatus("FAILED");
                            		bulkRecharge.setRemark("Agent doesn't belong with logged in Agency");
                            		recordFlag=0;
                            		}
                            		
                                }
                            }
                            if(columnIndex==7) {
                            	String amount=cell.getStringCellValue().replaceAll(",", "");
                            	System.out.println("amount value from string "+amount);
                            	Double numericValue=Double.parseDouble(amount);
                            	System.out.println("amount value from double "+numericValue);
                            	if (numericValue >= 0 && Math.abs(numericValue - Math.floor(numericValue)) < 1e-6) {
                            	    // The number has no fractional part, so round it to an integer
                            		int data = (int) Math.round(numericValue);
                            		Integer intValue=data;
                                	bulkRecharge.setAmount(intValue.toString());
                
                            	} else {
                            		bulkRecharge.setAmount(numericValue.toString());
                            		bulkRecharge.setStatus("FAILED");
                            		bulkRecharge.setRemark("Invalid amount");
                            		recordFlag=0;
                            		break;
                            	}
                            }
                            
                            break;
                        case NUMERIC:
                        	logger.info("cell content "+cell.getNumericCellValue());
                            System.out.print(cell.getNumericCellValue() + "\t");
                            if(columnIndex==7) {
                            	
                            	Double numericValue = cell.getNumericCellValue();
                            	if (numericValue >= 0 && Math.abs(numericValue - Math.floor(numericValue)) < 1e-6) {
                            	    // The number has no fractional part, so round it to an integer
                            		int data = (int) Math.round(cell.getNumericCellValue());
                            		Integer intValue=data;
                                	bulkRecharge.setAmount(intValue.toString());
                
                            	} else {
                            		bulkRecharge.setAmount(numericValue.toString());
                            		bulkRecharge.setStatus("FAILED");
                            		bulkRecharge.setRemark("Invalid amount");
                            		recordFlag=0;
                            		break;
                            	}
                            	
                            }
                            break;
                        case BOOLEAN:
                            System.out.print(cell.getBooleanCellValue() + "\t");
                            break;
                        case BLANK:
                            System.out.print("[BLANK]\t");
                            if(columnIndex==2) {
                        		bulkRecharge.setStatus("FAILED");
                        		bulkRecharge.setRemark("Agency Name must be there in record");
                        		recordFlag=0;
                            }
                            if(columnIndex==3) {
                            	bulkRecharge.setStatus("FAILED");
                        		bulkRecharge.setRemark("Agency Van must be there in record");
                        		recordFlag=0;
                            }
                            if(columnIndex==4) {
                            	bulkRecharge.setStatus("FAILED");
                        		bulkRecharge.setRemark("Agent Van must be there in record");
                        		recordFlag=0;
                            }
                            if(columnIndex==6) {
                            	bulkRecharge.setStatus("FAILED");
                        		bulkRecharge.setRemark("Agent UniqueId must be there in record");
                        		recordFlag=0;
                            }
                            if(columnIndex==7) {
                            	bulkRecharge.setStatus("FAILED");
                        		bulkRecharge.setRemark("Amount must be there in record");
                        		recordFlag=0;
                            }
                            break;
                        default:
                            System.out.print("[UNKNOWN]\t");
                            break;
                    }
                    
                }
                
                logger.info("currently to agencyId "+request.getAgencyId()); 
                if(recordFlag==1) {
                bulkRecharge.setStatus("Not Started");
                }
                
                bulkRecharge.setArchivedStatus("N");
                bulkRecharge.setCreatedBy(request.getAgencyName());
                bulkRecharge.setModifiedBy(request.getAgencyName());
                bulkRecharge.setJobId(jobId);
                bulkRecharge.setCreatedAt(LocalDateTime.now());
                bulkRecharge.setModifiedAt(LocalDateTime.now());
                logger.info("bulkRecharge record "+bulkRecharge);
                bulkRechargeRepo.save(bulkRecharge);
                
            }
            Integer recordCount=bulkRechargeRepo.findByJobIdAndArchivedStatus(jobId,"N").size();
            logger.info("total records in excel "+recordCount);
            bulkRechargeFile.setTotalCount(recordCount);
            bulkRechargeFile.setCreatedAt(LocalDateTime.now());
            bulkRechargeFile.setModifiedAt(LocalDateTime.now());
            bulkRechargeFileRepo.save(bulkRechargeFile);
            
            workbook.close();
        }    
		return "Records inserted Successfully";
	    }
	    else {
	    	return "unsupported file type";
	    }
	}
	
	public List<BulkRechargeFile> getBulkRechargeFileRecord(String van){
		Sort sort = Sort.by(Sort.Order.desc("createdAt"));
		return bulkRechargeFileRepo.findByAgencyVanAndArchivedStatus(van,"N",sort);
	}
	
	public List<BulkRecharge> getBulkRechargeRecord(String jobId, String flag){
		if(flag==null) {
		return bulkRechargeRepo.findByJobIdAndArchivedStatus(jobId,"N");
		}
		else if(flag.equalsIgnoreCase("SUCCESS")) {
			return bulkRechargeRepo.findByJobIdAndStatus(jobId, "SUCCESS");
		}
		else if(flag.equalsIgnoreCase("FAILED")) {
			return bulkRechargeRepo.findByJobIdAndStatus(jobId, "FAILED");
		}
		return null;
	}
	
	public String deleteBulkRecords(String jobId) {
		
		BulkRechargeFile bulkRechargeFile=bulkRechargeFileRepo.findByJobIdAndArchivedStatus(jobId,"N");
		if(bulkRechargeFile!=null) {
		bulkRechargeFile.setArchivedStatus("Y");
		bulkRechargeFileRepo.save(bulkRechargeFile);
		List<BulkRecharge> bulkRecharge=bulkRechargeRepo.findByJobIdAndArchivedStatus(jobId,"N");
		if(bulkRecharge!=null) {
		for(int i=0;i<bulkRecharge.size();i++) {
			BulkRecharge data=bulkRecharge.get(i);
			data.setArchivedStatus("Y");
			bulkRechargeRepo.save(data);
		}
		}
	}else {
		return "Records not available";
	}
		return "Records deleted Successfully";
	}
	
	public String processRecharge(String jobId,String emailId) throws JsonProcessingException {
		Optional<BulkRechargeFile> bulkRechargeFile=bulkRechargeFileRepo.findById(jobId);
		
		BulkRechargeFile data=null;
		if(bulkRechargeFile.isPresent()) {
			data=bulkRechargeFile.get();
			if(data.getStatus().equalsIgnoreCase("Not Started")) {
			data.setStatus("Processing");
			bulkRechargeFileRepo.save(data);
			updateStatusByJobId(jobId,"INITIATED");          
			return "Your request with request id "+jobId+" has been received. Please click refresh button to get updated status";
			//return "Please check again after sometime -> Please click refresh button to get updated status";
	
	}else {
				return " you are not allowed to request again";
			}
			
		} else {
		return "no record available";
	}
    }
	
	
	@Scheduled(fixedRate = 30000)
    public void processRechargeScheduled() throws JsonProcessingException {
		     logger.info("schedular is called");
             List<BulkRechargeFile> bulkRechargeFiles=bulkRechargeFileRepo.findByStatus("Processing");
             updateStatusToInQueueForProcessingRecords();
        
             
             int success=0;
		     int failed=0;
		     int flag=1;
		     BulkRechargeFile data=null;
		    	 for(int j=0;j<bulkRechargeFiles.size();j++) {
		    		 
		    		 logger.info("current bulkRechargefile record to process "+bulkRechargeFiles.get(j));
			    data=bulkRechargeFiles.get(j);
             //	List<BulkRecharge> records=bulkRechargeRepo.findByJobIdAndArchivedStatusAndStatus(jobId, "N","INITIATED");
			    List<BulkRecharge> records=bulkRechargeRepo.findByJobIdAndArchivedStatusAndStatus(data.getJobId(), "N","INITIATED");
		    for(int i=0;i<records.size();i++) {
		    	int tokenFlag=0;
		    	logger.info("current record of bulkRecharge "+records.get(i));
			flag=1;
			BulkRecharge record=records.get(i);
		
		RestTemplate restTemplate = new RestTemplate();
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(Constants.TOKEN_URL)
                .queryParam("grant_type", "password")
                .queryParam("username", Constants.TOKEN_USER)
                .queryParam("password", Constants.TOKEN_PASSWORD);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + Constants.basicAuthorizationToken);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<TokenResponse> responseEntity=null;
        
    try {
         responseEntity = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                TokenResponse.class
        );
         if (responseEntity.getStatusCode() == HttpStatus.OK) {
         	logger.info("token executed successfully with accessToken: "+responseEntity.getBody().getAccessToken());    
         } 
    }catch(Exception e) {
    	tokenFlag=1;
    	record.setRemark("internal error");
    }

     
       
        
        HttpHeaders mainheaders = new HttpHeaders();
        mainheaders.setContentType(MediaType.APPLICATION_JSON);
        if(responseEntity!=null) {
        mainheaders.set("Authorization", "Bearer " + responseEntity.getBody().getAccessToken());
        }
        Integer amount=Integer.parseInt(record.getAmount());
        TransactionWalletRequest request= new TransactionWalletRequest();
        request.setAmount(amount);
        request.setDestinationAgentId(record.getAgentId());
        request.setSourceAgentId(record.getAgencyId());
        request.setSourceType("WALLET");
        
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(request);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, mainheaders);

        ResponseEntity<TransactionWalletResponse> response=null;
       Double balance= findBalanceById(record.getAgencyVan());
       int intValue = balance.intValue();
       Integer walletBalance = Integer.valueOf(intValue);
       if(walletBalance<Integer.parseInt(record.getAmount())) {
    	   record.setRemark("insufficient balance");
    	   tokenFlag=1;
       }

        System.out.println("tokenflag status "+tokenFlag);
        if(record.getAgencyId()==null ||record.getAgentId()==null ||record.getAmount()==null || tokenFlag==1) {
        	System.out.println("inside tokenflag status "+tokenFlag);
          flag=0;
       	  record.setStatus("FAILED");
       	  failed++;
       	  bulkRechargeRepo.save(record);
        }
        else {
        	try {
        	response = restTemplate.exchange(Constants.API_URL, HttpMethod.POST, requestEntity, TransactionWalletResponse.class);
        	System.out.println("outside tokenflag status "+tokenFlag);
            logger.info("transaction completed successfully ");
        	}catch(Exception e) {
        		flag=0;
        		record.setStatus("FAILED");
        		record.setRemark(e.getMessage());
        		bulkRechargeRepo.save(record);
        	}
        }
      
      if(flag==1) {
        System.out.println("API Response: " + response.getBody());
        String[] parts = response.getBody().getLocation().split("/");
        String desiredString = parts[parts.length - 1];
        System.out.println("eventId "+desiredString);
        logger.info("event Id for current bulkRecharge record: "+desiredString);
        record.setEventId(desiredString);
        Optional<Event> output=eventRepo.findById(record.getEventId());
        if(output.isPresent()) {
			if(output.get().getStatus().toString().equalsIgnoreCase("SUCCESS")) {
				success++;
			}
			else if(output.get().getStatus().toString().equalsIgnoreCase("FAILED")) {
				failed++;
			}
			record.setStatus(output.get().getStatus().toString());
			BulkRechargeFile bulkRechargeFile=bulkRechargeFileRepo.findByJobIdAndArchivedStatus(record.getJobId(),"N");
			bulkRechargeFile.setSucessCount(bulkRechargeRepo.findByJobIdAndStatus(record.getJobId(),"SUCCESS").size());
			bulkRechargeFile.setErrorCount(bulkRechargeRepo.findByJobIdAndStatus(record.getJobId(),"FAILED").size());
			bulkRechargeFileRepo.save(bulkRechargeFile);
			bulkRechargeRepo.save(record);
			
			bulkRechargeFileRepo.save(bulkRechargeFile);	
            bulkRechargeRepo.save(record);
        if (response.getStatusCode() == HttpStatus.OK) {
           
           // System.out.println("API Response: " + response.getBody());
        }
      }
}

		
		
	}
		  //  System.out.println("sucess count "+success);
			logger.info("sucess count "+success);
			//System.out.println("failed count "+failed);
			logger.info("failed count "+failed);
			data.setSucessCount(bulkRechargeRepo.findByJobIdAndStatus(data.getJobId(),"SUCCESS").size());
			data.setErrorCount(bulkRechargeRepo.findByJobIdAndStatus(data.getJobId(),"FAILED").size());
			data.setStatus("Completed");
			data.setModifiedAt(LocalDateTime.now());
			bulkRechargeFileRepo.save(data);
}	

	}	

    	     
           
    
	@Scheduled(fixedRate = 15000)
    public void processRechargeScheduledForQueue() throws JsonProcessingException {
		List<BulkRecharge> records=bulkRechargeRepo.findByStatus("IN_QUEUE");
		for(int i=0;i<records.size();i++) {
			BulkRecharge data= records.get(i);
			Optional<Event> output=eventRepo.findById(data.getEventId());
			if(output.isPresent()) {
				if(!output.get().getStatus().toString().equalsIgnoreCase("IN_QUEUE")) {
					data.setStatus(output.get().getStatus().toString());
					bulkRechargeRepo.save(data);
				}
			}
		}
	}
	

	public ResponseEntity<Map<String, Object>> downloadBulkRechargeFile(String jobId) {
	    try {
	    	List<BulkRecharge> data  = bulkRechargeRepo.findByJobIdAndArchivedStatus(jobId, "N");
	       // List<BulkRecharge> data  = bulkRechargeRepo.findByJobIdAndArchivedStatus(jobId, "N");

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Bulk_Recharge_Data");

	        Row headerRow = sheet.createRow(0);
	        String[] headers = {"Discom Name", "Agency Name", "Agency Van", "Agent Van", "Agent Login ID",
	                "Agent Unique Id", "Rechargeable Amount", "Status"};
            logger.info("sheet headers "+headers.toString());
	        for (int i = 0; i < headers.length; i++) {
	            Cell cell = headerRow.createCell(i);
	            cell.setCellValue(headers[i]);
	        }

	        int rowNum = 1;
	        for (BulkRecharge item : data) {
	            Row row = sheet.createRow(rowNum++);
	            row.createCell(0).setCellValue(item.getDiscomName());
	            row.createCell(1).setCellValue(item.getAgencyName());
	            row.createCell(2).setCellValue(item.getAgencyVan());
	            row.createCell(3).setCellValue(item.getAgentVan());
	            row.createCell(4).setCellValue(item.getAgentLoginId());
	            row.createCell(5).setCellValue(item.getAgentUniqueId());
	            row.createCell(6).setCellValue(item.getAmount());
	            row.createCell(7).setCellValue(item.getStatus());
	        }

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        workbook.write(outputStream);
	        workbook.close();

	        byte[] excelData = outputStream.toByteArray();

	        
	        String base64ExcelData = Base64.getEncoder().encodeToString(excelData);

	        Map<String, Object> jsonResponse = new HashMap<>();
	        jsonResponse.put("message", "File successfully generated.");
	        jsonResponse.put("downloadLink", "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64," + base64ExcelData);

	        HttpHeaders mainheaders = new HttpHeaders();
	        mainheaders.setContentType(MediaType.APPLICATION_JSON);

	        return ResponseEntity.ok()
	                .headers(mainheaders)
	                .body(jsonResponse);
	    } catch (Exception e) {
	        e.printStackTrace();
	        Map<String, Object> errorResponse = new HashMap<>();
	        errorResponse.put("error", "Internal Server Error");
	        errorResponse.put("message", "Error generating and sending the file: " + e.getMessage());

	        HttpHeaders mainheaders = new HttpHeaders();
	        mainheaders.setContentType(MediaType.APPLICATION_JSON);

	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .headers(mainheaders)
	                .body(errorResponse);
	    }
	}

	public String sendSimpleMail(String agencyName,String jobId,String fileName, String toEmailId) {
		 try {
	            SimpleMailMessage mailMessage= new SimpleMailMessage();
	            String message = String.format("Dear %s,%n%nYour request for bulk recharge against the file %s has been completed. Kindly check the eWallet portal.", agencyName, fileName);
	            String subject="Bulk Recharge Update - "+jobId;
	            StringTokenizer stk = new StringTokenizer(toEmailId,",");
	            while (stk.hasMoreTokens())
	            {	
	            String data = stk.nextToken();
	            mailMessage.setFrom("application_alert@gen-xt.com");
	            mailMessage.setTo(data);
	            mailMessage.setText(message);
	            mailMessage.setSubject(subject);
	            javaMailSender.send(mailMessage);
	            
	            }
	            return "Mail Sent Successfully...";
	        }catch (Exception e) {
	        	e.printStackTrace();
	            return "Error while Sending Mail "+e.getMessage() ;
	        }
	}
	public long updateStatusByJobId(String jobId, String newStatus) {
        Query query = new Query(Criteria.where("jobId").is(jobId).and("status").is("Not Started"));
        Update update = Update.update("status", newStatus);

        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, BulkRecharge.class);

        return updateResult.getModifiedCount();
    }
	
	public void updateStatusToInQueueForProcessingRecords() {
        Query query = new Query(Criteria.where("status").is("Processing"));
        Update update = new Update().set("status", "IN_QUEUE");
        mongoTemplate.updateMulti(query, update, BulkRechargeFile.class);
    }
	
	public Double findBalanceById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Wallet wallet = mongoTemplate.findOne(query, Wallet.class);

        if (wallet != null) {
            return wallet.getBalance();
        } else {
            return null; 
        }
    }
	
	public static boolean isRowBlank(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK) {
                return false; // If any cell is not blank, the row is not blank
            }
        }
        return true; // All cells in the row are blank
    }
}
