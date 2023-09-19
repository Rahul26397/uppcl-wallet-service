package com.tequre.wallet.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"items"})
public class DiscomDivisionReport {

    private List<DiscomDivisionReportEntry> items;

    public List<DiscomDivisionReportEntry> getItems() {
        return items;
    }

    public void setItems(List<DiscomDivisionReportEntry> items) {
        this.items = items;
    }
}
