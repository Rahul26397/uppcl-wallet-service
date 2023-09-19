package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "vanId", "balance", "blockchainBalance" })
public class BlockchainWalletEntry {

    private String vanId;

    private Double balance;

    private Double blockchainBalance;

    public String getVanId() {
        return vanId;
    }

    public void setVanId(String vanId) {
        this.vanId = vanId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Double getBlockchainBalance() {
        return blockchainBalance;
    }

    public void setBlockchainBalance(Double blockchainBalance) {
        this.blockchainBalance = blockchainBalance;
    }
}
