package ovaro.plat4m.service.dto;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FinanceDemoDataBatchDTO {

    private UUID generatedBatchId;
    private String generatedLabel;
    private ZonedDateTime createdAt;
    private long accountCount;
    private long transactionCount;
    private long payeeCount;
    private long securityCount;
    private long priceCount;
    private long lotCount;
    private List<String> accountNames = new ArrayList<>();

    public UUID getGeneratedBatchId() {
        return generatedBatchId;
    }

    public void setGeneratedBatchId(UUID generatedBatchId) {
        this.generatedBatchId = generatedBatchId;
    }

    public String getGeneratedLabel() {
        return generatedLabel;
    }

    public void setGeneratedLabel(String generatedLabel) {
        this.generatedLabel = generatedLabel;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(long accountCount) {
        this.accountCount = accountCount;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public long getPayeeCount() {
        return payeeCount;
    }

    public void setPayeeCount(long payeeCount) {
        this.payeeCount = payeeCount;
    }

    public long getSecurityCount() {
        return securityCount;
    }

    public void setSecurityCount(long securityCount) {
        this.securityCount = securityCount;
    }

    public long getPriceCount() {
        return priceCount;
    }

    public void setPriceCount(long priceCount) {
        this.priceCount = priceCount;
    }

    public long getLotCount() {
        return lotCount;
    }

    public void setLotCount(long lotCount) {
        this.lotCount = lotCount;
    }

    public List<String> getAccountNames() {
        return accountNames;
    }

    public void setAccountNames(List<String> accountNames) {
        this.accountNames = accountNames;
    }
}
