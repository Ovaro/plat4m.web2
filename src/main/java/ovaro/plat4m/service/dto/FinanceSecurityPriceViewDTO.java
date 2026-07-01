package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class FinanceSecurityPriceViewDTO {

    private UUID id;
    private String symbol;
    private LocalDate date;
    private BigDecimal price;
    private String comment;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

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
