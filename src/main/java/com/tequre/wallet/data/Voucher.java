package com.tequre.wallet.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.tequre.wallet.enums.VoucherStatus;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Date;

@JsonInclude(Include.NON_NULL)
@Document(collection = "voucher")
public class Voucher extends BaseEntity {

    private String name;

    private Double amount;

    private String agentId;

    @Enumerated(EnumType.STRING)
    private VoucherStatus status;

    private String remark;

    private String invoiceId;

    private String paymentTarget;

    private Date redeemedDate;

    private Date expiryDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public VoucherStatus getStatus() {
        return status;
    }

    public void setStatus(VoucherStatus status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getPaymentTarget() {
        return paymentTarget;
    }

    public void setPaymentTarget(String paymentTarget) {
        this.paymentTarget = paymentTarget;
    }

    public Date getRedeemedDate() {
        return redeemedDate;
    }

    public void setRedeemedDate(Date redeemedDate) {
        this.redeemedDate = redeemedDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    @Override
    public String toString() {
        return "Voucher{" +
                "id='" + super.getId() + '\'' +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", agentId='" + agentId + '\'' +
                ", status=" + status +
                ", remark='" + remark + '\'' +
                ", invoiceId='" + invoiceId + '\'' +
                ", paymentTarget='" + paymentTarget + '\'' +
                ", redeemedDate=" + redeemedDate +
                ", expiryDate=" + expiryDate +
                '}';
    }
}
