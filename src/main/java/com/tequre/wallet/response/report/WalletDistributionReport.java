package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "count", "items"})
public class WalletDistributionReport {

    private Integer count;

    private List<WalletDistributionReportEntry> items;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<WalletDistributionReportEntry> getItems() {
        return items;
    }

    public void setItems(List<WalletDistributionReportEntry> items) {
        this.items = items;
    }
}
