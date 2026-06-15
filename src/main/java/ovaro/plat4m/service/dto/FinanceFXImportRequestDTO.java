package ovaro.plat4m.service.dto;

import java.time.LocalDate;
import java.util.List;

public class FinanceFXImportRequestDTO {

    private String baseCurrency;
    private List<String> quoteCurrencies;
    private LocalDate date;

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public List<String> getQuoteCurrencies() {
        return quoteCurrencies;
    }

    public void setQuoteCurrencies(List<String> quoteCurrencies) {
        this.quoteCurrencies = quoteCurrencies;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
