package ovaro.plat4m.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FinancePortfolioTradeDTO {

    private String id;
    private LocalDate date;
    private String type;
    private String accountId;
    private String accountName;
    private String securityId;
    private String symbol;
    private String name;
    private String currencyCode;
    private Boolean ignoredForRollup;
    private String ignoredForRollupReason;
    private Double quantity;
    private Double buyPrice;
    private Double buyFxRate;
    private Double currentPrice;
    private Double currentFxRate;
    private Double deltaAmountWithFx;
    private Double deltaPercentWithFx;
    private Double deltaAmountWithoutFx;
    private Double deltaPercentWithoutFx;
    private Boolean sold;
    private LocalDate sellDate;
    private Double sellPrice;
    private Double sellQuantity;
    private Double sellFxRate;
    private Double openQuantity;
    private Double realizedDeltaAmountWithFx;
    private Double realizedDeltaPercentWithFx;
    private Double realizedDeltaAmountWithoutFx;
    private Double realizedDeltaPercentWithoutFx;
    private Double holdingYears;
    private Double dividendIncomeAmountWithFx;
    private Double dividendIncomeAmountWithoutFx;
    private List<DividendIncomeDTO> dividends = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Boolean getIgnoredForRollup() {
        return ignoredForRollup;
    }

    public void setIgnoredForRollup(Boolean ignoredForRollup) {
        this.ignoredForRollup = ignoredForRollup;
    }

    public String getIgnoredForRollupReason() {
        return ignoredForRollupReason;
    }

    public void setIgnoredForRollupReason(String ignoredForRollupReason) {
        this.ignoredForRollupReason = ignoredForRollupReason;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(Double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public Double getBuyFxRate() {
        return buyFxRate;
    }

    public void setBuyFxRate(Double buyFxRate) {
        this.buyFxRate = buyFxRate;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Double getCurrentFxRate() {
        return currentFxRate;
    }

    public void setCurrentFxRate(Double currentFxRate) {
        this.currentFxRate = currentFxRate;
    }

    public Double getDeltaAmountWithFx() {
        return deltaAmountWithFx;
    }

    public void setDeltaAmountWithFx(Double deltaAmountWithFx) {
        this.deltaAmountWithFx = deltaAmountWithFx;
    }

    public Double getDeltaPercentWithFx() {
        return deltaPercentWithFx;
    }

    public void setDeltaPercentWithFx(Double deltaPercentWithFx) {
        this.deltaPercentWithFx = deltaPercentWithFx;
    }

    public Double getDeltaAmountWithoutFx() {
        return deltaAmountWithoutFx;
    }

    public void setDeltaAmountWithoutFx(Double deltaAmountWithoutFx) {
        this.deltaAmountWithoutFx = deltaAmountWithoutFx;
    }

    public Double getDeltaPercentWithoutFx() {
        return deltaPercentWithoutFx;
    }

    public void setDeltaPercentWithoutFx(Double deltaPercentWithoutFx) {
        this.deltaPercentWithoutFx = deltaPercentWithoutFx;
    }

    public Boolean getSold() {
        return sold;
    }

    public void setSold(Boolean sold) {
        this.sold = sold;
    }

    public LocalDate getSellDate() {
        return sellDate;
    }

    public void setSellDate(LocalDate sellDate) {
        this.sellDate = sellDate;
    }

    public Double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(Double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public Double getSellQuantity() {
        return sellQuantity;
    }

    public void setSellQuantity(Double sellQuantity) {
        this.sellQuantity = sellQuantity;
    }

    public Double getSellFxRate() {
        return sellFxRate;
    }

    public void setSellFxRate(Double sellFxRate) {
        this.sellFxRate = sellFxRate;
    }

    public Double getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Double openQuantity) {
        this.openQuantity = openQuantity;
    }

    public Double getRealizedDeltaAmountWithFx() {
        return realizedDeltaAmountWithFx;
    }

    public void setRealizedDeltaAmountWithFx(Double realizedDeltaAmountWithFx) {
        this.realizedDeltaAmountWithFx = realizedDeltaAmountWithFx;
    }

    public Double getRealizedDeltaPercentWithFx() {
        return realizedDeltaPercentWithFx;
    }

    public void setRealizedDeltaPercentWithFx(Double realizedDeltaPercentWithFx) {
        this.realizedDeltaPercentWithFx = realizedDeltaPercentWithFx;
    }

    public Double getRealizedDeltaAmountWithoutFx() {
        return realizedDeltaAmountWithoutFx;
    }

    public void setRealizedDeltaAmountWithoutFx(Double realizedDeltaAmountWithoutFx) {
        this.realizedDeltaAmountWithoutFx = realizedDeltaAmountWithoutFx;
    }

    public Double getRealizedDeltaPercentWithoutFx() {
        return realizedDeltaPercentWithoutFx;
    }

    public void setRealizedDeltaPercentWithoutFx(Double realizedDeltaPercentWithoutFx) {
        this.realizedDeltaPercentWithoutFx = realizedDeltaPercentWithoutFx;
    }

    public Double getHoldingYears() {
        return holdingYears;
    }

    public void setHoldingYears(Double holdingYears) {
        this.holdingYears = holdingYears;
    }

    public Double getDividendIncomeAmountWithFx() {
        return dividendIncomeAmountWithFx;
    }

    public void setDividendIncomeAmountWithFx(Double dividendIncomeAmountWithFx) {
        this.dividendIncomeAmountWithFx = dividendIncomeAmountWithFx;
    }

    public Double getDividendIncomeAmountWithoutFx() {
        return dividendIncomeAmountWithoutFx;
    }

    public void setDividendIncomeAmountWithoutFx(Double dividendIncomeAmountWithoutFx) {
        this.dividendIncomeAmountWithoutFx = dividendIncomeAmountWithoutFx;
    }

    public List<DividendIncomeDTO> getDividends() {
        return dividends;
    }

    public void setDividends(List<DividendIncomeDTO> dividends) {
        this.dividends = dividends;
    }

    public static class DividendIncomeDTO {

        private String id;
        private LocalDate date;
        private String type;
        private Double amountWithFx;
        private Double amountWithoutFx;
        private String baseCurrencyCode;
        private String sourceCurrencyCode;
        private Double sourceAmount;
        private Double fxRate;
        private Double transactionAmountWithFx;
        private Double transactionAmountWithoutFx;
        private Double transactionSourceAmount;
        private Double allocationRatio;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Double getAmountWithFx() {
            return amountWithFx;
        }

        public void setAmountWithFx(Double amountWithFx) {
            this.amountWithFx = amountWithFx;
        }

        public Double getAmountWithoutFx() {
            return amountWithoutFx;
        }

        public void setAmountWithoutFx(Double amountWithoutFx) {
            this.amountWithoutFx = amountWithoutFx;
        }

        public String getBaseCurrencyCode() {
            return baseCurrencyCode;
        }

        public void setBaseCurrencyCode(String baseCurrencyCode) {
            this.baseCurrencyCode = baseCurrencyCode;
        }

        public String getSourceCurrencyCode() {
            return sourceCurrencyCode;
        }

        public void setSourceCurrencyCode(String sourceCurrencyCode) {
            this.sourceCurrencyCode = sourceCurrencyCode;
        }

        public Double getSourceAmount() {
            return sourceAmount;
        }

        public void setSourceAmount(Double sourceAmount) {
            this.sourceAmount = sourceAmount;
        }

        public Double getFxRate() {
            return fxRate;
        }

        public void setFxRate(Double fxRate) {
            this.fxRate = fxRate;
        }

        public Double getTransactionAmountWithFx() {
            return transactionAmountWithFx;
        }

        public void setTransactionAmountWithFx(Double transactionAmountWithFx) {
            this.transactionAmountWithFx = transactionAmountWithFx;
        }

        public Double getTransactionAmountWithoutFx() {
            return transactionAmountWithoutFx;
        }

        public void setTransactionAmountWithoutFx(Double transactionAmountWithoutFx) {
            this.transactionAmountWithoutFx = transactionAmountWithoutFx;
        }

        public Double getTransactionSourceAmount() {
            return transactionSourceAmount;
        }

        public void setTransactionSourceAmount(Double transactionSourceAmount) {
            this.transactionSourceAmount = transactionSourceAmount;
        }

        public Double getAllocationRatio() {
            return allocationRatio;
        }

        public void setAllocationRatio(Double allocationRatio) {
            this.allocationRatio = allocationRatio;
        }
    }
}
