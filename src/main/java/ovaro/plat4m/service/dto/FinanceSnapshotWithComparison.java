package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceSnapshotWithComparison extends FinanceSnapshot {

    private LocalDate comparisonDate;
    private BigDecimal comparisonValue;
    private Double comparisonFxToLocal;
    private LocalDate comparisonFxDate;
    private FinanceInvestmentSnapshotDetails comparisonInvestment;

    public FinanceSnapshotWithComparison(LocalDate date) {
        super(date);
    }

    public FinanceSnapshotWithComparison(LocalDate date, BigDecimal value) {
        super(date, value);
    }

    public LocalDate getComparisonDate() {
        return comparisonDate;
    }

    public void setComparisonDate(LocalDate comparisonDate) {
        this.comparisonDate = comparisonDate;
    }

    public BigDecimal getComparisonValue() {
        return comparisonValue;
    }

    public void setComparisonValue(BigDecimal comparisonValue) {
        this.comparisonValue = comparisonValue;
    }

    public Double getComparisonFxToLocal() {
        return comparisonFxToLocal;
    }

    public void setComparisonFxToLocal(Double comparisonFxToLocal) {
        this.comparisonFxToLocal = comparisonFxToLocal;
    }

    public LocalDate getComparisonFxDate() {
        return comparisonFxDate;
    }

    public void setComparisonFxDate(LocalDate comparisonFxDate) {
        this.comparisonFxDate = comparisonFxDate;
    }

    public FinanceInvestmentSnapshotDetails getComparisonInvestment() {
        return comparisonInvestment;
    }

    public void setComparisonInvestment(FinanceInvestmentSnapshotDetails comparisonInvestment) {
        this.comparisonInvestment = comparisonInvestment;
    }
}
