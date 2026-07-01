package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceSecurityPriceUpdateDTO {

    private LocalDate date;
    private BigDecimal price;
    private String comment;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
