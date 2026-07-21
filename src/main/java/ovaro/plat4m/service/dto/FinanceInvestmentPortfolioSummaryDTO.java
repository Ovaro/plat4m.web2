package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FinanceInvestmentPortfolioSummaryDTO extends FinanceInvestmentSnapshotDetails {

    List<FinanceInvestmentSnapshotDetails> summaries;

    BigDecimal totalValue;
    Double totalAyi;
    BigDecimal cashValue;
    BigDecimal cashInterest;
    Double cashReturnPercent;
    BigDecimal expectedReturnCagr;

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

    public BigDecimal getCashValue() {
        return cashValue;
    }

    public void setCashValue(BigDecimal cashValue) {
        this.cashValue = cashValue;
    }

    public BigDecimal getCashInterest() {
        return cashInterest;
    }

    public void setCashInterest(BigDecimal cashInterest) {
        this.cashInterest = cashInterest;
    }

    public Double getCashReturnPercent() {
        return cashReturnPercent;
    }

    public void setCashReturnPercent(Double cashReturnPercent) {
        this.cashReturnPercent = cashReturnPercent;
    }

    public BigDecimal getExpectedReturnCagr() {
        return expectedReturnCagr;
    }

    public void setExpectedReturnCagr(BigDecimal expectedReturnCagr) {
        this.expectedReturnCagr = expectedReturnCagr;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
