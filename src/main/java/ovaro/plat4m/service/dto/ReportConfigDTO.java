package ovaro.plat4m.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportConfigDTO {

    private UUID id;
    private String reportKey;
    private String name;
    private String title;
    private String rowDimension;
    private String columnDimension;
    private boolean showPercentOfTotal;
    private String defaultView;
    private String datePreset;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean builtin;
    private boolean editable;
    private List<String> accountIds = new ArrayList<>();
    private List<String> categoryIds = new ArrayList<>();
    private List<String> payeeIds = new ArrayList<>();
    private List<String> familyMemberIds = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReportKey() {
        return reportKey;
    }

    public void setReportKey(String reportKey) {
        this.reportKey = reportKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRowDimension() {
        return rowDimension;
    }

    public void setRowDimension(String rowDimension) {
        this.rowDimension = rowDimension;
    }

    public String getColumnDimension() {
        return columnDimension;
    }

    public void setColumnDimension(String columnDimension) {
        this.columnDimension = columnDimension;
    }

    public boolean isShowPercentOfTotal() {
        return showPercentOfTotal;
    }

    public void setShowPercentOfTotal(boolean showPercentOfTotal) {
        this.showPercentOfTotal = showPercentOfTotal;
    }

    public String getDefaultView() {
        return defaultView;
    }

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }

    public String getDatePreset() {
        return datePreset;
    }

    public void setDatePreset(String datePreset) {
        this.datePreset = datePreset;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public List<String> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<String> accountIds) {
        this.accountIds = accountIds == null ? new ArrayList<>() : accountIds;
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds == null ? new ArrayList<>() : categoryIds;
    }

    public List<String> getPayeeIds() {
        return payeeIds;
    }

    public void setPayeeIds(List<String> payeeIds) {
        this.payeeIds = payeeIds == null ? new ArrayList<>() : payeeIds;
    }

    public List<String> getFamilyMemberIds() {
        return familyMemberIds;
    }

    public void setFamilyMemberIds(List<String> familyMemberIds) {
        this.familyMemberIds = familyMemberIds == null ? new ArrayList<>() : familyMemberIds;
    }
}
