package com.tequre.wallet.data;

public class Activity {

    public Double amount;

    public String txnId;

    public String type;

    public String user;

    public String transactionId;

    public String timestamp;

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "amount=" + amount +
                ", txnId='" + txnId + '\'' +
                ", type='" + type + '\'' +
                ", user='" + user + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
