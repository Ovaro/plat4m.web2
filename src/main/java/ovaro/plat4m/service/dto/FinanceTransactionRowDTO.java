package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ovaro.plat4m.domain.IFinanceTransactionWithRunningBalance;

public class FinanceTransactionRowDTO {

    private String accountId;
    private BigDecimal amount;
    private BigDecimal principalAmount;
    private String categoryId;
    private String categoryName;
    private Boolean cleared;
    private LocalDate date;
    private String id;
    private Boolean investment;
    private String investmentActivityType;
    private Integer investmentActivityTypeId;
    private String masterGuid;
    private String memo;
    private Integer number;
    private String parentCategoryId;
    private String parentCategoryName;
    private String payeeId;
    private String payeeName;
    private Double price;
    private Double quantity;
    private Boolean reconciled;
    private Boolean recurring;
    private BigDecimal runningBalance;
    private String securityId;
    private String securityName;
    private Boolean splitChild;
    private Boolean splitParent;
    private String statementId;
    private String importId;
    private Integer statusFlag;
    private Boolean transfer;
    private Boolean transferTo;
    private String transferredAccountId;
    private Boolean voided;
    private String whoId;
    private String whoName;
    private List<String> tags;
    private String tagsDisplay;

    public FinanceTransactionRowDTO() {}

    public FinanceTransactionRowDTO(IFinanceTransactionWithRunningBalance transaction) {
        this.accountId = transaction.getAccountId();
        this.amount = transaction.getAmount();
        this.principalAmount = transaction.getPrincipalAmount();
        this.categoryId = transaction.getCategoryId();
        this.categoryName = transaction.getCategoryName();
        this.cleared = transaction.getCleared();
        this.date = transaction.getDate();
        this.id = transaction.getId();
        this.investment = transaction.getInvestment();
        this.investmentActivityType = transaction.getInvestmentActivityType();
        this.investmentActivityTypeId = transaction.getInvestmentActivityTypeId();
        this.masterGuid = transaction.getMasterGuid();
        this.memo = transaction.getMemo();
        this.number = transaction.getNumber();
        this.parentCategoryId = transaction.getParentCategoryId();
        this.parentCategoryName = transaction.getParentCategoryName();
        this.payeeId = transaction.getPayeeId();
        this.payeeName = transaction.getPayeeName();
        this.price = transaction.getPrice();
        this.quantity = transaction.getQuantity();
        this.reconciled = transaction.getReconciled();
        this.recurring = transaction.getRecurring();
        this.runningBalance = transaction.getRunningBalance();
        this.securityId = transaction.getSecurityId();
        this.splitChild = transaction.getSplitChild();
        this.splitParent = transaction.getSplitParent();
        this.statementId = transaction.getStatementId();
        this.statusFlag = transaction.getStatusFlag();
        this.transfer = transaction.getTransfer();
        this.transferTo = transaction.getTransferTo();
        this.transferredAccountId = transaction.getTransferredAccountId();
        this.voided = transaction.getVoided();
        this.whoId = transaction.getWhoId();
        this.whoName = transaction.getWhoName();
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

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

    public Boolean getCleared() {
        return cleared;
    }

    public void setCleared(Boolean cleared) {
        this.cleared = cleared;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getInvestment() {
        return investment;
    }

    public void setInvestment(Boolean investment) {
        this.investment = investment;
    }

    public String getInvestmentActivityType() {
        return investmentActivityType;
    }

    public void setInvestmentActivityType(String investmentActivityType) {
        this.investmentActivityType = investmentActivityType;
    }

    public Integer getInvestmentActivityTypeId() {
        return investmentActivityTypeId;
    }

    public void setInvestmentActivityTypeId(Integer investmentActivityTypeId) {
        this.investmentActivityTypeId = investmentActivityTypeId;
    }

    public String getMasterGuid() {
        return masterGuid;
    }

    public void setMasterGuid(String masterGuid) {
        this.masterGuid = masterGuid;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(String parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }

    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public void setParentCategoryName(String parentCategoryName) {
        this.parentCategoryName = parentCategoryName;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Boolean getReconciled() {
        return reconciled;
    }

    public void setReconciled(Boolean reconciled) {
        this.reconciled = reconciled;
    }

    public Boolean getRecurring() {
        return recurring;
    }

    public void setRecurring(Boolean recurring) {
        this.recurring = recurring;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance;
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

    public Boolean getSplitChild() {
        return splitChild;
    }

    public void setSplitChild(Boolean splitChild) {
        this.splitChild = splitChild;
    }

    public Boolean getSplitParent() {
        return splitParent;
    }

    public void setSplitParent(Boolean splitParent) {
        this.splitParent = splitParent;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getImportId() {
        return importId;
    }

    public void setImportId(String importId) {
        this.importId = importId;
    }

    public Integer getStatusFlag() {
        return statusFlag;
    }

    public void setStatusFlag(Integer statusFlag) {
        this.statusFlag = statusFlag;
    }

    public Boolean getTransfer() {
        return transfer;
    }

    public void setTransfer(Boolean transfer) {
        this.transfer = transfer;
    }

    public Boolean getTransferTo() {
        return transferTo;
    }

    public void setTransferTo(Boolean transferTo) {
        this.transferTo = transferTo;
    }

    public String getTransferredAccountId() {
        return transferredAccountId;
    }

    public void setTransferredAccountId(String transferredAccountId) {
        this.transferredAccountId = transferredAccountId;
    }

    public Boolean getVoided() {
        return voided;
    }

    public void setVoided(Boolean voided) {
        this.voided = voided;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getTagsDisplay() {
        return tagsDisplay;
    }

    public void setTagsDisplay(String tagsDisplay) {
        this.tagsDisplay = tagsDisplay;
    }
}
