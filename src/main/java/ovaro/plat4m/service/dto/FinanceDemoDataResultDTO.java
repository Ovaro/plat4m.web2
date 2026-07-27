package ovaro.plat4m.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FinanceDemoDataResultDTO {

    private UUID generatedBatchId;
    private String generatedLabel;
    private int accountCount;
    private int transactionCount;
    private int payeeCount;
    private int securityCount;
    private int priceCount;
    private int lotCount;
    private List<String> accountNames = new ArrayList<>();
    private List<String> notes = new ArrayList<>();
    private boolean preview;

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

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public int getPayeeCount() {
        return payeeCount;
    }

    public void setPayeeCount(int payeeCount) {
        this.payeeCount = payeeCount;
    }

    public int getSecurityCount() {
        return securityCount;
    }

    public void setSecurityCount(int securityCount) {
        this.securityCount = securityCount;
    }

    public int getPriceCount() {
        return priceCount;
    }

    public void setPriceCount(int priceCount) {
        this.priceCount = priceCount;
    }

    public int getLotCount() {
        return lotCount;
    }

    public void setLotCount(int lotCount) {
        this.lotCount = lotCount;
    }

    public List<String> getAccountNames() {
        return accountNames;
    }

    public void setAccountNames(List<String> accountNames) {
        this.accountNames = accountNames;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }
}
