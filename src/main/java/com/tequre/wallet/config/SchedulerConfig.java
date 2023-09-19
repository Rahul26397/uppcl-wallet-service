package com.tequre.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerConfig {

    @Value("${scheduler.meterAgentSync}")
    private String meterAgentSync;

    @Value("${scheduler.lowWalletBalance}")
    private String lowWalletBalance;

    public String getMeterAgentSync() {
        return meterAgentSync;
    }

    public void setMeterAgentSync(String meterAgentSync) {
        this.meterAgentSync = meterAgentSync;
    }

    public String getLowWalletBalance() {
        return lowWalletBalance;
    }

    public void setLowWalletBalance(String lowWalletBalance) {
        this.lowWalletBalance = lowWalletBalance;
    }
}
