package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import ovaro.plat4m.domain.FinanceSecurityType;
import ovaro.plat4m.domain.FinanceUserSecurity;

public class FinanceSecurityHoldingDTO {

    private LocalDate date;
    private String id;
    private String accountId;
    private String accountName;
    private String name;
    private String symbol;
    private String userSymbol;
    private String masterGuid;
    private String comment;
    private String exchangeName;
    // private FinanceCurrency currency;
    private String currencyCode;
    private FinanceUserSecurity linked;
    private Integer type;
    private String typeName;
    private double quantity = 0;
    private BigDecimal price;
    private BigDecimal value;
    private ZonedDateTime priceDateTime;
    private Double fxRateToLocal;
    private ZonedDateTime fxDateTime;

    private String sector;
    private String industry;

    public FinanceSecurityHoldingDTO() {}

    public FinanceSecurityHoldingDTO(FinanceUserSecurity security) {
        name = security.getSecurity().getName();
        symbol = security.getSymbol();
        comment = security.getComment();
        exchangeName = security.getSecurity().getExchangeName();
        if (security.getSecurity().getCurrencyCode() != null) {
            currencyCode = security.getSecurity().getCurrencyCode();
        }
        linked = security.getLinked();
        type = security.getType();
        id = security.getId().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getMasterGuid() {
        return masterGuid;
    }

    public void setMasterGuid(String masterGuid) {
        this.masterGuid = masterGuid;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public FinanceUserSecurity getLinked() {
        return linked;
    }

    public void setUserLinked(FinanceUserSecurity linked) {
        this.linked = linked;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
        FinanceSecurityType t = FinanceSecurityType.toSecurityType(type);
        this.setTypeName(t.name());
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public ZonedDateTime getPriceDateTime() {
        return priceDateTime;
    }

    public void setPriceDateTime(ZonedDateTime priceDateTime) {
        this.priceDateTime = priceDateTime;
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

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public void setLinked(FinanceUserSecurity linked) {
        this.linked = linked;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getUserSymbol() {
        return userSymbol;
    }

    public void setUserSymbol(String userSymbol) {
        this.userSymbol = userSymbol;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
