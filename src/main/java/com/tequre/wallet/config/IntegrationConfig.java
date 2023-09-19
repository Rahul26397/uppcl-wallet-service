package com.tequre.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegrationConfig {

	@Value("${integration.adminNode}")
    private String adminNode;

    @Value("${integration.adminUser}")
    private String adminUser;

    @Value("${integration.adminPassword}")
    private String adminPassword;

    @Value("${integration.notifyKey}")
    private String notifyKey;

    @Value("${integration.notifySecret}")
    private String notifySecret;

    @Value("${integration.notifyServer}")
    private String notifyServer;

    @Value("${integration.tokenServer}")
    private String tokenServer;

    @Value("${integration.authServer}")
    private String authServer;

    @Value("${integration.billPostKey}")
    private String billPostKey;

    @Value("${integration.billPostSecret}")
    private String billPostSecret;

    @Value("${integration.billPostServer}")
    private String billPostServer;

    @Value("${integration.rapdrpMeterAgentUrl}")
    private String rapdrpMeterAgentUrl;
    
    @Value("${integration.puvvnlRapdrpMeterAgentUrl}")
    private String puvvnlRapdrpMeterAgentUrl;
    
    @Value("${integration.pvvnlRapdrpMeterAgentUrl}")
    private String pvvnlRapdrpMeterAgentUrl;
    
    @Value("${integration.mvvnlRapdrpMeterAgentUrl}")
    private String mvvnlRapdrpMeterAgentUrl;
    
    @Value("${integration.dvvnlRapdrpMeterAgentUrl}")
    private String dvvnlRapdrpMeterAgentUrl;

    @Value("${integration.rapdrpMeterAgentUsername}")
    private String rapdrpMeterAgentUsername;
    
    @Value("${integration.rapdrpMeterAgentUsernameRMS}")
    private String rapdrpMeterAgentUsernameRMS;

    @Value("${integration.rapdrpMeterAgentPassword}")
    private String rapdrpMeterAgentPassword;
    
    @Value("${integration.rapdrpMeterAgentPasswordRMS}")
    private String rapdrpMeterAgentPasswordRMS;

    @Value("${integration.nonRapdrpMeterAgentConfig.meterAgentUrl}")
    private String meterAgentUrl;

    @Value("${integration.nonRapdrpMeterAgentConfig.meterAgentValidationUrl}")
    private String meterAgentValidationUrl;

    @Value("${integration.nonRapdrpMeterAgentConfig.puvvnlMeterAgentUrl}")
    private String puvvnlMeterAgentUrl;

    @Value("${integration.nonRapdrpMeterAgentConfig.puvvnlMeterAgentValidationUrl}")
    private String puvvnlMeterAgentValidationUrl;

    @Value("${integration.nonRapdrpMeterAgentConfig.mvvnlMeterAgentUrl}")
    private String mvvnlMeterAgentUrl;

    @Value("${integration.nonRapdrpMeterAgentConfig.mvvnlMeterAgentValidationUrl}")
    private String mvvnlMeterAgentValidationUrl;

    @Value("${integration.nonRapdrpMeterAgentConfig.pvvnlMeterAgentUrl}")
    private String pvvnlMeterAgentUrl;

    @Value("${integration.nonRapdrpMeterAgentConfig.pvvnlMeterAgentValidationUrl}")
    private String pvvnlMeterAgentValidationUrl;

    @Value("${integration.nonRapdrpMeterAgentConfig.dvvnlMeterAgentUrl}")
    private String dvvnlMeterAgentUrl;

    @Value("${integration.nonRapdrpMeterAgentConfig.dvvnlMeterAgentValidationUrl}")
    private String dvvnlMeterAgentValidationUrl;

    @Value("${integration.mapping.tokenUrl}")
    private String mappingTokenUrl;

    @Value("${integration.mapping.url}")
    private String mappingUrl;
    
    @Value("${integration.connectionCountUrl}")
    private String connectionCountUrl;

    @Value("${integration.mapping.username}")
    private String mappingUsername;

    @Value("${integration.mapping.password}")
    private String mappingPassword;

    @Value("${integration.mapping.consumerKey}")
    private String mappingConsumerKey;
    
    @Value("${integration.connectionCountConsumerKey}")
    private String connectionCountConsumerKey;
    
    @Value("${integration.connectionCountConsumerSecret}")
    private String connectionCountConsumerSecret;
    

    @Value("${integration.mapping.consumerSecret}")
    private String mappingConsumerSecret;

    public String getAdminNode() {
        return adminNode;
    }

    public void setAdminNode(String adminNode) {
        this.adminNode = adminNode;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getNotifyKey() {
        return notifyKey;
    }

    public void setNotifyKey(String notifyKey) {
        this.notifyKey = notifyKey;
    }

    public String getNotifySecret() {
        return notifySecret;
    }

    public void setNotifySecret(String notifySecret) {
        this.notifySecret = notifySecret;
    }

    public String getNotifyServer() {
        return notifyServer;
    }

    public void setNotifyServer(String notifyServer) {
        this.notifyServer = notifyServer;
    }

    public String getTokenServer() {
        return tokenServer;
    }

    public void setTokenServer(String tokenServer) {
        this.tokenServer = tokenServer;
    }

    public String getAuthServer() {
        return authServer;
    }

    public void setAuthServer(String authServer) {
        this.authServer = authServer;
    }

    public String getBillPostKey() {
        return billPostKey;
    }

    public void setBillPostKey(String billPostKey) {
        this.billPostKey = billPostKey;
    }

    public String getBillPostSecret() {
        return billPostSecret;
    }

    public void setBillPostSecret(String billPostSecret) {
        this.billPostSecret = billPostSecret;
    }

    public String getBillPostServer() {
        return billPostServer;
    }

    public void setBillPostServer(String billPostServer) {
        this.billPostServer = billPostServer;
    }

    public String getRapdrpMeterAgentUrl() {
        return rapdrpMeterAgentUrl;
    }

    public void setRapdrpMeterAgentUrl(String rapdrpMeterAgentUrl) {
        this.rapdrpMeterAgentUrl = rapdrpMeterAgentUrl;
    }
    
    public String getPuvvnlRapdrpMeterAgentUrl() {
        return puvvnlRapdrpMeterAgentUrl;
    }

    public void setPuvvnlRapdrpMeterAgentUrl(String rapdrpMeterAgentUrl) {
        this.puvvnlRapdrpMeterAgentUrl = rapdrpMeterAgentUrl;
    }
    
    public String getPvvnlRapdrpMeterAgentUrl() {
        return pvvnlRapdrpMeterAgentUrl;
    }

    public void setPvvnlRapdrpMeterAgentUrl(String rapdrpMeterAgentUrl) {
        this.pvvnlRapdrpMeterAgentUrl = rapdrpMeterAgentUrl;
    }
    
    public String getMvvnlRapdrpMeterAgentUrl() {
        return mvvnlRapdrpMeterAgentUrl;
    }

    public void setMvvnlRapdrpMeterAgentUrl(String rapdrpMeterAgentUrl) {
        this.mvvnlRapdrpMeterAgentUrl = rapdrpMeterAgentUrl;
    }
    
    public String getDvvnlRapdrpMeterAgentUrl() {
        return dvvnlRapdrpMeterAgentUrl;
    }

    public void setDvvnlRapdrpMeterAgentUrl(String rapdrpMeterAgentUrl) {
        this.dvvnlRapdrpMeterAgentUrl = rapdrpMeterAgentUrl;
    }

    public String getRapdrpMeterAgentUsername() {
        return rapdrpMeterAgentUsername;
    }
    
    public String getRapdrpMeterAgentUsernameRMS() {
        return rapdrpMeterAgentUsernameRMS;
    }

    public void setRapdrpMeterAgentUsername(String rapdrpMeterAgentUsername) {
        this.rapdrpMeterAgentUsername = rapdrpMeterAgentUsername;
    }
    
    public void setRapdrpMeterAgentUsernameRMS(String rapdrpMeterAgentUsername) {
        this.rapdrpMeterAgentUsernameRMS = rapdrpMeterAgentUsername;
    }

    public String getRapdrpMeterAgentPassword() {
        return rapdrpMeterAgentPassword;
    }

    public void setRapdrpMeterAgentPassword(String rapdrpMeterAgentPassword) {
        this.rapdrpMeterAgentPassword = rapdrpMeterAgentPassword;
    }
    
    public String getRapdrpMeterAgentPasswordRMS() {
        return rapdrpMeterAgentPasswordRMS;
    }

    public void setRapdrpMeterAgentPasswordRMS(String rapdrpMeterAgentPassword) {
        this.rapdrpMeterAgentPasswordRMS = rapdrpMeterAgentPassword;
    }

    public String getMappingTokenUrl() {
        return mappingTokenUrl;
    }

    public void setMappingTokenUrl(String mappingTokenUrl) {
        this.mappingTokenUrl = mappingTokenUrl;
    }

    public String getMappingUrl() {
        return mappingUrl;
    }
    
    public String getConnectionCountUrl() {
        return connectionCountUrl;
    }

    public void setMappingUrl(String mappingUrl) {
        this.mappingUrl = mappingUrl;
    }
    
    public void setconnectionCountUrl(String connectionCountUrl) {
        this.connectionCountUrl = connectionCountUrl;
    }

    public String getMappingUsername() {
        return mappingUsername;
    }

    public void setMappingUsername(String mappingUsername) {
        this.mappingUsername = mappingUsername;
    }

    public String getMappingPassword() {
        return mappingPassword;
    }

    public void setMappingPassword(String mappingPassword) {
        this.mappingPassword = mappingPassword;
    }

    public String getMappingConsumerKey() {
        return mappingConsumerKey;
    }

    public void setMappingConsumerKey(String mappingConsumerKey) {
        this.mappingConsumerKey = mappingConsumerKey;
    }
    
    public String getConnectionCountConsumerKey() {
        return connectionCountConsumerKey;
    }

    public void setConnectionCountConsumerKey(String connectionCountConsumerKey) {
        this.connectionCountConsumerKey = connectionCountConsumerKey;
    }
    
    public String getConnectionCountConsumerSecret() {
        return connectionCountConsumerSecret;
    }

    public void setConnectionCountConsumerSecret(String connectionCountConsumerSecret) {
        this.connectionCountConsumerSecret = connectionCountConsumerSecret;
    }

    public String getMappingConsumerSecret() {
        return mappingConsumerSecret;
    }

    public void setMappingConsumerSecret(String mappingConsumerSecret) {
        this.mappingConsumerSecret = mappingConsumerSecret;
    }

    public String getMeterAgentUrl() {
        return meterAgentUrl;
    }

    public void setMeterAgentUrl(String meterAgentUrl) {
        this.meterAgentUrl = meterAgentUrl;
    }

    public String getMeterAgentValidationUrl() {
        return meterAgentValidationUrl;
    }

    public void setMeterAgentValidationUrl(String meterAgentValidationUrl) {
        this.meterAgentValidationUrl = meterAgentValidationUrl;
    }

    public String getPuvvnlMeterAgentUrl() {
        return puvvnlMeterAgentUrl;
    }

    public void setPuvvnlMeterAgentUrl(String puvvnlMeterAgentUrl) {
        this.puvvnlMeterAgentUrl = puvvnlMeterAgentUrl;
    }

    public String getPuvvnlMeterAgentValidationUrl() {
        return puvvnlMeterAgentValidationUrl;
    }

    public void setPuvvnlMeterAgentValidationUrl(String puvvnlMeterAgentValidationUrl) {
        this.puvvnlMeterAgentValidationUrl = puvvnlMeterAgentValidationUrl;
    }

    public String getMvvnlMeterAgentUrl() {
        return mvvnlMeterAgentUrl;
    }

    public void setMvvnlMeterAgentUrl(String mvvnlMeterAgentUrl) {
        this.mvvnlMeterAgentUrl = mvvnlMeterAgentUrl;
    }

    public String getMvvnlMeterAgentValidationUrl() {
        return mvvnlMeterAgentValidationUrl;
    }

    public void setMvvnlMeterAgentValidationUrl(String mvvnlMeterAgentValidationUrl) {
        this.mvvnlMeterAgentValidationUrl = mvvnlMeterAgentValidationUrl;
    }

    public String getPvvnlMeterAgentUrl() {
        return pvvnlMeterAgentUrl;
    }

    public void setPvvnlMeterAgentUrl(String pvvnlMeterAgentUrl) {
        this.pvvnlMeterAgentUrl = pvvnlMeterAgentUrl;
    }

    public String getPvvnlMeterAgentValidationUrl() {
        return pvvnlMeterAgentValidationUrl;
    }

    public void setPvvnlMeterAgentValidationUrl(String pvvnlMeterAgentValidationUrl) {
        this.pvvnlMeterAgentValidationUrl = pvvnlMeterAgentValidationUrl;
    }

    public String getDvvnlMeterAgentUrl() {
        return dvvnlMeterAgentUrl;
    }

    public void setDvvnlMeterAgentUrl(String dvvnlMeterAgentUrl) {
        this.dvvnlMeterAgentUrl = dvvnlMeterAgentUrl;
    }

    public String getDvvnlMeterAgentValidationUrl() {
        return dvvnlMeterAgentValidationUrl;
    }

    public void setDvvnlMeterAgentValidationUrl(String dvvnlMeterAgentValidationUrl) {
        this.dvvnlMeterAgentValidationUrl = dvvnlMeterAgentValidationUrl;
    }
}
