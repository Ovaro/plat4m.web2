package ovaro.plat4m.service.dto;

import java.util.ArrayList;
import java.util.List;

public class FinanceSecurityPriceRefreshResultDTO {

    private String jobId;
    private String status;
    private String currentSymbol;
    private String currentMessage;
    private boolean complete;
    private long startedAtEpochMs;
    private Long completedAtEpochMs;
    private int requestedCount;
    private int processedCount;
    private int refreshedCount;
    private int skippedCount;
    private int failedCount;
    private int appliedCount;
    private boolean applyRequested;
    private List<FinanceSecurityPriceRefreshItemDTO> items = new ArrayList<>();

    public int getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentSymbol() {
        return currentSymbol;
    }

    public void setCurrentSymbol(String currentSymbol) {
        this.currentSymbol = currentSymbol;
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public void setCurrentMessage(String currentMessage) {
        this.currentMessage = currentMessage;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public long getStartedAtEpochMs() {
        return startedAtEpochMs;
    }

    public void setStartedAtEpochMs(long startedAtEpochMs) {
        this.startedAtEpochMs = startedAtEpochMs;
    }

    public Long getCompletedAtEpochMs() {
        return completedAtEpochMs;
    }

    public void setCompletedAtEpochMs(Long completedAtEpochMs) {
        this.completedAtEpochMs = completedAtEpochMs;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(int processedCount) {
        this.processedCount = processedCount;
    }

    public int getRefreshedCount() {
        return refreshedCount;
    }

    public void setRefreshedCount(int refreshedCount) {
        this.refreshedCount = refreshedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getAppliedCount() {
        return appliedCount;
    }

    public void setAppliedCount(int appliedCount) {
        this.appliedCount = appliedCount;
    }

    public boolean isApplyRequested() {
        return applyRequested;
    }

    public void setApplyRequested(boolean applyRequested) {
        this.applyRequested = applyRequested;
    }

    public List<FinanceSecurityPriceRefreshItemDTO> getItems() {
        return items;
    }

    public void setItems(List<FinanceSecurityPriceRefreshItemDTO> items) {
        this.items = items;
    }
}
