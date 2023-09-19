package com.tequre.wallet.config;

import com.tequre.wallet.enums.Mode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Configuration
public class HLFConfig {

    @Value("${hlf.url}")
    private String url;

    @Value("${hlf.mode}")
    @Enumerated(EnumType.STRING)
    private Mode mode;

    @Value("${hlf.isPrimary}")
    private boolean isPrimary;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}
