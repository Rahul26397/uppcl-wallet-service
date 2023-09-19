package com.tequre.wallet.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmsRequest implements Serializable {

    private String templateId;

    private Set<Tuple> fields;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Set<Tuple> getFields() {
        return fields;
    }

    public void setFields(Set<Tuple> fields) {
        this.fields = fields;
    }
}
