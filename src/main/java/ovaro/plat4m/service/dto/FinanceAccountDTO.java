package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import ovaro.plat4m.domain.FinanceAccount;

/**
 * A DTO representing a user, with only the public attributes.
 */
public class FinanceAccountDTO extends FinanceAccount {

    private BigDecimal balance;
    private String accountType;
    private Double fxRateToLocal;
    private ZonedDateTime fxDateTime;
    private String balanceWarning;
    private boolean naBalance = false;

    public boolean isNaBalance() {
        return naBalance;
    }

    public void setNaBalance(boolean naBalance) {
        this.naBalance = naBalance;
    }

    public FinanceAccountDTO(FinanceAccount account) {
        super(account);
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Double getFxRateToLocal() {
        return fxRateToLocal;
    }

    public void setFxRateToLocal(Double fxRateToLocal) {
        this.fxRateToLocal = fxRateToLocal;
    }

    public ZonedDateTime getFxDateTime() {
        return fxDateTime;
    }

    public void setFxDateTime(ZonedDateTime fxDateTime) {
        this.fxDateTime = fxDateTime;
    }

    @Override
    public String toString() {
        return "FinanceAccountDTO [balance=" + balance + "]";
    }

    public String getBalanceWarning() {
        return balanceWarning;
    }

    public void setBalanceWarning(String balanceWarning) {
        this.balanceWarning = balanceWarning;
    }
}
