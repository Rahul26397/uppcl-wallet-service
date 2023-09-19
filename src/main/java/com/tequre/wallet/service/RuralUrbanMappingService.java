package com.tequre.wallet.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tequre.wallet.config.IntegrationConfig;
import com.tequre.wallet.response.RuralUrbanMappingResponse;
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

import java.util.Base64;

import static com.tequre.wallet.utils.Constants.AUTHORIZATION_HEADER;

@Service
public class RuralUrbanMappingService {

    private final Logger logger = LoggerFactory.getLogger(RuralUrbanMappingService.class);

    @Autowired
    private Gson gson;

    @Autowired
    private IntegrationConfig integrationConfig;

    @Autowired
    private RestTemplate restTemplate;

    private String getBearerToken() {
        String key = integrationConfig.getMappingConsumerKey() + ":" + integrationConfig.getMappingConsumerSecret();
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
            throw new IllegalStateException("Unable to get mapping details");
        }
    }

    public RuralUrbanMappingResponse getMapping(String area, String code) {
        HttpHeaders headers = new HttpHeaders();
        String token = getBearerToken();
        headers.set(AUTHORIZATION_HEADER, "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(integrationConfig.getMappingUrl())
                .queryParam("area", area)
                .queryParam("code", code);
        ResponseEntity<String> response = restTemplate
                .exchange(builder.toUriString(), HttpMethod.GET, entity, String.class);
        logger.info("Mapping Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonAgent = gson.fromJson(response.getBody(), JsonObject.class);
            String status = jsonAgent.getAsJsonPrimitive("status").getAsString();
            if ("200".equalsIgnoreCase(status)) {
                return gson.fromJson(response.getBody(), RuralUrbanMappingResponse.class);
            }
        }
        return null;
    }
}
