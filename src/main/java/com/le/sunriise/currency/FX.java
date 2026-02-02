package com.le.sunriise.currency;

import java.time.ZonedDateTime;
import java.util.Date;

public class FX {

    Integer fromCurrencyId;
    Integer toCurrencyId;
    Double rate;
    Date date;

    public FX(Integer fromCurrencyId, Integer toCurrencyId, Double rate, Date date) {
        this.fromCurrencyId = fromCurrencyId;
        this.toCurrencyId = toCurrencyId;
        this.rate = rate;
        this.date = date;
    }

    public Integer getFromCurrencyId() {
        return fromCurrencyId;
    }

    public void setFromCurrencyId(Integer fromCurrencyId) {
        this.fromCurrencyId = fromCurrencyId;
    }

    public Integer getToCurrencyId() {
        return toCurrencyId;
    }

    public void setToCurrencyId(Integer toCurrencyId) {
        this.toCurrencyId = toCurrencyId;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
