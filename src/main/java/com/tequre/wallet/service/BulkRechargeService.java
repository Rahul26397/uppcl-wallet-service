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

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.tequre.wallet.event.Event;
import com.tequre.wallet.data.BulkRecharge;
import com.tequre.wallet.response.TransactionWalletResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
@Service
public class BulkRechargeService {
	

    private boolean scheduledTaskTriggered = false;
	
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
	
	private static Integer job_Id=null;
	private static String email_Id=null;
	private static final String TOKEN_URL = "https://ewallet-test.uppclonline.com:8280/token";
    private static final String API_URL = "https://ewallet-test.uppclonline.com:8280/v1transactionwallet/1.0.0";
    private final String basicAuthorizationToken = "cEQzTTlRU3RnWnRpNnFfZ3hBbEx3NnRaWWFJYTpLWmgyc0M5WFA3TTU0VHNrekJ1T3VMNklUUjhh";
	
	public String storeRecords(MultipartFile file,BulkRechargeRequest request) throws IOException{
		//Integer jobId= bulkRechargeFileRepo.findMaxValue();
		String extension=null;
		String originalFilename = file.getOriginalFilename();
	    if (originalFilename != null) {
	        int lastIndex = originalFilename.lastIndexOf('.');
	        if (lastIndex != -1) {
	            extension= originalFilename.substring(lastIndex + 1);
	        }
	        else {
	        	return "No extension in file";
	        }
	    }
	        else {
	        	return "No file Name present";
	        }
	    
	    System.out.println("file format "+extension);   
	    if(extension.equalsIgnoreCase("xlsx")) {
		Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = dateFormat.format(currentDate);
		BulkRechargeFile record =bulkRechargeFileRepo.findTopByOrderByJobIdDesc();
		
		Integer jobId=null;
		if(record==null) {
			jobId = Integer.parseInt(formattedDate.replaceAll("-", "").concat("1"));
		}
		else {
			Integer totalRecord=((int)bulkRechargeFileRepo.count())+1;
			String nextDigit=totalRecord.toString();
			jobId = Integer.parseInt(formattedDate.replaceAll("-", "").concat(nextDigit));
			//jobId=record.getJobId()+1;
		}
		logger.info("current maximum JobId "+jobId);
//		if(jobId==null) {
//			jobId=1;
//		}
//		else {
//			jobId++;
//		}
		int recordCount = 0;
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
            // Create a workbook from the input stream
            Workbook workbook = new XSSFWorkbook(inputStream);

            // Get the first sheet from the workbook
            Sheet sheet = workbook.getSheetAt(0); // Change the index if needed
            Iterator<Row> rowIterator = sheet.iterator();
            
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }
            while (rowIterator.hasNext()) {
            	BulkRecharge bulkRecharge=new BulkRecharge();
            	//Integer id=bulkRechargeRepo.findMaxId();
//            	Integer id=null;
//            	BulkRecharge recharge=bulkRechargeRepo.findTopByOrderByIdDesc();
//            	if(recharge==null) {
//            		id=1;
//            	}
//            	else {
//            		id=recharge.getId()+1;
//            	}
//            	logger.info("current maximum value of id "+id);
//            	if(id==null) {
//            		id=1;
//            	}
//            	else {
//            		id++;
//            	}
            	
                Row row = rowIterator.next();
                recordCount++;
                int columnIndex=0;
                // Iterate through each cell in the row
                for (Cell cell : row) {
                	columnIndex++;
                	logger.info("current columnIndex "+columnIndex);
                    // Get the cell value based on the cell type
                    switch (cell.getCellType()) {
                        case STRING:
                            System.out.print(cell.getStringCellValue() + "\t");
                            logger.info("cell content "+cell.getStringCellValue());
                            if(columnIndex==1) {
                            	bulkRecharge.setDiscomName(cell.getStringCellValue());
                            }
                            if(columnIndex==2) {
                            	bulkRecharge.setAgencyName(cell.getStringCellValue());
                            }
                            if(columnIndex==3) {
                            	bulkRecharge.setAgencyVan(cell.getStringCellValue());
                            	 ObjectMapper objectMapper = new ObjectMapper();
                            	 String agencyId=agentRepo.findAgentIdByVan(cell.getStringCellValue());
                            	 if(agencyId!=null) {
                                 JsonNode jsonNode = objectMapper.readTree(agencyId);
                                 String idValue = jsonNode.get("_id").asText();
                                 logger.info("agencyId based on van "+idValue);
                                 System.out.println("current agencyId "+idValue);
                                 bulkRecharge.setAgencyId(idValue);
                            	 }
                            	
                            	
                            	//bulkRecharge.setAgencyId(cell.getStringCellValue());
                            }
                            if(columnIndex==4) {
                            	bulkRecharge.setAgentVan(cell.getStringCellValue());
                            	
                            	String agentId=agentRepo.findAgentIdByVan(cell.getStringCellValue());
                            	if(agentId!=null) {
                            	 ObjectMapper objectMapper = new ObjectMapper();
                                 JsonNode jsonNode = objectMapper.readTree(agentId);
                                 String idValue = jsonNode.get("_id").asText();
                                 System.out.println("currently we want "+idValue);
                                 logger.info("agentId based on van "+idValue);
                                 System.out.println("current agentId "+idValue);
                            	bulkRecharge.setAgentId(idValue);
                            	}
                            }
                            if(columnIndex==5) {
                            	bulkRecharge.setAgentLoginId(cell.getStringCellValue());
                            	
                            }
                            if(columnIndex==6) {
                            	bulkRecharge.setAgentUniqueId(cell.getStringCellValue());
                            }
                            if(columnIndex==7) {
                            	int data = (int) Math.round(cell.getNumericCellValue());
                            	Integer intValue=data;
                            	bulkRecharge.setAmount(intValue.toString());
                            }
                            break;
                        case NUMERIC:
                        	logger.info("cell content "+cell.getNumericCellValue());
                            System.out.print(cell.getNumericCellValue() + "\t");
                            if(columnIndex==7) {
                            	int data = (int) Math.round(cell.getNumericCellValue());
                            	Integer intValue=data;
                            	bulkRecharge.setAmount(intValue.toString());
                            }
                            break;
                        case BOOLEAN:
                            System.out.print(cell.getBooleanCellValue() + "\t");
                            break;
                        case BLANK:
                            System.out.print("[BLANK]\t");
                            break;
                        default:
                            System.out.print("[UNKNOWN]\t");
                            break;
                    }
                }
                System.out.println("currently to agencyId "+request.getAgencyId()); 
                bulkRecharge.setStatus("Not Started");
                bulkRecharge.setArchivedStatus("N");
                bulkRecharge.setCreatedBy(request.getAgencyName());
                bulkRecharge.setModifiedBy(request.getAgencyName());
                bulkRecharge.setJobId(jobId);
              //  bulkRecharge.setId(id);
                bulkRecharge.setCreatedAt(LocalDateTime.now());
                bulkRecharge.setModifiedAt(LocalDateTime.now());
                
                bulkRechargeRepo.save(bulkRecharge);
                
            }
            logger.info("total records in excel "+recordCount);
            bulkRechargeFile.setTotalCount(recordCount);
            bulkRechargeFile.setCreatedAt(LocalDateTime.now());
            bulkRechargeFile.setModifiedAt(LocalDateTime.now());
            bulkRechargeFileRepo.save(bulkRechargeFile);
            

            // Close the workbook
            workbook.close();
        }    
		return "Records inserted Successfully";
	    }
	    else {
	    	return "unsupported file type";
	    }
	}
	
	public List<BulkRechargeFile> getBulkRechargeFileRecord(String van){
		Sort sort = Sort.by(Sort.Order.desc("_id"));
		return bulkRechargeFileRepo.findByAgencyVanAndArchivedStatus(van,"N",sort);
	}
	
	public List<BulkRecharge> getBulkRechargeRecord(Integer jobId, String flag){
	//	Integer jobId= bulkRechargeFileRepo.findJobIdByFileName(fileName);
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
	
	public String deleteBulkRecords(Integer jobId) {
		
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
	
	public String processRecharge(Integer jobId,String emailId) throws JsonProcessingException {
		job_Id=jobId;
		email_Id=emailId;
        
        
		Optional<BulkRechargeFile> bulkRechargeFile=bulkRechargeFileRepo.findById(jobId);
		
		BulkRechargeFile data=null;
		if(bulkRechargeFile.isPresent()) {
			data=bulkRechargeFile.get();
			if(data.getStatus().equalsIgnoreCase("Not Started")) {
			data.setStatus("Processing");
			bulkRechargeFileRepo.save(data);
			//bulkRechargeRepo.updateStatusByJobId(jobId, "Processing");
			updateStatusByJobId(jobId,"Processing");
			scheduledTaskTriggered = true;           
		//return "Your process has been completed with jobId "+jobId+ " ." ;
            return" your process is in progress. please wait for sometime";
	
	}else {
				return " you are not allowed to request again";
			}
			
		} else {
		return "no record available";
	}
    }
	
	@Scheduled(fixedRate = 10000)
    public void processRechargeScheduled() throws JsonProcessingException {
    	 try {
    		 if (scheduledTaskTriggered) {
    	            scheduledTaskTriggered = false;
    		 Integer jobId = job_Id;
    	     String emailId = email_Id;
    	     System.out.println("jobId "+jobId +" and emailId "+emailId);
    	    
             Thread.sleep(30000); // Sleep for 30 seconds
             Optional<BulkRechargeFile> bulkRechargeFile=bulkRechargeFileRepo.findById(jobId);
             int success=0;
		     int failed=0;
		     int flag=1;
		     BulkRechargeFile data=null;
		     if(bulkRechargeFile.isPresent()) {
			    data=bulkRechargeFile.get();
             	List<BulkRecharge> records=bulkRechargeRepo.findByJobIdAndArchivedStatus(jobId, "N");
	//	bulkRechargeRepo.updateStatusByJobId(jobId, "Processing");
		for(int i=0;i<records.size();i++) {
			flag=1;
			BulkRecharge record=records.get(i);
			System.out.println("record "+record);
		RestTemplate restTemplate = new RestTemplate();
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(TOKEN_URL)
                .queryParam("grant_type", "password")
                .queryParam("username", "uppclgenx")
                .queryParam("password", "APimTDu!2019@");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + basicAuthorizationToken);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<TokenResponse> responseEntity = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                TokenResponse.class
        );

        // Check if the request was successful (HTTP status code 200)
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
             System.out.println("right now "+responseEntity.getBody()); 
         
        } 
        
        HttpHeaders mainheaders = new HttpHeaders();
//        mainheaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        mainheaders.setContentType(MediaType.APPLICATION_JSON);
        mainheaders.set("Authorization", "Bearer " + responseEntity.getBody().getAccessToken());
        Integer amount=Integer.parseInt(record.getAmount());
        TransactionWalletRequest request= new TransactionWalletRequest();
        request.setAmount(amount);
        request.setDestinationAgentId(record.getAgentId());
        request.setSourceAgentId(record.getAgencyId());
        request.setSourceType("WALLET");
        
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(request);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, mainheaders);


//        MultiValueMap<String, Object> map= new LinkedMultiValueMap<>();
//        
//        map.add("amount", amount);
//        map.add("destinationAgentId", record.getAgentId());
//        map.add("sourceAgentId", record.getAgencyId());
//        map.add("sourceType", "WALLET");
//        
//        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, mainheaders);
        ResponseEntity<TransactionWalletResponse> response=null;
        try {
           response = restTemplate.exchange(API_URL, HttpMethod.POST, requestEntity, TransactionWalletResponse.class);
      }catch(Exception e) {
    	  flag=0;
    	  record.setStatus("FAILED");
    	  failed++;
    	  bulkRechargeRepo.save(record);
    	  
      }
      if(flag==1) {
        System.out.println("API Response: " + response.getBody());
        String[] parts = response.getBody().getLocation().split("/");
        String desiredString = parts[parts.length - 1];
        System.out.println("eventId "+desiredString);
        record.setEventId(desiredString);
        
        bulkRechargeRepo.save(record);
        if (response.getStatusCode() == HttpStatus.OK) {
           
            System.out.println("API Response: " + response.getBody());
        }
      }
}
		for(int i=0;i<records.size();i++) {
			
			BulkRecharge record=records.get(i);
			if(record.getEventId()!=null) {
			Optional<Event> output=eventRepo.findById(record.getEventId());
			
			if(output.isPresent()) {
				if(output.get().getStatus().toString().equalsIgnoreCase("SUCCESS")) {
					success++;
				}
				else if(output.get().getStatus().toString().equalsIgnoreCase("FAILED")) {
					failed++;
				}
			
			    System.out.println("final the event status"+output.get().getStatus().toString());
				record.setStatus(output.get().getStatus().toString());
				bulkRechargeRepo.save(record);
			}
		}
	}
		
		System.out.println("sucess count "+success);
		System.out.println("failed count "+failed);
		data.setSucessCount(success);
		data.setErrorCount(failed);
		data.setStatus("Completed");
		data.setModifiedAt(LocalDateTime.now());
		bulkRechargeFileRepo.save(data);
		
		
		if(emailId!=null) {
			sendSimpleMail(data.getAgencyName(),jobId,data.getFileName(),emailId);
		}
}	
       // return "Your request  with request id " +jobId +" has been received. We will notify once the process is completed";
	//realone-->	return "Your request with request id "+jobId+" has been received. Please check again after sometime";
		
			
          
    	 }     
           
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
         }
    }


	
    
    
