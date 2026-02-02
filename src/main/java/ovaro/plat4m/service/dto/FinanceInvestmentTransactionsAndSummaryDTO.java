package ovaro.plat4m.service.dto;

import java.util.List;

public class FinanceInvestmentTransactionsAndSummaryDTO extends FinanceInvestmentSummaryDTO {

    List<FinanceInvestmentTransactionDTO> transactions;

    public FinanceInvestmentTransactionsAndSummaryDTO(String userSecurityId) {
        super(userSecurityId);
    }

    public List<FinanceInvestmentTransactionDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<FinanceInvestmentTransactionDTO> transactions) {
        this.transactions = transactions;
    }
}
