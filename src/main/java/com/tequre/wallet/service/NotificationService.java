package com.tequre.wallet.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tequre.wallet.config.IntegrationConfig;
import com.tequre.wallet.request.MailRequest;
import com.tequre.wallet.request.SmsRequest;
import com.tequre.wallet.request.Tuple;
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

import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.tequre.wallet.utils.Constants.AUTHORIZATION_HEADER;

@Service
public class NotificationService {

    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private IntegrationConfig integrationConfig;

    @Autowired
    private RestTemplate restTemplate;


    public void email(String templateId, Map<String, String> fields) {
        String accessToken = getToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTemplateId(templateId);
        Set<Tuple> tuples = new HashSet<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            Tuple t = new Tuple();
            t.setKey(entry.getKey());
            t.setValue(entry.getValue());
            tuples.add(t);
        }
        mailRequest.setFields(tuples);
        Gson gson = new Gson();
        String mailString = gson.toJson(mailRequest);
        logger.info("Mail Body: " + mailString);
        HttpEntity<MailRequest> entity = new HttpEntity<>(mailRequest, headers);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getNotifyServer(), HttpMethod.POST, entity, String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            logger.error("Error Sending Mail: ", response.getBody());
        }
    }

    public void sms(String templateId, Map<String, String> fields) {
        String accessToken = getToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        SmsRequest smsRequest = new SmsRequest();
        smsRequest.setTemplateId(templateId);
        Set<Tuple> tuples = new HashSet<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            Tuple t = new Tuple();
            t.setKey(entry.getKey());
            t.setValue(entry.getValue());
            tuples.add(t);
        }
        smsRequest.setFields(tuples);
        Gson gson = new Gson();
        String smsString = gson.toJson(smsRequest);
        logger.info("SMS Body: " + smsString);
        HttpEntity<SmsRequest> entity = new HttpEntity<>(smsRequest, headers);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getNotifyServer(), HttpMethod.POST, entity, String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            logger.error("Error Sending SMS: ", response.getBody());
        }
    }

    private String getToken() {
        String key = integrationConfig.getNotifyKey() + ":" + integrationConfig.getNotifySecret();
        String base64Encoded = Base64.getEncoder().encodeToString(key.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION_HEADER, "Basic " + base64Encoded);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getTokenServer() + "?grant_type=client_credentials",
                        HttpMethod.POST, entity, String.class);
        logger.info("Auth Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonBody = new Gson().fromJson(response.getBody(), JsonObject.class);
            String accessToken = jsonBody.get("access_token").getAsString();
            return accessToken;
        }
        return null;
    }
}
