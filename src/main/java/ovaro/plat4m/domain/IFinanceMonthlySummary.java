package ovaro.plat4m.domain;

import java.math.BigDecimal;

public interface IFinanceMonthlySummary {
    public String getAccountId();

    public void setAccountId(String accountId);

    public String getAccountName();

    public void setAccountName(String accountName);

    public int getY();

    public void setY(int y);

    public int getM();

    public void setM(int m);

    public BigDecimal getRunningBalance();

    public void setRunningBalance(BigDecimal runningBalance);

    public Double getRate();

    public void setRate(Double rate);
    // String accountId;
    // int y;
    // int m;
    // BigDecimal runningBalance;
    // public String getAccountId() {
    //     return accountId;
    // }
    // public void setAccountId(String accountId) {
    //     this.accountId = accountId;
    // }
    // public int getY() {
    //     return y;
    // }
    // public void setY(int y) {
    //     this.y = y;
    // }
    // public int getM() {
    //     return m;
    // }
    // public void setM(int m) {
    //     this.m = m;
    // }
    // public BigDecimal getRunningBalance() {
    //     return runningBalance;
    // }
    // public void setRunningBalance(BigDecimal runningBalance) {
    //     this.runningBalance = runningBalance;
    // }

}
