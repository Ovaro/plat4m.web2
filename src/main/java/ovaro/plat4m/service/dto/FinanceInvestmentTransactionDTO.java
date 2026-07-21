package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import ovaro.plat4m.domain.FinanceInvestmentActivityType;
import ovaro.plat4m.domain.FinanceTransaction;

public class FinanceInvestmentTransactionDTO {

    private static final Set<FinanceInvestmentActivityType> POSITIVE_REINVESTMENT_TYPES = EnumSet.of(
        FinanceInvestmentActivityType.DIVDEND_REINVESTMENT,
        FinanceInvestmentActivityType.REINVEST_DIVIDEND_COMBINED,
        FinanceInvestmentActivityType.REINVEST_INTEREST,
        FinanceInvestmentActivityType.REINVEST_L_TERM_CG_DIST,
        FinanceInvestmentActivityType.REINVEST_S_TERM_CG_DIST
    );

    FinanceTransaction transaction;
    LocalDate date;
    String type;
    String currencyCode;
    Double quantity;
    Double price;
    BigDecimal amount;
    BigDecimal amountBase;
    Double rateToBase;

    public FinanceInvestmentTransactionDTO(FinanceTransaction txn) {
        this.transaction = txn;
        FinanceInvestmentActivityType investmentActivityType = transaction.getInvestmentActivityType();
        type = investmentActivityType.name();
        price = transaction.getPrice();
        quantity = transaction.getQuantity();
        amount = normalizeDisplayAmount(transaction.getAmount(), investmentActivityType);
        amountBase = normalizeDisplayAmount(transaction.getAmountBase(), investmentActivityType);
        rateToBase = transaction.getRateToBase();
        currencyCode = transaction.getCurrencyCode();
        date = transaction.getDate();
    }

    private BigDecimal normalizeDisplayAmount(BigDecimal rawAmount, FinanceInvestmentActivityType investmentActivityType) {
        if (rawAmount == null) {
            return null;
        }
        if (investmentActivityType != null && POSITIVE_REINVESTMENT_TYPES.contains(investmentActivityType)) {
            return rawAmount.abs();
        }
        return rawAmount;
    }

    public FinanceTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(FinanceTransaction transaction) {
        this.transaction = transaction;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountBase() {
        return amountBase;
    }

    public void setAmountBase(BigDecimal amountBase) {
        this.amountBase = amountBase;
    }

    public Double getRateToBase() {
        return rateToBase;
    }

    public void setRateToBase(Double rateToBase) {
        this.rateToBase = rateToBase;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
