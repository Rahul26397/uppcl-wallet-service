package com.tequre.wallet.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "wallet")
public class Wallet {

    @Id
    @JsonProperty("vanId")
    private String id;

    private Double balance;

    private String walletType;

    private String walletStatus;

    private boolean blockchainCommit;

    @JsonProperty("vanId")
    public String getId() {
        return id;
    }

    @JsonProperty("vanId")
    public void setId(String id) {
        this.id = id;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getWalletType() {
        return walletType;
    }

    public void setWalletType(String walletType) {
        this.walletType = walletType;
    }

    public String getWalletStatus() {
        return walletStatus;
    }

    public void setWalletStatus(String walletStatus) {
        this.walletStatus = walletStatus;
    }

    public boolean isBlockchainCommit() {
        return blockchainCommit;
    }

    public void setBlockchainCommit(boolean blockchainCommit) {
        this.blockchainCommit = blockchainCommit;
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "id='" + id + '\'' +
                ", balance=" + balance +
                ", walletType='" + walletType + '\'' +
                ", walletStatus='" + walletStatus + '\'' +
                '}';
    }
}