//	public ResponseEntity<ByteArrayResource> downloadBulkRechargeFile(Integer jobId) throws IOException {
//        // Set the response headers
//        
//        
//        List<BulkRecharge> data= bulkRechargeRepo.findByJobIdAndArchivedStatus(jobId,"N");
//        // Create a new Excel workbook and sheet
//        Workbook workbook = new XSSFWorkbook();
//        Sheet sheet = workbook.createSheet("Bulk_Recharge_Data");
//
//        // Create the header row
//        Row headerRow = sheet.createRow(0);
//        String[] headers = {"Discom Name", "Agency Name", "Agency Van", "Agent Van", "Agent Login ID",
//                "Agent Unique Id", "Rechargeable Amount", "Status"};
//
//        for (int i = 0; i < headers.length; i++) {
//            Cell cell = headerRow.createCell(i);
//            cell.setCellValue(headers[i]);
//        }
//
//       
//
//        int rowNum = 1;
//        for (BulkRecharge item : data) {
//            Row row = sheet.createRow(rowNum++);
//            row.createCell(0).setCellValue(item.getDiscomName());
//            row.createCell(1).setCellValue(item.getAgencyName());
//            row.createCell(2).setCellValue(item.getAgencyVan());
//            row.createCell(3).setCellValue(item.getAgentVan());
//            row.createCell(4).setCellValue(item.getAgentLoginId());
//            row.createCell(5).setCellValue(item.getAgentUniqueId());
//            row.createCell(6).setCellValue(item.getAmount());
//            row.createCell(7).setCellValue(item.getStatus());
//           
//        }
//        
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        workbook.write(outputStream);
//        workbook.close();
//
//        byte[] result=outputStream.toByteArray();
//        
//        ByteArrayResource resource = new ByteArrayResource(result);
//
//        // Set response headers
//        HttpHeaders mainheaders = new HttpHeaders();
//        mainheaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Records.xlsx");
//        mainheaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
//        mainheaders.setContentLength(result.length);
//
//        // Return ResponseEntity with the Excel data
//        return ResponseEntity.ok()
//                .headers(mainheaders)
//                .body(resource);
//    }
	
	public ResponseEntity<Map<String, Object>> downloadBulkRechargeFile(Integer jobId) {
	    try {
	        List<BulkRecharge> data = bulkRechargeRepo.findByJobIdAndArchivedStatus(jobId, "N");

	        // Create a new Excel workbook and sheet
	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Bulk_Recharge_Data");

	        // Create the header row
	        Row headerRow = sheet.createRow(0);
	        String[] headers = {"Discom Name", "Agency Name", "Agency Van", "Agent Van", "Agent Login ID",
	                "Agent Unique Id", "Rechargeable Amount", "Status"};

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

	        // Encode the Excel data as a Base64 string
	        String base64ExcelData = Base64.getEncoder().encodeToString(excelData);

	        // Prepare JSON response with a download link
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
	        // Handle exceptions properly and return an error JSON response
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




	
	public String sendSimpleMail(String agencyName,Integer jobId,String fileName, String toEmailId) {
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
	public long updateStatusByJobId(Integer jobId, String newStatus) {
        Query query = new Query(Criteria.where("jobId").is(jobId));
        Update update = Update.update("status", newStatus);

        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, BulkRecharge.class);

        return updateResult.getModifiedCount();
    }
}
