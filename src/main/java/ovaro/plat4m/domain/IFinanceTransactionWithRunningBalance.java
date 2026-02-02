package ovaro.plat4m.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IFinanceTransactionWithRunningBalance {
    String getPayeeId();
    void setPayeeId(String payeeId);
    String getPayeeName();
    void setPayeeName(String payeeName);

    String getId();
    void setId(String id);
    BigDecimal getAmount();
    void setAmount(BigDecimal amount);
    Integer getStatusFlag();
    void setStatusFlag(Integer statusFlag);
    LocalDate getDate();
    void setDate(LocalDate date);
    Boolean getRecurring();
    void setRecurring(Boolean recurring);

    String getCategoryId();
    void setCategoryId(String categoryId);
    String getCategoryName();
    void setCategoryName(String categoryName);

    String getParentCategoryId();
    void setParentCategoryId(String categoryId);
    String getParentCategoryName();
    void setParentCategoryName(String categoryName);

    String getTransferredAccountId();
    void setTransferredAccountId(String transferredAccountId);
    String getSecurityId();
    void setSecurityId(String securityId);
    Boolean getInvestment();
    void setInvestment(Boolean investment);
    Double getQuantity();
    void setQuantity(Double quantity);
    Double getPrice();
    void setPrice(Double price);
    Boolean getTransfer();
    void setTransfer(Boolean transfer);
    Boolean getCleared();
    void setCleared(Boolean cleared);
    Boolean getReconciled();
    void setReconciled(Boolean reconciled);
    String getMemo();
    void setMemo(String memo);

    String getMasterGuid();
    void setMasterGuid(String masterGuid);
    String getAccountId();
    void setAccountId(String accountId);
    Boolean getTransferTo();
    void setTransferTo(Boolean transferTo);
    Boolean getSplitParent();
    void setSplitParent(Boolean splitParent);
    Boolean getSplitChild();
    void setSplitChild(Boolean splitChild);
    Boolean getVoided();
    void setVoided(Boolean voided);
    String getStatementId();
    void setStatementId(String statementId);
    Integer getNumber();
    void setNumber(Integer number);

    BigDecimal getRunningBalance();
    void setRunningBalance(BigDecimal runningBalance);

    Integer getInvestmentActivityTypeId();
    void setInvestmentActivityTypeId(Integer investmentActivityType);

    String getInvestmentActivityType();
    void setInvestmentActivityType(String investmentActivityType);
}
