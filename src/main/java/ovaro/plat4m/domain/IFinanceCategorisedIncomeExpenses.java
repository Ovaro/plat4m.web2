package ovaro.plat4m.domain;

import java.math.BigDecimal;

public interface IFinanceCategorisedIncomeExpenses {
    BigDecimal getAmount();
    void setAmount(BigDecimal amount);

    String getCategoryId();
    void setCategoryId(String categoryId);
    String getCategoryName();
    void setCategoryName(String categoryName);

    String getParentCategoryId();
    void setParentCategoryId(String categoryId);
    String getParentCategoryName();
    void setParentCategoryName(String categoryName);

    String getGrandParentCategoryId();
    void setGrandParentCategoryId(String categoryId);
    String getGrandParentCategoryName();
    void setGrandParentCategoryName(String categoryName);

    String getTransferredAccountId();
    void setTransferredAccountId(String transferredAccountId);
}
