package com.tequre.wallet.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tequre.wallet.config.IntegrationConfig;
import com.tequre.wallet.data.ServiceMessage;
import com.tequre.wallet.data.Transaction;
import com.tequre.wallet.enums.Discom;
import com.tequre.wallet.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Service
public class UrbanMeterReaderService {

	private final Logger logger = LoggerFactory.getLogger(UrbanMeterReaderService.class);

    private String authorizationHeader = null;

    private static String SUCCESS = "Executed Successfully";

    @Autowired
    private IntegrationConfig integrationConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Gson gson;

    private enum RequestFlag {
        SYNC("SYNC"),
        VALIDATE("VALIDATE"),
        DEREGISTER("DE-REGISTER"),
        UPDATE("UPDATE");

        String value;

        RequestFlag(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public boolean sync(String uniqueId, String agencyId, String agencyVan, String agentId, String agentVan, String discom) {
        HttpHeaders headers = new HttpHeaders();
        discom = discom.toUpperCase();
        if(discom != null) {
        	headers.set(HttpHeaders.AUTHORIZATION, authorizationHeaderRMS());
            headers.setContentType(MediaType.APPLICATION_JSON);
        } else {
        	headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader());
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader());
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject syncPayload = new JsonObject();
        syncPayload.addProperty("requestFlag", RequestFlag.SYNC.getValue());
        syncPayload.addProperty("uniqueId", uniqueId);
        syncPayload.addProperty("agencyId", agencyId);
        syncPayload.addProperty("agencyVan", agencyVan);
        syncPayload.addProperty("agentId", agentId);
        syncPayload.addProperty("agentVan", agentVan);
        String userString = syncPayload.toString();
        HttpEntity<String> entity = new HttpEntity<>(userString, headers);
        /*try {
            ResponseEntity<String> response = restTemplate
                    .exchange(integrationConfig.getRapdrpMeterAgentUrl(),
                            HttpMethod.POST, entity, String.class);
            logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
            if (response.getStatusCode().is2xxSuccessful()) {
                return getStatus(response.getBody());
            }
        } catch (Throwable th) {
            logger.error("Exception occurred in sync operation for " + agentId + " for agency " + agencyId, th);
        }*/
        
        try {
        	/*
        	 * 
        	 * Added Switch condition by Hotam Singh
        	 */
        	switch (discom) {
        	case "PUVVNL":
            	ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getPuvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
            	if (response.getStatusCode().is2xxSuccessful()) {
                    return getStatus(response.getBody());
                }
        	case "PUVNL":
            	ResponseEntity<String> response0 = restTemplate
                .exchange(integrationConfig.getPuvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response0.getStatusCode() + " Body: " + response0.getBody());
            	if (response0.getStatusCode().is2xxSuccessful()) {
                    return getStatus(response0.getBody());
                }
            case "PVVNL":
            	ResponseEntity<String> response1 = restTemplate
                .exchange(integrationConfig.getPvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response1.getStatusCode() + " Body: " + response1.getBody());
            	if (response1.getStatusCode().is2xxSuccessful()) {
                    return getStatus(response1.getBody());
                }
            case "MVVNL":
            	ResponseEntity<String> response2 = restTemplate
                .exchange(integrationConfig.getMvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response2.getStatusCode() + " Body: " + response2.getBody());
            	if (response2.getStatusCode().is2xxSuccessful()) {
                    return getStatus(response2.getBody());
                }
            case "DVVNL":
            	ResponseEntity<String> response3 = restTemplate
                .exchange(integrationConfig.getDvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response3.getStatusCode() + " Body: " + response3.getBody());
            	if (response3.getStatusCode().is2xxSuccessful()) {
                    return getStatus(response3.getBody());
                }
            default:
            	ResponseEntity<String> response4 = restTemplate
                .exchange(integrationConfig.getRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response4.getStatusCode() + " Body: " + response4.getBody());
            	if (response4.getStatusCode().is2xxSuccessful()) {
                    return getStatus(response4.getBody());
                }
        }
        	
        	
            
        } catch (Throwable th) {
            logger.error("Exception occurred in sync operation for " + agentId + " for agency " + agencyId, th);
        }
        return false;
    }

