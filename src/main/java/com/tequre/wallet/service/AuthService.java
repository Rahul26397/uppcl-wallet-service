package com.tequre.wallet.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tequre.wallet.config.IntegrationConfig;
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

@Service
public class AuthService {

    private final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static String API_VERSION = "resource=2.0, protocol=1.0";

    private static String HEADER_USERNAME = "X-OpenAM-Username";

    private static String HEADER_PASSWORD = "X-OpenAM-Password";

    private static String HEADER_API_VERSION = "Accept-API-Version";

    private static String AUTH_URL = "/openam/json/realms/root/authenticate";

    private static String VALIDATE_TOKEN_URL = "/openam/json/sessions/TOKEN_ID?_action=validate";

    @Autowired
    private IntegrationConfig integrationConfig;

    @Autowired
    private RestTemplate restTemplate;

    public String getAdminToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USERNAME, integrationConfig.getAdminUser());
        headers.set(HEADER_PASSWORD, integrationConfig.getAdminPassword());
        headers.set(HEADER_API_VERSION, API_VERSION);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getAuthServer() + AUTH_URL, HttpMethod.POST, entity, String.class);
        logger.info("Auth Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonAgent = new Gson().fromJson(response.getBody(), JsonObject.class);
            String tokenId = jsonAgent.get("tokenId").getAsString();
            return tokenId;
        }
        return null;
    }

    public String validateToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_API_VERSION, API_VERSION);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        String constructedUrl = VALIDATE_TOKEN_URL.replaceAll("TOKEN_ID", token);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getAuthServer() + constructedUrl, HttpMethod.POST, entity, String.class);
        logger.info("Validate Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonAgent = new Gson().fromJson(response.getBody(), JsonObject.class);
            boolean isValid = jsonAgent.getAsJsonObject("valid").getAsBoolean();
            if (isValid) {
                return jsonAgent.get("sessionUid").getAsString();
            }
        }
        return null;
    }
}
