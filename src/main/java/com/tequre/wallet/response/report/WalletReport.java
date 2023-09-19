package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"nextPageToken", "items"})
public class WalletReport {

    private String nextPageToken;

    private List<WalletReportEntry> items;

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public List<WalletReportEntry> getItems() {
        return items;
    }

    public void setItems(List<WalletReportEntry> items) {
        this.items = items;
    }
}
