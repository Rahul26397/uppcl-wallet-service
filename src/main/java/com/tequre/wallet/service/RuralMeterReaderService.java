package com.tequre.wallet.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tequre.wallet.config.IntegrationConfig;
import com.tequre.wallet.enums.Discom;

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

@Service
public class RuralMeterReaderService {

    private final Logger logger = LoggerFactory.getLogger(RuralMeterReaderService.class);

    @Autowired
    private IntegrationConfig integrationConfig;

    @Autowired
    private RestTemplate restTemplate;

    private static String SUCCESS = "Success";

    private static String FAILURE = "Fail";

    @Autowired
    private Gson gson;

    public boolean sync(String uniqueId, String discom, String agentVan, String agencyId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(meterAgentUrl(discom))
                .queryParam("userid", uniqueId)
                .queryParam("discom", discom.toUpperCase())
                .queryParam("rdrVan", agentVan)
                .queryParam("rdrAgency", agencyId)
                .queryParam("payStatus", 1);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate
                    .exchange(builder.toUriString(),
                            HttpMethod.GET, entity, String.class);
            logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
            if (response.getStatusCode().is2xxSuccessful()) {
                return getStatus(response.getBody());
            }
        } catch (Throwable th) {
            logger.error("Exception occurred in sync operation for " + agentVan + " for agency " + agencyId, th);
        }
        return false;
    }

    public boolean deregister(String uniqueId, String discom, String agentVan, String agencyId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(meterAgentUrl(discom))
                .queryParam("userid", uniqueId)
                .queryParam("discom", discom.toUpperCase())
                .queryParam("rdrVan", agentVan)
                .queryParam("rdrAgency", agencyId)
                .queryParam("payStatus", 0);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate
                    .exchange(builder.toUriString(),
                            HttpMethod.GET, entity, String.class);
            logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
            if (response.getStatusCode().is2xxSuccessful()) {
                return getStatus(response.getBody());
            }
        } catch (Throwable th) {
            logger.error("Exception occurred in sync operation for " + agentVan + " for agency " + agencyId, th);
        }
        return false;
    }

    public ResponseEntity<String> validate(String uniqueId, String discom) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(meterAgentValidationUrl(discom))
                .queryParam("userid", uniqueId)
                .queryParam("discom", discom);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate
                    .exchange(builder.toUriString(), HttpMethod.GET, entity, String.class);
            logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
            return response;
        } catch (Throwable th) {
            logger.error("Exception occurred in validate operation for " + uniqueId
                    + " belonging to discom " + discom, th);
            return null;
        }
    }

	private boolean getStatus(String body) {
        JsonElement jsonElement = gson.fromJson(body, JsonElement.class);
        if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            JsonObject childResponse = jsonArray.get(0).getAsJsonObject();
            String status = childResponse.get("Status").getAsString();
            if (SUCCESS.equalsIgnoreCase(status)) {
                return true;
            }
        }
        return false;
    }

    private String meterAgentUrl(String discom) {
        String url = null;
        if (discom != null) {
            switch (discom.toUpperCase()) {
                case "PUVVNL":
                    url = integrationConfig.getPuvvnlMeterAgentUrl();
                    break;
                case "MVVNL":
                    url = integrationConfig.getMvvnlMeterAgentUrl();
                    break;
                case "PVVNL":
                    url = integrationConfig.getPvvnlMeterAgentUrl();
                    break;
                case "DVVNL":
                    url = integrationConfig.getDvvnlMeterAgentUrl();
                    break;
                default:
                    url = integrationConfig.getMeterAgentUrl();
            }
        }
        if (url == null) {
            url = integrationConfig.getMeterAgentUrl();
        }
        return url;
    }

    private String meterAgentValidationUrl(String discom) {
        String url = null;
        if (discom != null) {
            switch (discom.toUpperCase()) {
                case "PUVVNL":
                    url = integrationConfig.getPuvvnlMeterAgentValidationUrl();
                    break;
                case "MVVNL":
                    url = integrationConfig.getMvvnlMeterAgentValidationUrl();
                    break;
                case "PVVNL":
                    url = integrationConfig.getPvvnlMeterAgentValidationUrl();
                    break;
                case "DVVNL":
                    url = integrationConfig.getDvvnlMeterAgentValidationUrl();
                    break;
                default:
                    url = integrationConfig.getMeterAgentValidationUrl();
            }
        }
        if (url == null) {
            url = integrationConfig.getMeterAgentValidationUrl();
        }
        return url;
    }

}
