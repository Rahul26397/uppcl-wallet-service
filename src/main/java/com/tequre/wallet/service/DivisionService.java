package com.tequre.wallet.service;

import static com.mongodb.client.model.Filters.eq;
import static com.tequre.wallet.utils.Constants.AUTHORIZATION_HEADER;

import com.tequre.wallet.config.IntegrationConfig;
import com.tequre.wallet.enums.SourceType;
import com.tequre.wallet.response.RuralUrbanMappingResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.tomcat.util.json.JSONParser;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Projections;

@Service
public class DivisionService {
	
	private final Logger logger = LoggerFactory.getLogger(DivisionService.class);
	
    @Autowired
    private MongoClient mongoClient;
    
    @Autowired
    private IntegrationConfig integrationConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private Gson gson;
    
    private String getBearerToken() {
        String key = integrationConfig.getConnectionCountConsumerKey() + ":" + integrationConfig.getConnectionCountConsumerSecret();
        String base64Encoded = Base64.getEncoder().encodeToString(key.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, "Basic " + base64Encoded);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(integrationConfig.getMappingTokenUrl())
                .queryParam("grant_type", "password")
                .queryParam("username", integrationConfig.getMappingUsername())
                .queryParam("password", integrationConfig.getMappingPassword());
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate
                .exchange(builder.toUriString(), HttpMethod.POST, entity, String.class);
        logger.info("Auth Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonAgent = gson.fromJson(response.getBody(), JsonObject.class);
            String accessToken = jsonAgent.get("access_token").getAsString();
            return accessToken;
        } else {
            throw new IllegalStateException("Unable to get connection details");
        }
    }
    
    public String getConnectionCount(String govtCode) {
    	if(govtCode.equals("")) {
    		logger.info("Govt Code is missing");
    		return "0";
    	}
        HttpHeaders headers = new HttpHeaders();
        String token = getBearerToken();
        headers.set(AUTHORIZATION_HEADER, "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(integrationConfig.getConnectionCountUrl())
                .queryParam("govCode", govtCode);
        ResponseEntity<String> response = restTemplate
                .exchange(builder.toUriString(), HttpMethod.GET, entity, String.class);
        logger.info("Mapping Status: " + response.getStatusCode() + " Body: " + response.getBody());
        
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonAgent = gson.fromJson(response.getBody(), JsonObject.class);
           logger.info("jsonAgent "+ jsonAgent);
           JsonElement jsonArray2 = jsonAgent.get("data");
           logger.info("jsonArray2 "+ jsonArray2);
           JsonArray array = jsonArray2.getAsJsonArray();
           String totalCount = "0";
           if(array.size() != 0) {
        	   for(JsonElement i:array) {
            	   JsonObject jsobj = i.getAsJsonObject();
            	   if(jsobj.get("organizationCode").getAsString().equals(govtCode)) {
            		   totalCount =  jsobj.get("totalCount").toString();
            	   }
               }  
           }
           return totalCount;
        } else {
            throw new IllegalStateException("Unable to get connection details");
        }
    }
    
    public AggregateIterable getDepartmentListing() {
        MongoDatabase database = mongoClient.getDatabase("wallet");
        MongoCollection<Document> agentCollection = database.getCollection("agent");
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = null;
        Document matchResult = new Document();
        Document matchResult2 = new Document();
        result = database
                .getCollection("agent").aggregate(Arrays.asList(new Document("$match", 
                	    new Document("agencyType", "DEPARTMENT")), 
                	    new Document("$lookup", 
                	    new Document("from", "wallet")
                	            .append("localField", "van")
                	            .append("foreignField", "_id")
                	            .append("as", "balance")), 
                	    new Document("$unwind", 
                	    new Document("path", "$balance")
                	            .append("preserveNullAndEmptyArrays", true)), 
                	    new Document("$lookup", 
                	    new Document("from", "user")
                	            .append("localField", "user.$id")
                	            .append("foreignField", "_id")
                	            .append("as", "userData")), 
                	    new Document("$unwind", 
                	    new Document("path", "$userData")
                	            .append("preserveNullAndEmptyArrays", true)), 
                	    new Document("$project", 
                	    new Document("departmentName", "$agencyName")
                	            .append("departmentCode", "$empId")
                	            .append("status", 1L)
                	            .append("agencyType", 1L)
                	            .append("balance", "$balance.balance")
                	            .append("van", 1L)), 
                	    new Document("$sort", 
                	    new Document("departmentName", 1L))));
        
        /*for (Document dbObject : result) {
            if(dbObject.get("departmentCode") != null) {
                dbObject.put("totalCount", getConnectionCount((String) dbObject.get("departmentCode")));
                // result.put("totalCount", getConnectionCount((String) dbObject.get("departmentCode")));
            }
            documents.add(dbObject);
        }
        return (AggregateIterable<?>) documents;*/
        return result;
    }

