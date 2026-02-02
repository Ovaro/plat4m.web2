package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceSnapshot {

    // Base Data
    private String id;
    private LocalDate date;
    private BigDecimal value;
    // FX
    private String currencyIsoCode;
    private Double fxToLocal;
    private LocalDate fxDate;
    // Investment details (if relevant)
    private FinanceInvestmentSnapshotDetails investment;

    public FinanceSnapshot(LocalDate date) {
        this.date = date;
    }

    public FinanceSnapshot(LocalDate date, BigDecimal value) {
        this.date = date;
        this.value = value;
    }

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

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Double getFxToLocal() {
        return fxToLocal;
    }

    public void setFxToLocal(Double fxToLocal) {
        this.fxToLocal = fxToLocal;
    }

    public LocalDate getFxDate() {
        return fxDate;
    }

    public void setFxDate(LocalDate fxDate) {
        this.fxDate = fxDate;
    }

    public String getCurrencyIsoCode() {
        return currencyIsoCode;
    }

    public void setCurrencyIsoCode(String currencyIsoCode) {
        this.currencyIsoCode = currencyIsoCode;
    }

    public FinanceInvestmentSnapshotDetails getInvestment() {
        return investment;
    }

    public FinanceInvestmentSnapshotDetails checkAndGetInvestment() {
        if (this.investment == null) {
            this.investment = new FinanceInvestmentSnapshotDetails();
        }
        return this.investment;
    }

    public void setInvestment(FinanceInvestmentSnapshotDetails investment) {
        this.investment = investment;
    }
    // private BigDecimal price;
    // private Double quantity;

    //private String type; // BUY, SELL, ETC
    //private Map<String, Object> data;

    // public Map<String, Object> getData() {
    //     return data;
    // }
    // public void setData(Map<String, Object> data) {
    //     this.data = data;
    // }

    // public void addData(String key, Object value) {
    //     if(this.data == null) {
    //         this.data = new HashMap<String, Object>();
    //     }
    //     this.data.put(key, value);
    // }
    // public String getType() {
    //     return type;
    // }
    // public void setType(String type) {
    //     this.type = type;
    // }

}
