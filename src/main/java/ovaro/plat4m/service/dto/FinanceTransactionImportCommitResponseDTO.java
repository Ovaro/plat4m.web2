package ovaro.plat4m.service.dto;

public class FinanceTransactionImportCommitResponseDTO {

    private String importId;
    private Integer createdTransactionCount;
    private String correlationStatus;
    private String correlationMessage;
    private String focusTransactionId;
    private Integer focusRowIndex;

    public String getImportId() {
        return importId;
    }

    public void setImportId(String importId) {
        this.importId = importId;
    }

    public Integer getCreatedTransactionCount() {
        return createdTransactionCount;
    }

    public void setCreatedTransactionCount(Integer createdTransactionCount) {
        this.createdTransactionCount = createdTransactionCount;
    }

    public String getCorrelationStatus() {
        return correlationStatus;
    }

    public void setCorrelationStatus(String correlationStatus) {
        this.correlationStatus = correlationStatus;
    }

    public String getCorrelationMessage() {
        return correlationMessage;
    }

    public void setCorrelationMessage(String correlationMessage) {
        this.correlationMessage = correlationMessage;
    }

    public String getFocusTransactionId() {
        return focusTransactionId;
    }

    public void setFocusTransactionId(String focusTransactionId) {
        this.focusTransactionId = focusTransactionId;
    }

    public Integer getFocusRowIndex() {
        return focusRowIndex;
    }

    public void setFocusRowIndex(Integer focusRowIndex) {
        this.focusRowIndex = focusRowIndex;
    }
}
