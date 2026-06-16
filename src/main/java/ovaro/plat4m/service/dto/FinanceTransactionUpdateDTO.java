package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FinanceTransactionUpdateDTO {

    private LocalDate date;
    private BigDecimal amount;
    private String payeeId;
    private String payeeName;
    private String categoryId;
    private String whoId;
    private String transferredAccountId;
    private String memo;
    private Boolean cleared;
    private List<String> tags;
    private List<SplitLineDTO> splits;
    private Boolean replaceWithTransfer;

    public static class SplitLineDTO {

        private String categoryId;
        private String categoryName;
        private String whoId;
        private String whoName;
        private String memo;
        private BigDecimal amount;

        public String getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(String categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getWhoId() {
            return whoId;
        }

        public void setWhoId(String whoId) {
            this.whoId = whoId;
        }

        public String getWhoName() {
            return whoName;
        }

        public void setWhoName(String whoName) {
            this.whoName = whoName;
        }

        public String getMemo() {
            return memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public void setPayeeId(String payeeId) {
        this.payeeId = payeeId;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getWhoId() {
        return whoId;
    }

    public void setWhoId(String whoId) {
        this.whoId = whoId;
    }

    public String getTransferredAccountId() {
        return transferredAccountId;
    }

    public void setTransferredAccountId(String transferredAccountId) {
        this.transferredAccountId = transferredAccountId;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Boolean getCleared() {
        return cleared;
    }

    public void setCleared(Boolean cleared) {
        this.cleared = cleared;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<SplitLineDTO> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitLineDTO> splits) {
        this.splits = splits;
    }

    public Boolean getReplaceWithTransfer() {
        return replaceWithTransfer;
    }

    public void setReplaceWithTransfer(Boolean replaceWithTransfer) {
        this.replaceWithTransfer = replaceWithTransfer;
    }
}
