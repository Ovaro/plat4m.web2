package ovaro.plat4m.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IFinanceSecurityPriceInPeriod {
    // public String getId();

    // public void setId(String id);

    public BigDecimal getPrice();

    public void setPrice(BigDecimal price);

    public LocalDateTime getDate();

    public void setDate(LocalDateTime date);

    // public ZonedDateTime getSerialDateTime();

    // public void setSerialDateTime(ZonedDateTime serialDateTime);

    // public String getComment();

    // public void setComment(String comment);

    // public BigDecimal getOpen();

    // public void setOpen(BigDecimal open);

    // public BigDecimal getClose();

    // public void setClose(BigDecimal close);

    // public BigDecimal getHigh();

    // public void setHigh(BigDecimal high);

    // public BigDecimal getLow();

    // public void setLow(BigDecimal low);

    // public Integer getVolume();

    // public void setVolume(Integer volume);

    // public BigDecimal getChange();

    // public void setChange(BigDecimal change);

    public String getSymbol();

    public void setSymbol(String symbol);
    // public LocalDate geMaxDate();

    // public void setMaxDate(LocalDate maxDate);
}
