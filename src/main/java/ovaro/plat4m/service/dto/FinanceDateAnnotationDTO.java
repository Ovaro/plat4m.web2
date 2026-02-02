package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceDateAnnotationDTO {

    LocalDate date;
    String annotation;
    String type; // BUY, SELL, ETC
    Double quantity;
    BigDecimal price;
    BigDecimal totalValue = BigDecimal.ZERO;

    public FinanceDateAnnotationDTO(LocalDate date) {
        this.date = date;
    }

    public FinanceDateAnnotationDTO(LocalDate date, String annotation, String type) {
        this.date = date;
        this.annotation = annotation;
        this.type = type;
    }

    public FinanceDateAnnotationDTO() {}

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
}
