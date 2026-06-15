package ovaro.plat4m.service.dto;

import java.time.LocalDate;

public class FinanceFXImportResultDTO {

    private LocalDate date;
    private String baseCurrency;
    private int updated;

    public FinanceFXImportResultDTO() {}

    public FinanceFXImportResultDTO(LocalDate date, String baseCurrency, int updated) {
        this.date = date;
        this.baseCurrency = baseCurrency;
        this.updated = updated;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }
}
