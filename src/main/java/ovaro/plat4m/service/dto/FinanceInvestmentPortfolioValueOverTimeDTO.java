package ovaro.plat4m.service.dto;

import java.util.ArrayList;
import java.util.List;

public class FinanceInvestmentPortfolioValueOverTimeDTO {

    //Map<String, List<FinanceInvestmentValueOverTimeDTO>> valueOverTimePerSecurity;
    List<FinanceInvestmentValueOverTimeDTO> portfolioValueHistoryItems;

    public FinanceInvestmentPortfolioValueOverTimeDTO() {}

    public void addPortfolioValueHistoryItem(FinanceInvestmentValueOverTimeDTO portfolioValueHistoryItem) {
        if (portfolioValueHistoryItems == null) {
            portfolioValueHistoryItems = new ArrayList<FinanceInvestmentValueOverTimeDTO>();
        }
        portfolioValueHistoryItems.add(portfolioValueHistoryItem);
    }

    public List<FinanceInvestmentValueOverTimeDTO> getPortfolioValueHistoryItems() {
        return portfolioValueHistoryItems;
    }

    public void setPortfolioValueHistoryItems(List<FinanceInvestmentValueOverTimeDTO> portfolioValueHistoryItems) {
        this.portfolioValueHistoryItems = portfolioValueHistoryItems;
    }
}
