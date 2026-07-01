package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceTransactionImportRowUpdateDTO {

    private LocalDate date;
    private BigDecimal amount;
    private String payeeText;
    private String resolvedTransferAccountId;
    private Boolean externalTransferLike;
    private String resolvedPayeeId;
    private String resolvedCategoryId;
    private String memo;
    private Boolean accepted;
    private Boolean ignored;
    private Boolean applyDuplicateResolution;
    private Boolean duplicateConfirmed;
    private Boolean duplicateRejected;

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

    public String getPayeeText() {
        return payeeText;
    }

    public void setPayeeText(String payeeText) {
        this.payeeText = payeeText;
    }

    public String getResolvedTransferAccountId() {
        return resolvedTransferAccountId;
    }

    public void setResolvedTransferAccountId(String resolvedTransferAccountId) {
        this.resolvedTransferAccountId = resolvedTransferAccountId;
    }

    public Boolean getExternalTransferLike() {
        return externalTransferLike;
    }

    public void setExternalTransferLike(Boolean externalTransferLike) {
        this.externalTransferLike = externalTransferLike;
    }

    public String getResolvedPayeeId() {
        return resolvedPayeeId;
    }

    public void setResolvedPayeeId(String resolvedPayeeId) {
        this.resolvedPayeeId = resolvedPayeeId;
    }

    public String getResolvedCategoryId() {
        return resolvedCategoryId;
    }

    public void setResolvedCategoryId(String resolvedCategoryId) {
        this.resolvedCategoryId = resolvedCategoryId;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getIgnored() {
        return ignored;
    }

    public void setIgnored(Boolean ignored) {
        this.ignored = ignored;
    }

    public Boolean getApplyDuplicateResolution() {
        return applyDuplicateResolution;
    }

    public void setApplyDuplicateResolution(Boolean applyDuplicateResolution) {
        this.applyDuplicateResolution = applyDuplicateResolution;
    }

    public Boolean getDuplicateConfirmed() {
        return duplicateConfirmed;
    }

    public void setDuplicateConfirmed(Boolean duplicateConfirmed) {
        this.duplicateConfirmed = duplicateConfirmed;
    }

    public Boolean getDuplicateRejected() {
        return duplicateRejected;
    }

    public void setDuplicateRejected(Boolean duplicateRejected) {
        this.duplicateRejected = duplicateRejected;
    }
}
