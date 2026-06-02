package com.waterpark.tickershow.dto.request;

import java.math.BigDecimal;

public class SepayWebhookRequest {
    private Long id;
    private String gateway;
    private String transactionDate;
    private String accountNumber;
    private String subAccount;
    private String transferType;
    private BigDecimal transferAmount;
    private BigDecimal accumulated;
    private String code;
    private String content;
    private String referenceCode;
    private String description;

    public Long getId() {
        return id;
    }

    public String getGateway() {
        return gateway;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getSubAccount() {
        return subAccount;
    }

    public String getTransferType() {
        return transferType;
    }

    public BigDecimal getTransferAmount() {
        return transferAmount;
    }

    public BigDecimal getAccumulated() {
        return accumulated;
    }

    public String getCode() {
        return code;
    }

    public String getContent() {
        return content;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public String getDescription() {
        return description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setSubAccount(String subAccount) {
        this.subAccount = subAccount;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public void setTransferAmount(BigDecimal transferAmount) {
        this.transferAmount = transferAmount;
    }

    public void setAccumulated(BigDecimal accumulated) {
        this.accumulated = accumulated;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}