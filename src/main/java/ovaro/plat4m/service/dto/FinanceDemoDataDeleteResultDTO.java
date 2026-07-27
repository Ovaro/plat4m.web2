package ovaro.plat4m.service.dto;

import java.util.UUID;

public class FinanceDemoDataDeleteResultDTO {

    private UUID generatedBatchId;
    private String generatedLabel;
    private long accountCount;
    private long transactionCount;
    private long payeeCount;
    private long securityCount;
    private long priceCount;
    private long lotCount;
    private long institutionCount;
    private long sourceLinkCount;

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

    public long getInstitutionCount() {
        return institutionCount;
    }

    public void setInstitutionCount(long institutionCount) {
        this.institutionCount = institutionCount;
    }

    public long getSourceLinkCount() {
        return sourceLinkCount;
    }

    public void setSourceLinkCount(long sourceLinkCount) {
        this.sourceLinkCount = sourceLinkCount;
    }
}