    public AggregateIterable getTowerListing() {
        MongoDatabase database = mongoClient.getDatabase("wallet");
        MongoCollection<Document> agentCollection = database.getCollection("agent");
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = null;
        Document matchResult = new Document();
        Document matchResult2 = new Document();
        result = database
                .getCollection("agent").aggregate(Arrays.asList(new Document("$match", 
                	    new Document("agencyType", "TOWER")), 
                	    new Document("$lookup", 
                	    new Document("from", "wallet")
                	            .append("localField", "van")
                	            .append("foreignField", "_id")
                	            .append("as", "balance")), 
                	    new Document("$unwind", 
                	    new Document("path", "$balance")
                	            .append("preserveNullAndEmptyArrays", true)), 
                	    new Document("$lookup", 
                	    new Document("from", "user")
                	            .append("localField", "user.$id")
                	            .append("foreignField", "_id")
                	            .append("as", "userData")), 
                	    new Document("$unwind", 
                	    new Document("path", "$userData")
                	            .append("preserveNullAndEmptyArrays", true)), 
                	    new Document("$project", 
                	    new Document("departmentName", "$agencyName")
                	            .append("departmentCode", "$empId")
                	            .append("status", 1L)
                	            .append("agencyType", 1L)
                	            .append("balance", "$balance.balance")
                	            .append("van", 1L)), 
                	    new Document("$sort", 
                	    new Document("departmentName", 1L))));
        return result;
    }
    
