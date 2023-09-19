package com.tequre.wallet.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tequre.wallet.config.IntegrationConfig;
import com.tequre.wallet.data.User;
import com.tequre.wallet.enums.Role;
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
public class UserService {

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private IntegrationConfig integrationConfig;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestTemplate restTemplate;

    private static String HEADER_TOKEN = "iplanetDirectoryPro";

    private static String HEADER_API_VERSION = "Accept-API-Version";

    private static String CREATE_USER = "/openam/json/realms/root/users/?_action=create";

    private static String UPDATE_USER = "/openam/json/realms/root/users/UID";

    private static String DELETE_USER = "/openam/json/realms/root/users/UID";

    private static String READ_USER = "/openam/json/realms/root/users/UID";

    private static String READ_USER_FROM_TOKEN = "/openam/json/sessions/?_action=getSessionInfo&tokenId=TOKEN_ID";

    public boolean createUser(String userName, String password, String email, Role role) {
        String version = "resource=3.0, protocol=2.1";
        String superToken = authService.getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_TOKEN, superToken);
        headers.set(HEADER_API_VERSION, version);
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject user = new JsonObject();
        user.addProperty("username", userName);
        user.addProperty("userpassword", password);
        user.addProperty("registeras", role.name());
        user.addProperty("mail", email);
        String userString = user.toString();
        HttpEntity<String> entity = new HttpEntity<>(userString, headers);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getAuthServer() + CREATE_USER, HttpMethod.POST, entity, String.class);
        logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 201) {
            return true;
        }
        return false;
    }

    public User updatePassword(String userName, String password) {
        String version = "resource=3.0, protocol=2.1";
        String superToken = authService.getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_TOKEN, superToken);
        headers.set(HEADER_API_VERSION, version);
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject user = new JsonObject();
        user.addProperty("password", password);
        String userString = user.toString();
        HttpEntity<String> entity = new HttpEntity<>(userString, headers);
        String constructedUrl = UPDATE_USER.replaceAll("UID", userName);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getAuthServer() + constructedUrl, HttpMethod.PUT, entity, String.class);
        logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonAgent = new Gson().fromJson(response.getBody(), JsonObject.class);
            String uid = jsonAgent.get("username").getAsString();
            String pswd = jsonAgent.get("password").getAsString();
            User updatedUser = new User();
            updatedUser.setUserName(uid);
            updatedUser.setPassword(pswd);
            return updatedUser;
        }
        return null;
    }

    public boolean deleteUser(String userName) {
        String version = "protocol=2.1,resource=3.0";
        String superToken = authService.getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_TOKEN, superToken);
        headers.set(HEADER_API_VERSION, version);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        String constructedUrl = DELETE_USER.replaceAll("UID", userName);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getAuthServer() + constructedUrl, HttpMethod.DELETE, entity, String.class);
        logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonAgent = new Gson().fromJson(response.getBody(), JsonObject.class);
            String success = jsonAgent.get("success").getAsString();
            if (Boolean.getBoolean(success)) {
                logger.info("User: " + userName + " deleted successfully.");
                return true;
            }
        }
        return false;
    }

    public boolean userExist(String userName) {
        String version = "resource=3.0,protocol=1.0";
        String superToken = authService.getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_TOKEN, superToken);
        headers.set(HEADER_API_VERSION, version);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        String constructedUrl = READ_USER.replaceAll("UID", userName);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getAuthServer() + constructedUrl, HttpMethod.GET, entity, String.class);
        logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            return true;
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        String version = "resource=2.1,protocol=1.0";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_API_VERSION, version);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        String constructedUrl = READ_USER_FROM_TOKEN.replaceAll("TOKEN_ID", token);
        ResponseEntity<String> response = restTemplate
                .exchange(integrationConfig.getAuthServer() + constructedUrl, HttpMethod.GET, entity, String.class);
        logger.info("Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCodeValue() == 200) {
            JsonObject jsonAgent = new Gson().fromJson(response.getBody(), JsonObject.class);
            String uid = jsonAgent.get("username").getAsString();
            return uid;
        }
        return null;
    }
}
