package ovaro.plat4m.service.dto;

import java.math.BigDecimal;

public class FinanceDemoDataAccountSpecDTO {

    private String accountType;
    private Boolean include;
    private Integer accountCount;
    private Integer years;
    private String density;
    private BigDecimal startingValue;
    private BigDecimal targetValue;
    private Integer stockCount;
    private String tradingProfile;
    private String hints;

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Boolean getInclude() {
        return include;
    }

    public void setInclude(Boolean include) {
        this.include = include;
    }

    public Integer getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(Integer accountCount) {
        this.accountCount = accountCount;
    }

    public Integer getYears() {
        return years;
    }

    public void setYears(Integer years) {
        this.years = years;
    }

    public String getDensity() {
        return density;
    }

    public void setDensity(String density) {
        this.density = density;
    }

    public BigDecimal getStartingValue() {
        return startingValue;
    }

    public void setStartingValue(BigDecimal startingValue) {
        this.startingValue = startingValue;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public String getTradingProfile() {
        return tradingProfile;
    }

    public void setTradingProfile(String tradingProfile) {
        this.tradingProfile = tradingProfile;
    }

    public String getHints() {
        return hints;
    }

    public void setHints(String hints) {
        this.hints = hints;
    }
}