    public AggregateIterable findData(Document filter) {
        MongoDatabase database = mongoClient.getDatabase("wallet");
        MongoCollection<Document> agentCollection = database.getCollection("agent");
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = null;
        Document matchResult = new Document();
        matchResult.append("vanId", new Document("$ne", "UPPCL"));
        // matchResult.append("entityType", "AGENT");
        if(filter.get("discom") != null) {
            matchResult.append("discom", Pattern.compile("^"+filter.get("discom")));
        }
        if(filter.get("division") != null){
            matchResult.append("division", filter.get("division"));
        }
        if(filter.get("van") != null){
            //eq("van", "UPCA8554408791")
            Bson projectionFields = Projections.fields(
                    Projections.include("_id"));
            // Projections.excludeId());
            Document agencyId = agentCollection.find(eq("van", filter.get("van")))
                    .projection(projectionFields)
                    // .sort(Sorts.descending("imdb.rating"))
                    .first();
            matchResult.append("agencyId", agencyId.get("_id"));
        }
        if(filter.get("transactionType") != null){
            matchResult.append("transactionType", filter.get("transactionType"));
        } else {
            matchResult.append("$or", Arrays.asList(new Document("transactionType", "RAPDRP"),
                    new Document("transactionType", "NON_RAPDRP")));
        }
        if(filter.get("consumerId") != null){
            matchResult.append("consumerId", filter.get("consumerId"));
        }
        // if(discom != null && division != null && van!=null && transactionType !=null && consumerId !=null) {
        result = database
                .getCollection("transaction").aggregate(Arrays.asList(new Document("$match",
                                matchResult
                                        .append("$and", Arrays.asList(new Document("transactionTime",
                                                        new Document("$gte",
                                                                new java.util.Date((Long) filter.get("startTime")))),
                                                new Document("transactionTime",
                                                        new Document("$lte",
                                                                new java.util.Date((Long) filter.get("endTime"))))))),
                        new Document("$sort",
                                new Document("transactionTime", -1L)),
                        new Document("$lookup",
                                new Document("from", "uppcl_hierarchy")
                                        .append("pipeline", Arrays.asList(new Document("$match",
                                                new Document("$or", Arrays.asList(new Document("rapdrpDivisionCode", "$divisioncode"),
                                                        new Document("nonRapdrpDivisionCode", "$divisioncode"))))))
                                        .append("as", "hierarchy")),
                        new Document("$unwind",
                                new Document("path", "$hierarchy")
                                        .append("preserveNullAndEmptyArrays", true)),
                        new Document("$lookup",
                                new Document("from", "agent")
                                        .append("localField", "entityId")
                                        .append("foreignField", "_id")
                                        .append("as", "agent_details")),
                        new Document("$unwind",
                                new Document("path", "$agent_details")
                                        .append("preserveNullAndEmptyArrays", true)),
                        new Document("$group",
                                new Document("_id", "$division")
                                        .append("discom",
                                                new Document("$first", "$discom"))
                                        .append("zone",
                                                new Document("$first", "$hierarchy.zoneName"))
                                        .append("circle",
                                                new Document("$first", "$hierarchy.circleName"))
                                        .append("district",
                                                new Document("$first", "$hierarchy.districtName"))
                                        .append("totalActiveAgents",
                                                new Document("$sum",
                                                        new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$eq", Arrays.asList("$agent_details.status", "ACTIVE")),
                                                                new Document("$eq", Arrays.asList("$agent_details.agentType", "AGENT")))), 1L, 0L))))
                                        .append("totalTillDateActiveAgents",
                                                new Document("$sum",
                                                        new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$eq", Arrays.asList("$agent_details.status", "ACTIVE")),
                                                                new Document("$eq", Arrays.asList("$agent_details.agentType", "AGENT")),
                                                                new Document("$gte", Arrays.asList("$transactionTime",
                                                                        new java.util.Date((Long) filter.get("tillDateStartTime")))),
                                                                new Document("$lte", Arrays.asList("$transactionTime",
                                                                        new java.util.Date((Long) filter.get("tillDateEndTime")))))), 1L, 0L))))
                                        .append("totalExternalAgents",
                                                new Document("$sum",
                                                        new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$eq", Arrays.asList("$agent_details.status", "ACTIVE")),
                                                                new Document("$eq", Arrays.asList("$agent_details.agentType", "AGENCY")))), 1L, 0L))))
                                        .append("totalBillCount",
                                                new Document("$sum", 1L))
                                        .append("totalTillDateBillCount",
                                                new Document("$sum",
                                                        new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$gte", Arrays.asList("$transactionTime",
                                                                        new java.util.Date((Long) filter.get("tillDateStartTime")))),
                                                                new Document("$lte", Arrays.asList("$transactionTime",
                                                                        new java.util.Date((Long) filter.get("tillDateEndTime")))))), 1L, 0L))))
                                        .append("totalBillAmount",
                                                new Document("$sum",
                                                        new Document("$sum", "$amount")))
                                        .append("totalTillDateBillAmount",
                                                new Document("$sum",
                                                        new Document("$sum",
                                                                new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$gte", Arrays.asList("$transactionTime",
                                                                                new java.util.Date((Long) filter.get("tillDateStartTime")))),
                                                                        new Document("$lte", Arrays.asList("$transactionTime",
                                                                                new java.util.Date((Long) filter.get("tillDateEndTime")))))), "$transactions.amount", 0L))))))));
        return result;
    }

    public AggregateIterable findData2(Document filter) {
        MongoDatabase database = mongoClient.getDatabase("wallet");
        MongoCollection<Document> agentCollection = database.getCollection("agent");
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = null;
        Document matchResult = new Document();
        Document matchResult2 = new Document();
        if(filter.get("discom") != null) {
            matchResult.append("discom", Pattern.compile("^"+filter.get("discom")));
        }
        if(filter.get("division") != null){
            matchResult.append("division", filter.get("division"));
        }
        if(filter.get("van") != null){
            //eq("van", "UPCA8554408791")
            Bson projectionFields = Projections.fields(
                    Projections.include("_id"));
            // Projections.excludeId());
            Document agencyId = agentCollection.find(eq("van", filter.get("van")))
                    .projection(projectionFields)
                    // .sort(Sorts.descending("imdb.rating"))
                    .first();
            matchResult.append("agencyId", agencyId.get("_id"));
        }
        if(filter.get("transactionType") != null){
            if(filter.get("transactionType")=="RAPDRP") {
                matchResult2.append("rapdrpDivisionCode",
                        new Document("$ne", ""));
            }
            if(filter.get("transactionType")=="NON_RAPDRP") {
                matchResult2.append("nonRapdrpDivisionCode",
                        new Document("$ne", ""));
            }
            matchResult.append("transactionType", filter.get("transactionType"));
        } else {
            matchResult.append("$or", Arrays.asList(new Document("transactionType", "RAPDRP"),
                    new Document("transactionType", "NON_RAPDRP")));
            matchResult2.append("$or", Arrays.asList(new Document("rapdrpDivisionCode",
                            new Document("$ne", "")),
                    new Document("nonRapdrpDivisionCode",
                            new Document("$ne", ""))));
        }
        if(filter.get("consumerId") != null){
            matchResult.append("consumerId", filter.get("consumerId"));
        }
        // if(discom != null && division != null && van!=null && transactionType !=null && consumerId !=null) {
        result = database
                .getCollection("division_report_31-12-2021").aggregate(Arrays.asList(new Document("$match",
                                matchResult.append("$and", Arrays.asList(new Document("agent_details.status", "ACTIVE"),
                                        new Document("transactionTime",
                                                new Document("$gte",
                                                        new java.util.Date((Long) filter.get("startTime")))),
                                        new Document("transactionTime",
                                                new Document("$lte",
                                                        new java.util.Date((Long) filter.get("endTime"))))))),
                        new Document("$group",
                                new Document("_id",
                                        new Document("divisionCode", "$divisionCode")
                                                .append("discomName", "$hierarchy_details.discomName"))
                                        .append("discom",
                                                new Document("$first", "$hierarchy_details.discomName"))
                                        .append("zone",
                                                new Document("$first", "$hierarchy_details.zoneName"))
                                        .append("circle",
                                                new Document("$first", "$hierarchy_details.circleName"))
                                        .append("district",
                                                new Document("$first", "$hierarchy_details.districtName"))
                                        .append("division",
                                                new Document("$first", "$hierarchy_details.divisionName"))
                                        .append("totalActiveAgents",
                                                new Document("$addToSet", "$entityId"))
                                        .append("totalTillDateActiveAgents",
                                                new Document("$addToSet",
                                                        new Document("$and", Arrays.asList(new Document("$gte", Arrays.asList("$transactionTime",
                                                                        new java.util.Date((Long) filter.get("tillDateStartTime")))),
                                                                new Document("$lte", Arrays.asList("$transactionTime",
                                                                        new java.util.Date((Long) filter.get("tillDateEndTime"))))))))
                                        .append("totalBillCount",
                                                new Document("$sum", 1L))
                                        .append("totalTillDateBillCount",
                                                new Document("$sum",
                                                        new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$gte", Arrays.asList("$transactionTime",
                                                                        new java.util.Date((Long) filter.get("tillDateStartTime")))),
                                                                new Document("$lte", Arrays.asList("$transactionTime",
                                                                        new java.util.Date((Long) filter.get("tillDateEndTime")))))), 1L, 0L))))
                                        .append("totalBillAmount",
                                                new Document("$sum",
                                                        new Document("$sum", "$amount")))
                                        .append("totalTillDateBillAmount",
                                                new Document("$sum",
                                                        new Document("$sum",
                                                                new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$gte", Arrays.asList("$transactionTime",
                                                                                new java.util.Date((Long) filter.get("tillDateStartTime")))),
                                                                        new Document("$lte", Arrays.asList("$transactionTime",
                                                                                new java.util.Date((Long) filter.get("tillDateEndTime")))))), "$amount", 0L)))))),
                        new Document("$project",
                                new Document("discom", 1L)
                                        .append("zone", 1L)
                                        .append("circle", 1L)
                                        .append("district", 1L)
                                        .append("division", 1L)
                                        .append("totalActiveAgents",
                                                new Document("$size", "$totalActiveAgents"))
                                        .append("totalTillDateActiveAgents",
                                                new Document("$size",
                                                        new Document("$filter",
                                                                new Document("input", "$totalTillDateActiveAgents")
                                                                        .append("as", "totalTillDateActiveAgents")
                                                                        .append("cond",
                                                                                new Document("$eq", Arrays.asList("$$totalTillDateActiveAgents", true))))))
                                        .append("totalBillCount", 1L)
                                        .append("totalTillDateBillCount", 1L)
                                        .append("totalBillAmount", 1L)
                                        .append("totalTillDateBillAmount", 1L))));
        return result;
    }

    public AggregateIterable getCommissionWiseReport(Document filter) {
        MongoDatabase database = mongoClient.getDatabase("wallet");
        MongoCollection<Document> agentCollection = database.getCollection("agent");
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = null;
        Document matchResult = new Document();
        // String agencyName = null;
        if(filter.get("discom") != null) {
            matchResult.append("transaction.discom", Pattern.compile("^"+filter.get("discom")));
        }
        if(filter.get("division") != null){
            matchResult.append("transaction.division", filter.get("division"));
        }
        if(filter.get("van") != null){
            Bson projectionFields = Projections.fields(
                    Projections.include("_id", "agencyName", "van"));
            Document agencyId = agentCollection.find(eq("van", filter.get("van")))
                    .projection(projectionFields)
                    .first();
            // matchResult.append("vanId", filter.get("van"));
           matchResult.append("agencyId", agencyId.get("_id"));
            // agencyName = (String) agencyId.get("agencyName");
        }
        if(filter.get("consumerId") != null){
            matchResult.append("transaction.consumerId", filter.get("consumerId"));
        }
        
        result = database
                .getCollection("agency_wise_commission_report").aggregate(Arrays.asList(new Document("$match",
                        matchResult.append("$and", Arrays.asList(new Document("transaction.transactionTime",
                                        new Document("$gte",
                                                new java.util.Date((Long) filter.get("startTime")))),
                                new Document("transaction.transactionTime",
                                        new Document("$lte",
                                                new java.util.Date((Long) filter.get("endTime"))))))), 
                		new Document("$group", 
                			    new Document("_id", "$agencyId")
                			            .append("agencyId", 
                			    new Document("$first", "$agencyId"))
                			            .append("agencyName", 
                			    new Document("$first", "$agencyName"))
                			            .append("count", 
                			    new Document("$sum", 1L))
                			            .append("rechargeAmount", 
                			    new Document("$sum", 
                			    new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$eq", Arrays.asList("$transaction.transactionType", "BANK")), 
                			                                new Document("$eq", Arrays.asList("$transaction.activity", "CREDIT")))), "$transaction.amount", 0L))))
                			            .append("commissionAmount", 
                			    new Document("$sum", 
                			    new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$eq", Arrays.asList("$transaction.transactionType", "COMMISSION")), 
                			                                new Document("$eq", Arrays.asList("$transaction.activity", "CREDIT")))), "$transaction.amount", 0L))))
                			            .append("billColection", 
                			    new Document("$sum", 
                			    new Document("$cond", Arrays.asList(new Document("$or", Arrays.asList(new Document("$eq", Arrays.asList("$transaction.transactionType", "RAPDRP")), 
                			                                new Document("$eq", Arrays.asList("$transaction.transactionType", "NON_RAPDRP")))), "$transaction.amount", 0L)))))));
        return result;
    }

}
