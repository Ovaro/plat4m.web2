package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IncomeExpenseReportDrilldownDTO {

    private String title;
    private String rowKey;
    private String rowLabel;
    private String columnKey;
    private String columnLabel;
    private String currencyCode;
    private BigDecimal total = BigDecimal.ZERO;
    private List<TransactionDTO> transactions = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public List<TransactionDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDTO> transactions) {
        this.transactions = transactions;
    }

    public static class TransactionDTO {

        private String id;
        private LocalDate date;
        private String accountId;
        private String payeeName;
        private String categoryName;
        private String familyMemberName;
        private String memo;
        private String sectionLabel;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal grossAmount;
        private boolean lotAdjusted;
        private boolean hasLotDetails;
        private String securityId;
        private String securityName;
        private String originalCurrencyCode;
        private BigDecimal originalAmount;
        private Double fxRateToBase;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getPayeeName() {
            return payeeName;
        }

        public void setPayeeName(String payeeName) {
            this.payeeName = payeeName;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getFamilyMemberName() {
            return familyMemberName;
        }

        public void setFamilyMemberName(String familyMemberName) {
            this.familyMemberName = familyMemberName;
        }

        public String getMemo() {
            return memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }

        public String getSectionLabel() {
            return sectionLabel;
        }

        public void setSectionLabel(String sectionLabel) {
            this.sectionLabel = sectionLabel;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getGrossAmount() {
            return grossAmount;
        }

        public void setGrossAmount(BigDecimal grossAmount) {
            this.grossAmount = grossAmount;
        }

        public boolean isLotAdjusted() {
            return lotAdjusted;
        }

        public void setLotAdjusted(boolean lotAdjusted) {
            this.lotAdjusted = lotAdjusted;
        }

        public boolean isHasLotDetails() {
            return hasLotDetails;
        }

        public void setHasLotDetails(boolean hasLotDetails) {
            this.hasLotDetails = hasLotDetails;
        }

        public String getSecurityId() {
            return securityId;
        }

        public void setSecurityId(String securityId) {
            this.securityId = securityId;
        }

        public String getSecurityName() {
            return securityName;
        }

        public void setSecurityName(String securityName) {
            this.securityName = securityName;
        }

        public String getOriginalCurrencyCode() {
            return originalCurrencyCode;
        }

        public void setOriginalCurrencyCode(String originalCurrencyCode) {
            this.originalCurrencyCode = originalCurrencyCode;
        }

        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        public void setOriginalAmount(BigDecimal originalAmount) {
            this.originalAmount = originalAmount;
        }

        public Double getFxRateToBase() {
            return fxRateToBase;
        }

        public void setFxRateToBase(Double fxRateToBase) {
            this.fxRateToBase = fxRateToBase;
        }
    }
}
