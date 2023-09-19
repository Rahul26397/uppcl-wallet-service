package com.tequre.wallet.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tequre.wallet.data.BaseEntity;
import com.tequre.wallet.enums.EventStatus;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "events")
public class Event extends BaseEntity {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date date;

    private String type;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private String reason;

    private Object payload;

    private Object response;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + super.getId() + '\'' +
                ", date=" + date +
                ", type='" + type + '\'' +
                '}';
    }
}
