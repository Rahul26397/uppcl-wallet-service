package com.tequre.wallet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tequre.wallet.controller.ReportController;
import com.tequre.wallet.data.BulkRecharge;
import com.tequre.wallet.enums.AgencyType;
import com.tequre.wallet.enums.AgentType;
import com.tequre.wallet.enums.AreaType;
import com.tequre.wallet.enums.Discom;
import com.tequre.wallet.enums.TransactionType;
import com.tequre.wallet.response.ReportResponse;
import com.tequre.wallet.response.TransactionQueryResponse;



@Service
public class ReportService {
	
	private final Logger logger = LoggerFactory.getLogger(ReportService.class);
		
	@Autowired
    private MongoClient mongoClient;

	
	public ReportResponse divisionWiseReport(Long startDate, Long endDate, Discom discom, String division, AgencyType agencyType) {
	    MongoDatabase database = mongoClient.getDatabase("wallet");
	    MongoCollection<Document> transactionCollection = database.getCollection("transaction");

//	    Date fromDate = DateGenerator(startDate); // Define a function to parse dates
//	    Date toDate = DateGenerator(endDate);
//	    
//	    Date fromDate= new Date(Long.parseLong(startDate.replace("-", "")));
//	    Date toDate= new Date(Long.parseLong(endDate.replace("-", "")));
//	    System.out.println("fromDate "+Long.parseLong(startDate.replace("-", "")) +"toDate"+toDate.toString());
	    
        ReportResponse report=new ReportResponse();
        
	    List<Document> combinedDocuments = new ArrayList<>();
	    Document matchResult = new Document();

	    if (agencyType != null) {
	        matchResult.append("agency.agencyType", agencyType.name());
	    }
	    if (discom != null && ("PUVVNL".equals(discom.toString()) || "PUVNL".equals(discom.toString()))) {
	        // Combine results for "PUVVNL" and "PUVNL"
	        matchResult.append("discom", Pattern.compile("^(PUVVNL|PUVNL)"));
	    } else if (discom != null) {
	        // Use the provided discom value
	        matchResult.append("discom", Pattern.compile("^" + discom.toString()));
	    }
//	    if (discom != null) {
//	        matchResult.append("discom", Pattern.compile("^" + discom.toString()));
//	    }
	    if (division != null) {
	        matchResult.append("division", division);
	    }
	    
	    List<TransactionQueryResponse> combinedRecords = new ArrayList<>();

	    List<Document> aggregationPipeline = new ArrayList<>();
	    aggregationPipeline.add(new Document("$match", new Document
	            ("activity", TransactionType.DEBIT.name())
	            .append("transactionTime", new Document("$gte",new java.util.Date(startDate))
	                    .append("$lte", new java.util.Date(endDate)))
	            .append("transactionType", new Document("$in", Arrays.asList(AreaType.RAPDRP.name(), AreaType.NON_RAPDRP.name())))
	    ));
	    aggregationPipeline.add(new Document("$lookup", new Document("from", "agent")
	            .append("localField", "agencyId")
	            .append("foreignField", "_id")
	            .append("as", "agency")));
	    aggregationPipeline.add(new Document("$unwind", new Document("path", "$agency")));
	    
	    aggregationPipeline.add(  new Document("$match",  matchResult));
	    
	    aggregationPipeline.add(new Document("$group", new Document("_id",
	            new Document("agencyType", "$agency.agencyType")
	                    .append("discom", 
	                    	    new Document("$substr", Arrays.asList("$discom", 0L, 6L)))
	                    .append("division", "$division"))
	            .append("count", new Document("$sum", 1L))
	            .append("totalAmount", new Document("$sum", "$amount"))));

	    if (agencyType == null || agencyType.toString().equalsIgnoreCase(AgencyType.MR.name()) || agencyType.toString().equalsIgnoreCase(AgencyType.PACS.name())) {
	        AggregateIterable<Document> result = transactionCollection.aggregate(aggregationPipeline);
	        for (Document document : result) {
	        	TransactionQueryResponse record = convertDocumentToTransactionRecord(document);
	            combinedRecords.add(record);
	            combinedDocuments.add(document);
	        }
	    }

	    if (agencyType == null || (!agencyType.toString().equalsIgnoreCase(AgencyType.MR.name()) && !agencyType.toString().equalsIgnoreCase(AgencyType.PACS.name()))) {
	        // Clear the aggregationPipeline and reuse it
	        aggregationPipeline.clear();
	        aggregationPipeline.add(new Document("$match", new Document
	                ("activity", TransactionType.DEBIT.name())
	                .append("transactionTime", new Document("$gte", new java.util.Date(startDate))
	                        .append("$lte", new java.util.Date(endDate)))
	                .append("entityType", new Document("$in", Arrays.asList(AgentType.DEPARTMENT.name(),AgentType.AGENCY.name())))
	                .append("transactionType", new Document("$in", Arrays.asList(AreaType.RAPDRP.name(),AreaType.NON_RAPDRP.name())))
	        ));
	        aggregationPipeline.add(new Document("$lookup", new Document()
	                .append("from", "agent")
	                .append("localField", "entityId")
	                .append("foreignField", "_id")
	                .append("as", "agency")));
	        aggregationPipeline.add(new Document("$unwind", new Document("path", "$agency")));
	        
	        aggregationPipeline.add(  new Document("$match",  matchResult));
	        
	        aggregationPipeline.add(new Document("$group", new Document("_id",
	                new Document("agencyType", "$agency.agencyType")
	                .append("discom", 
                    	    new Document("$substr", Arrays.asList("$discom", 0L, 6L)))
	                        .append("division", "$division"))
	                .append("count", new Document("$sum", 1L))
	                .append("totalAmount", new Document("$sum", "$amount"))));

	        AggregateIterable<Document> result2 = transactionCollection.aggregate(aggregationPipeline);
	        logger.info("current result data "+result2.toString());
	        for (Document document : result2) {
	        	logger.info("current iteration data "+document.toString());
	        	TransactionQueryResponse record = convertDocumentToTransactionRecord(document);
	            combinedRecords.add(record);
	            combinedDocuments.add(document);
	        }
	    }
	    report.setRecordCount(combinedRecords.size());
	    report.setResult(combinedRecords);

	    return report;
	}


	 
	 public Date DateGenerator(String time) {    
	        // Create a SimpleDateFormat for parsing the input date string
	        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	        inputDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	         Date date=null;
			try {
				date = inputDateFormat.parse(time);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        // Get the milliseconds since epoch (Unix timestamp) and format it
	        long milliseconds = date.getTime();
	       return new Date(milliseconds);
	        
	       
	 }
	 
	 public ResponseEntity<Map<String, Object>> downloaddivisionWiseReport(Long startDate, Long endDate, Discom discom, String division, AgencyType agencyType) {
		    try {
		    		ReportResponse result  = divisionWiseReport(startDate,endDate,discom,division,agencyType) ;
		    	List<TransactionQueryResponse> data=result.getResult();

		        Workbook workbook = new XSSFWorkbook();
		        Sheet sheet = workbook.createSheet("Division_Wise_Report");

		        Row headerRow = sheet.createRow(0);
		        String[] headers = {"Agency Type", "Discom", "Division", "Total Posting Amount"};
	            logger.info("sheet headers "+headers.toString());
		        for (int i = 0; i < headers.length; i++) {
		            Cell cell = headerRow.createCell(i);
		            cell.setCellValue(headers[i]);
		        }

		        int rowNum = 1;
		        for (TransactionQueryResponse item : data) {
		            Row row = sheet.createRow(rowNum++);
		            row.createCell(0).setCellValue(item.getId().getAgencyType());
		            row.createCell(1).setCellValue(item.getId().getDiscom());
		            row.createCell(2).setCellValue(item.getId().getDivision());
		            row.createCell(3).setCellValue(item.getTotalAmount());
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
	 
	 private TransactionQueryResponse convertDocumentToTransactionRecord(Document document) {
		 TransactionQueryResponse record = new TransactionQueryResponse();
		    
		    Document idDoc = document.get("_id", Document.class);
		    TransactionQueryResponse.Id id = new TransactionQueryResponse.Id();
		    id.setAgencyType(idDoc.getString("agencyType"));
		    id.setDiscom(idDoc.getString("discom").replace("-", ""));
		    id.setDivision(idDoc.getString("division"));
		    
		    record.setId(id);
		    record.setCount(document.getLong("count"));
		    record.setTotalAmount(document.getDouble("totalAmount"));
		    
		    return record;
		}
	 
	

	

	
}
