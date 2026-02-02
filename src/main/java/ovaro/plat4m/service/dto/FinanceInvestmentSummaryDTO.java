package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class FinanceInvestmentSummaryDTO {

    String userSecurityId;
    Double quantity;
    BigDecimal price;

    ZonedDateTime priceDateTime;
    BigDecimal totalCapitalInvested = BigDecimal.ZERO;
    BigDecimal totalCapitalGain = BigDecimal.ZERO;
    BigDecimal totalCurrencyGain = BigDecimal.ZERO;
    BigDecimal totalIncome = BigDecimal.ZERO;
    BigDecimal totalReturn = BigDecimal.ZERO;
    BigDecimal totalFees = BigDecimal.ZERO;
    BigDecimal marketValue = BigDecimal.ZERO;

    Double totalReturnCAGR;
    Double totalReturnPC;

    Double totalCapitalGainCAGR;
    Double totalCapitalGainPC;

    Double totalCurrencyGainCAGR;
    Double totalCurrencyGainPC;

    Double totalIncomePC;
    Double totalIncomeCAGR;

    Double modDietzPC;
    Double modDietzPCpa;

    String currencyCode;

    Double ayi;

    private Double fxRateToLocal;
    private ZonedDateTime fxDateTime;

    public BigDecimal getValueInBaseCurrency() {
        if (price != null && quantity != null) {
            double val = getPrice().doubleValue() * getQuantity();
            if (getFxRateToLocal() != null) {
                val = val * getFxRateToLocal();
            }
            return new BigDecimal(val);
        }
        return null;
    }

    public FinanceInvestmentSummaryDTO(String userSecurityId) {
        this.userSecurityId = userSecurityId;
    }

    public FinanceInvestmentSummaryDTO() {}

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Double getFxRateToLocal() {
        return fxRateToLocal;
    }

    public void setFxRateToLocal(Double fxRateToLocal) {
        this.fxRateToLocal = fxRateToLocal;
    }

    public ZonedDateTime getFxDateTime() {
        return fxDateTime;
    }

    public void setFxDateTime(ZonedDateTime fxDateTime) {
        this.fxDateTime = fxDateTime;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public ZonedDateTime getPriceDateTime() {
        return priceDateTime;
    }

    public void setPriceDateTime(ZonedDateTime priceDateTime) {
        this.priceDateTime = priceDateTime;
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

    public String getUserSecurityId() {
        return userSecurityId;
    }

    public void setUserSecurityId(String userSecurityId) {
        this.userSecurityId = userSecurityId;
    }

    public Double getModDietzPC() {
        return modDietzPC;
    }

    public void setModDietzPC(Double modDietzPC) {
        this.modDietzPC = modDietzPC;
    }

    public Double getModDietzPCpa() {
        return modDietzPCpa;
    }

    public void setModDietzPCpa(Double modDietzPCpa) {
        this.modDietzPCpa = modDietzPCpa;
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

    public Double getAyi() {
        return ayi;
    }

    public void setAyi(Double ayi) {
        this.ayi = ayi;
    }
}