    public ResponseEntity<String> validate(String uniqueId, String discomName) {
        HttpHeaders headers = new HttpHeaders();
        discomName = discomName.toUpperCase();
        if(discomName != null) {
        	headers.set(HttpHeaders.AUTHORIZATION, authorizationHeaderRMS());
            headers.setContentType(MediaType.APPLICATION_JSON);
        } else {
        	headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader());
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        // headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader());
        // headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject validatePayload = new JsonObject();
        validatePayload.addProperty("requestFlag", RequestFlag.VALIDATE.getValue());
        validatePayload.addProperty("uniqueId", uniqueId);
        String userString = validatePayload.toString();
        HttpEntity<String> entity = new HttpEntity<>(userString, headers);
        try {
        	/*
        	 * 
        	 * Added Switch condition by Hotam Singh
        	 */
        	switch (discomName) {
        	case "PUVVNL":
            	ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getPuvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
            	return response;
        	case "PUVNL":
            	ResponseEntity<String> response0 = restTemplate
                .exchange(integrationConfig.getPuvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response0.getStatusCode() + " Body: " + response0.getBody());
            	return response0;
            case "PVVNL":
            	ResponseEntity<String> response1 = restTemplate
                .exchange(integrationConfig.getPvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response1.getStatusCode() + " Body: " + response1.getBody());
            	return response1;
            case "MVVNL":
            	ResponseEntity<String> response2 = restTemplate
                .exchange(integrationConfig.getMvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response2.getStatusCode() + " Body: " + response2.getBody());
            	return response2;
            case "DVVNL":
            	ResponseEntity<String> response3 = restTemplate
                .exchange(integrationConfig.getDvvnlRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response3.getStatusCode() + " Body: " + response3.getBody());
            	return response3;
            default:
            	ResponseEntity<String> response4 = restTemplate
                .exchange(integrationConfig.getRapdrpMeterAgentUrl(),
                        HttpMethod.POST, entity, String.class);
            	logger.info("Status: " + response4.getStatusCode() + " Body: " + response4.getBody());
            	return response4;
        }
            
        } catch (Throwable th) {
            logger.error("Exception occurred in validate operation for " + uniqueId, th);
            return null;
        }
    }

    public boolean deregister(String uniqueId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader());
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject deregisterPayload = new JsonObject();
        deregisterPayload.addProperty("requestFlag", RequestFlag.DEREGISTER.getValue());
        deregisterPayload.addProperty("uniqueId", uniqueId);
        String deregisterString = deregisterPayload.toString();
        HttpEntity<String> entity = new HttpEntity<>(deregisterString, headers);
        try {
            ResponseEntity<String> response = restTemplate
                    .exchange(integrationConfig.getRapdrpMeterAgentUrl(),
                            HttpMethod.POST, entity, String.class);
            logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
            if (response.getStatusCode().is2xxSuccessful()) {
                return getStatus(response.getBody());
            }
        } catch (Throwable th) {
            logger.error("Exception occurred in de-register operation for uniqueId " + uniqueId, th);
        }
        return false;
    }

    private boolean getStatus(String body) {
        JsonObject syncResponse = gson.fromJson(body, JsonObject.class);
        String message = syncResponse.get("message").getAsString();
        String responseCode = syncResponse.get("responseCode").getAsString();
        int code = Integer.parseInt(responseCode);
        if (code == 0 && SUCCESS.equalsIgnoreCase(message)) {
            return true;
        }
        return false;
    }

    private String authorizationHeader() {
        if (Objects.isNull(authorizationHeader)) {
            authorizationHeader = WebUtils.authorization(integrationConfig.getRapdrpMeterAgentUsername(),
                    integrationConfig.getRapdrpMeterAgentPassword());
        }
        return authorizationHeader;
    }
    
    private String authorizationHeaderRMS() {
        if (Objects.isNull(authorizationHeader)) {
            authorizationHeader = WebUtils.authorization(integrationConfig.getRapdrpMeterAgentUsernameRMS(),
                    integrationConfig.getRapdrpMeterAgentPasswordRMS());
        }
        return authorizationHeader;
    }

	
}
