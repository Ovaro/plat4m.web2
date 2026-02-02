package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FinanceInvestmentPortfolioSummaryDTO extends FinanceInvestmentSnapshotDetails {

    List<FinanceInvestmentSnapshotDetails> summaries;

    BigDecimal totalValue;
    Double totalAyi;

    LocalDate date;

    public FinanceInvestmentPortfolioSummaryDTO() {}

    public List<FinanceInvestmentSnapshotDetails> getSummaries() {
        return summaries;
    }

    public void setSummaries(List<FinanceInvestmentSnapshotDetails> summaries) {
        this.summaries = summaries;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public Double getTotalAyi() {
        return totalAyi;
    }

    public void setTotalAyi(Double totalAyi) {
        this.totalAyi = totalAyi;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
