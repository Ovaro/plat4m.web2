package ovaro.plat4m.domain;

public interface FinanceSecurityInvestmentSummary {
    String getAccountId();
    void setAccountId(String account_id);

    String getAccountName();
    void setAccountName(String account_name);

    String getSecurityId();
    void setSecurityId(String security_id);

    String getName();
    void setName(String name);

    String getSymbol();
    void setSymbol(String symbol);

    String getUserSymbol();
    void setUserSymbol(String userSymbol);

    Double getAddSec();
    void setAddSec(Double addSec);

    Double getRemoveSec();
    void setRemoveSec(Double removeSec);

    String getCurrencyCode();
    void setCurrencyCode(String currencyCode);

    String getSector();
    void setSector(String sector);

    String getIndustry();
    void setIndustry(String industry);

    String getExchangeName();
    void setExchangeName(String exchangeName);

    String getExchangeMic();
    void setExchangeMic(String exchangeMic);

    Integer getSecurityType();
    void setSecurityType(Integer securityType);
}
