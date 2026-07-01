package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceSecurityPriceRefreshItemDTO {

    private String userSecurityId;
    private String symbol;
    private String requestedSymbol;
    private String requestedExchangeMic;
    private String currencyCode;
    private boolean selected = true;
    private boolean applied;
    private boolean canApply;
    private String appliedSource;
    private String status;
    private String message;
    private LocalDate priceDate;
    private BigDecimal price;
    private LocalDate aiPriceDate;
    private BigDecimal aiPrice;
    private String aiMessage;
    private LocalDate twelveDataPriceDate;
    private BigDecimal twelveDataPrice;
    private String twelveDataMessage;
    private BigDecimal previousPrice;
    private BigDecimal priceDeltaValue;
    private BigDecimal priceDeltaPercent;

    public String getUserSecurityId() {
        return userSecurityId;
    }

    public void setUserSecurityId(String userSecurityId) {
        this.userSecurityId = userSecurityId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public boolean isCanApply() {
        return canApply;
    }

    public void setCanApply(boolean canApply) {
        this.canApply = canApply;
    }

    public String getAppliedSource() {
        return appliedSource;
    }

    public void setAppliedSource(String appliedSource) {
        this.appliedSource = appliedSource;
    }

    public String getRequestedSymbol() {
        return requestedSymbol;
    }

    public void setRequestedSymbol(String requestedSymbol) {
        this.requestedSymbol = requestedSymbol;
    }

    public String getRequestedExchangeMic() {
        return requestedExchangeMic;
    }

    public void setRequestedExchangeMic(String requestedExchangeMic) {
        this.requestedExchangeMic = requestedExchangeMic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getAiPriceDate() {
        return aiPriceDate;
    }

    public void setAiPriceDate(LocalDate aiPriceDate) {
        this.aiPriceDate = aiPriceDate;
    }

    public BigDecimal getAiPrice() {
        return aiPrice;
    }

    public void setAiPrice(BigDecimal aiPrice) {
        this.aiPrice = aiPrice;
    }

    public String getAiMessage() {
        return aiMessage;
    }

    public void setAiMessage(String aiMessage) {
        this.aiMessage = aiMessage;
    }

    public LocalDate getTwelveDataPriceDate() {
        return twelveDataPriceDate;
    }

    public void setTwelveDataPriceDate(LocalDate twelveDataPriceDate) {
        this.twelveDataPriceDate = twelveDataPriceDate;
    }

    public BigDecimal getTwelveDataPrice() {
        return twelveDataPrice;
    }

    public void setTwelveDataPrice(BigDecimal twelveDataPrice) {
        this.twelveDataPrice = twelveDataPrice;
    }

    public String getTwelveDataMessage() {
        return twelveDataMessage;
    }

    public void setTwelveDataMessage(String twelveDataMessage) {
        this.twelveDataMessage = twelveDataMessage;
    }

    public BigDecimal getPreviousPrice() {
        return previousPrice;
    }

    public void setPreviousPrice(BigDecimal previousPrice) {
        this.previousPrice = previousPrice;
    }

    public BigDecimal getPriceDeltaValue() {
        return priceDeltaValue;
    }

    public void setPriceDeltaValue(BigDecimal priceDeltaValue) {
        this.priceDeltaValue = priceDeltaValue;
    }

    public BigDecimal getPriceDeltaPercent() {
        return priceDeltaPercent;
    }

    public void setPriceDeltaPercent(BigDecimal priceDeltaPercent) {
        this.priceDeltaPercent = priceDeltaPercent;
    }
}
