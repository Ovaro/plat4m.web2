package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import ovaro.plat4m.domain.IFinanceMonthlySummary;

public class FinanceSnapshotsDTO {

    LocalDate date;
    String accountName;
    String accountId;
    BigDecimal runningBalance;
    Double rateToBase;
    String currencyCode;

    public FinanceSnapshotsDTO() {}

    public FinanceSnapshotsDTO(String accountId, String accountName, int y, int m, BigDecimal runningBalance, Double rateToBase) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.runningBalance = runningBalance;
        this.rateToBase = rateToBase;
        this.date = LocalDate.of(y, m, 1).with(TemporalAdjusters.lastDayOfMonth());
    }

    public FinanceSnapshotsDTO(IFinanceMonthlySummary ifc) {
        this.accountId = ifc.getAccountId();
        this.accountName = ifc.getAccountName();
        this.runningBalance = ifc.getRunningBalance();
        this.rateToBase = ifc.getRate();
        this.date = LocalDate.of(ifc.getY(), ifc.getM(), 1).with(TemporalAdjusters.lastDayOfMonth());
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Double getRateToBase() {
        return rateToBase;
    }

    public void setRateToBase(Double rateToBase) {
        this.rateToBase = rateToBase;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
