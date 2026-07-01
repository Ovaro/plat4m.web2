package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceTransactionImportRowDTO {

    private String id;
    private Integer rowIndex;
    private LocalDate date;
    private BigDecimal amount;
    private String transactionKind;
    private String payeeText;
    private String transferAccountText;
    private String memo;
    private BigDecimal runningBalance;
    private String aiCategoryGuess;
    private Double aiConfidence;
    private String resolvedPayeeId;
    private String resolvedPayeeName;
    private String resolvedCategoryId;
    private String resolvedCategoryName;
    private String resolvedTransferAccountId;
    private String resolvedTransferAccountName;
    private boolean payeeNeedsReview;
    private boolean categoryNeedsReview;
    private boolean transferNeedsReview;
    private boolean externalTransferLike;
    private boolean dateNeedsReview;
    private boolean amountNeedsReview;
    private boolean balanceMismatch;
    private boolean duplicateSuspected;
    private boolean duplicateStrongMatch;
    private String duplicateTransactionId;
    private LocalDate duplicateTransactionDate;
    private BigDecimal duplicateTransactionAmount;
    private String duplicateTransactionPayeeName;
    private boolean duplicateConfirmed;
    private boolean duplicateRejected;
    private boolean accepted;
    private boolean ignored;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
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

    public String getTransactionKind() {
        return transactionKind;
    }

    public void setTransactionKind(String transactionKind) {
        this.transactionKind = transactionKind;
    }

    public String getPayeeText() {
        return payeeText;
    }

    public void setPayeeText(String payeeText) {
        this.payeeText = payeeText;
    }

    public String getTransferAccountText() {
        return transferAccountText;
    }

    public void setTransferAccountText(String transferAccountText) {
        this.transferAccountText = transferAccountText;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance;
    }

    public String getAiCategoryGuess() {
        return aiCategoryGuess;
    }

    public void setAiCategoryGuess(String aiCategoryGuess) {
        this.aiCategoryGuess = aiCategoryGuess;
    }

    public Double getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(Double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public String getResolvedPayeeId() {
        return resolvedPayeeId;
    }

    public void setResolvedPayeeId(String resolvedPayeeId) {
        this.resolvedPayeeId = resolvedPayeeId;
    }

    public String getResolvedPayeeName() {
        return resolvedPayeeName;
    }

    public void setResolvedPayeeName(String resolvedPayeeName) {
        this.resolvedPayeeName = resolvedPayeeName;
    }

    public String getResolvedCategoryId() {
        return resolvedCategoryId;
    }

    public void setResolvedCategoryId(String resolvedCategoryId) {
        this.resolvedCategoryId = resolvedCategoryId;
    }

    public String getResolvedCategoryName() {
        return resolvedCategoryName;
    }

    public void setResolvedCategoryName(String resolvedCategoryName) {
        this.resolvedCategoryName = resolvedCategoryName;
    }

    public String getResolvedTransferAccountId() {
        return resolvedTransferAccountId;
    }

    public void setResolvedTransferAccountId(String resolvedTransferAccountId) {
        this.resolvedTransferAccountId = resolvedTransferAccountId;
    }

    public String getResolvedTransferAccountName() {
        return resolvedTransferAccountName;
    }

    public void setResolvedTransferAccountName(String resolvedTransferAccountName) {
        this.resolvedTransferAccountName = resolvedTransferAccountName;
    }

    public boolean isPayeeNeedsReview() {
        return payeeNeedsReview;
    }

    public void setPayeeNeedsReview(boolean payeeNeedsReview) {
        this.payeeNeedsReview = payeeNeedsReview;
    }

    public boolean isCategoryNeedsReview() {
        return categoryNeedsReview;
    }

    public void setCategoryNeedsReview(boolean categoryNeedsReview) {
        this.categoryNeedsReview = categoryNeedsReview;
    }

    public boolean isTransferNeedsReview() {
        return transferNeedsReview;
    }

    public void setTransferNeedsReview(boolean transferNeedsReview) {
        this.transferNeedsReview = transferNeedsReview;
    }

    public boolean isExternalTransferLike() {
        return externalTransferLike;
    }

    public void setExternalTransferLike(boolean externalTransferLike) {
        this.externalTransferLike = externalTransferLike;
    }

    public boolean isDateNeedsReview() {
        return dateNeedsReview;
    }

    public void setDateNeedsReview(boolean dateNeedsReview) {
        this.dateNeedsReview = dateNeedsReview;
    }

    public boolean isAmountNeedsReview() {
        return amountNeedsReview;
    }

    public void setAmountNeedsReview(boolean amountNeedsReview) {
        this.amountNeedsReview = amountNeedsReview;
    }

    public boolean isBalanceMismatch() {
        return balanceMismatch;
    }

    public void setBalanceMismatch(boolean balanceMismatch) {
        this.balanceMismatch = balanceMismatch;
    }

    public boolean isDuplicateSuspected() {
        return duplicateSuspected;
    }

    public void setDuplicateSuspected(boolean duplicateSuspected) {
        this.duplicateSuspected = duplicateSuspected;
    }

    public boolean isDuplicateStrongMatch() {
        return duplicateStrongMatch;
    }

    public void setDuplicateStrongMatch(boolean duplicateStrongMatch) {
        this.duplicateStrongMatch = duplicateStrongMatch;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public String getDuplicateTransactionId() {
        return duplicateTransactionId;
    }

    public void setDuplicateTransactionId(String duplicateTransactionId) {
        this.duplicateTransactionId = duplicateTransactionId;
    }

    public LocalDate getDuplicateTransactionDate() {
        return duplicateTransactionDate;
    }

    public void setDuplicateTransactionDate(LocalDate duplicateTransactionDate) {
        this.duplicateTransactionDate = duplicateTransactionDate;
    }

    public BigDecimal getDuplicateTransactionAmount() {
        return duplicateTransactionAmount;
    }

    public void setDuplicateTransactionAmount(BigDecimal duplicateTransactionAmount) {
        this.duplicateTransactionAmount = duplicateTransactionAmount;
    }

    public String getDuplicateTransactionPayeeName() {
        return duplicateTransactionPayeeName;
    }

    public void setDuplicateTransactionPayeeName(String duplicateTransactionPayeeName) {
        this.duplicateTransactionPayeeName = duplicateTransactionPayeeName;
    }

    public boolean isDuplicateConfirmed() {
        return duplicateConfirmed;
    }

    public void setDuplicateConfirmed(boolean duplicateConfirmed) {
        this.duplicateConfirmed = duplicateConfirmed;
    }

    public boolean isDuplicateRejected() {
        return duplicateRejected;
    }

    public void setDuplicateRejected(boolean duplicateRejected) {
        this.duplicateRejected = duplicateRejected;
    }
}
