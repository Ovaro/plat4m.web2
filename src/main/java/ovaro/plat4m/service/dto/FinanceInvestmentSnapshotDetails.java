package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceInvestmentSnapshotDetails {

    // User Security ID
    private String userSecurityId;
    private boolean ignoredForRollup = false;
    private String ignoredForRollupReason;
    // Price & Quantity
    private BigDecimal price;
    private LocalDate priceDate;
    private Double quantity;
    private boolean estimatedPrice = false;

    // FX
    private String currencyIsoCode;
    private Double fxToLocal;
    private LocalDate fxDate;
    // Investment Metrics
    private BigDecimal totalCapitalInvested;
    private BigDecimal totalCapitalGain;
    private BigDecimal totalCurrencyGain;
    private BigDecimal totalIncome;
    private BigDecimal totalReturn;
    private BigDecimal totalFees;
    // Investment Metrics - Percentages
    private Double totalReturnCAGR;
    private Double totalReturnPC;
    private Double totalCapitalGainCAGR;
    private Double totalCapitalGainPC;
    private Double totalCurrencyGainCAGR;
    private Double totalCurrencyGainPC;
    private Double totalIncomePC;
    private Double totalIncomeCAGR;

    public FinanceInvestmentSnapshotDetails() {}

    public FinanceInvestmentSnapshotDetails(BigDecimal price, Double quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public boolean isEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(boolean estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getFxToLocal() {
        return fxToLocal;
    }

    public void setFxToLocal(Double fxToLocal) {
        this.fxToLocal = fxToLocal;
    }

    public String getCurrencyIsoCode() {
        return currencyIsoCode;
    }

    public void setCurrencyIsoCode(String currencyIsoCode) {
        this.currencyIsoCode = currencyIsoCode;
    }

    public BigDecimal getTotalCapitalInvested() {
        return totalCapitalInvested;
    }

    public void setTotalCapitalInvested(BigDecimal totalCapitalInvested) {
        this.totalCapitalInvested = totalCapitalInvested;
    }

    public BigDecimal getTotalCapitalGain() {
        return totalCapitalGain;
    }

    public void setTotalCapitalGain(BigDecimal totalCapitalGain) {
        this.totalCapitalGain = totalCapitalGain;
    }

    public BigDecimal getTotalCurrencyGain() {
        return totalCurrencyGain;
    }

    public void setTotalCurrencyGain(BigDecimal totalCurrencyGain) {
        this.totalCurrencyGain = totalCurrencyGain;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalReturn() {
        return totalReturn;
    }

    public void setTotalReturn(BigDecimal totalReturn) {
        this.totalReturn = totalReturn;
    }

    public BigDecimal getTotalFees() {
        return totalFees;
    }

    public void setTotalFees(BigDecimal totalFees) {
        this.totalFees = totalFees;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public LocalDate getFxDate() {
        return fxDate;
    }

    public void setFxDate(LocalDate fxDate) {
        this.fxDate = fxDate;
    }

    public Double getTotalReturnCAGR() {
        return totalReturnCAGR;
    }

    public void setTotalReturnCAGR(Double totalReturnCAGR) {
        this.totalReturnCAGR = totalReturnCAGR;
    }

    public Double getTotalReturnPC() {
        return totalReturnPC;
    }

    public void setTotalReturnPC(Double totalReturnPC) {
        this.totalReturnPC = totalReturnPC;
    }

    public Double getTotalCapitalGainCAGR() {
        return totalCapitalGainCAGR;
    }

    public void setTotalCapitalGainCAGR(Double totalCapitalGainCAGR) {
        this.totalCapitalGainCAGR = totalCapitalGainCAGR;
    }

    public Double getTotalCapitalGainPC() {
        return totalCapitalGainPC;
    }

    public void setTotalCapitalGainPC(Double totalCapitalGainPC) {
        this.totalCapitalGainPC = totalCapitalGainPC;
    }

    public Double getTotalCurrencyGainCAGR() {
        return totalCurrencyGainCAGR;
    }

    public void setTotalCurrencyGainCAGR(Double totalCurrencyGainCAGR) {
        this.totalCurrencyGainCAGR = totalCurrencyGainCAGR;
    }

    public Double getTotalCurrencyGainPC() {
        return totalCurrencyGainPC;
    }

    public void setTotalCurrencyGainPC(Double totalCurrencyGainPC) {
        this.totalCurrencyGainPC = totalCurrencyGainPC;
    }

    public Double getTotalIncomePC() {
        return totalIncomePC;
    }

    public void setTotalIncomePC(Double totalIncomePC) {
        this.totalIncomePC = totalIncomePC;
    }

    public Double getTotalIncomeCAGR() {
        return totalIncomeCAGR;
    }

    public void setTotalIncomeCAGR(Double totalIncomeCAGR) {
        this.totalIncomeCAGR = totalIncomeCAGR;
    }

    public String getUserSecurityId() {
        return userSecurityId;
    }

    public void setUserSecurityId(String userSecurityId) {
        this.userSecurityId = userSecurityId;
    }

    public boolean isIgnoredForRollup() {
        return ignoredForRollup;
    }

    public void setIgnoredForRollup(boolean ignoredForRollup) {
        this.ignoredForRollup = ignoredForRollup;
    }

    public String getIgnoredForRollupReason() {
        return ignoredForRollupReason;
    }

    public void setIgnoredForRollupReason(String ignoredForRollupReason) {
        this.ignoredForRollupReason = ignoredForRollupReason;
    }
}
