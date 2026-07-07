package ovaro.plat4m.service.dto;

public class IncomeExpenseReportDrilldownRequestDTO {

    private ReportConfigDTO config;
    private String rowKey;
    private String rowLabel;
    private String columnKey;
    private String columnLabel;

    public ReportConfigDTO getConfig() {
        return config;
    }

    public void setConfig(ReportConfigDTO config) {
        this.config = config;
    }

    public String getRowKey() {
        return rowKey;
    }

    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public String getColumnKey() {
        return columnKey;
    }

    public void setColumnKey(String columnKey) {
        this.columnKey = columnKey;
    }

    public String getColumnLabel() {
        return columnLabel;
    }

    public void setColumnLabel(String columnLabel) {
        this.columnLabel = columnLabel;
    }
}
