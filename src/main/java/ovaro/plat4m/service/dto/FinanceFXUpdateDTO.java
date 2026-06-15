package ovaro.plat4m.service.dto;

import java.time.LocalDate;

public class FinanceFXUpdateDTO {

    private LocalDate date;
    private String fromIsoCode;
    private String toIsoCode;
    private Double rate;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getFromIsoCode() {
        return fromIsoCode;
    }

    public void setFromIsoCode(String fromIsoCode) {
        this.fromIsoCode = fromIsoCode;
    }

    public String getToIsoCode() {
        return toIsoCode;
    }

    public void setToIsoCode(String toIsoCode) {
        this.toIsoCode = toIsoCode;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }
}
