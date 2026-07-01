package ovaro.plat4m.service.dto;

import java.util.List;

public class FinanceTransactionImportDTO {

    private String importId;
    private String accountId;
    private String accountName;
    private String status;
    private String correlationStatus;
    private String correlationMessage;
    private Integer totalRows;
    private Integer flaggedRows;
    private String createdDateTime;
    private List<FinanceTransactionImportRowDTO> rows;

    public String getImportId() {
        return importId;
    }

    public void setImportId(String importId) {
        this.importId = importId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getFlaggedRows() {
        return flaggedRows;
    }

    public void setFlaggedRows(Integer flaggedRows) {
        this.flaggedRows = flaggedRows;
    }

    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public List<FinanceTransactionImportRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<FinanceTransactionImportRowDTO> rows) {
        this.rows = rows;
    }
}
