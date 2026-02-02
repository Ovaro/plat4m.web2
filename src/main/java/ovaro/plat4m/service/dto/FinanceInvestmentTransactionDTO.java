package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import ovaro.plat4m.domain.FinanceTransaction;

public class FinanceInvestmentTransactionDTO {

    FinanceTransaction transaction;
    LocalDate date;
    String type;
    //String currencyCode;
    Double quantity;
    Double price;
    BigDecimal amount;

    public FinanceInvestmentTransactionDTO(FinanceTransaction txn) {
        this.transaction = txn;
        type = transaction.getInvestmentActivityType().name();
        price = transaction.getPrice();
        quantity = transaction.getQuantity();
        amount = transaction.getAmount();
        date = transaction.getDate();
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
    // public String getCurrencyCode() {
    //     return currencyCode;
    // }
    // public void setCurrencyCode(String currencyCode) {
    //     this.currencyCode = currencyCode;
    // }

}
