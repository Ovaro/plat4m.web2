package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FinanceCustomPortfolioOptionsDTO {

    private List<FinanceCustomPortfolioSecurityOptionDTO> securities = new ArrayList<>();
    private List<FinanceCustomPortfolioAccountOptionDTO> accounts = new ArrayList<>();

    public List<FinanceCustomPortfolioSecurityOptionDTO> getSecurities() {
        return securities;
    }

    public void setSecurities(List<FinanceCustomPortfolioSecurityOptionDTO> securities) {
        this.securities = securities;
    }

    public List<FinanceCustomPortfolioAccountOptionDTO> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<FinanceCustomPortfolioAccountOptionDTO> accounts) {
        this.accounts = accounts;
    }

    public static class FinanceCustomPortfolioSecurityOptionDTO {

        private String id;
        private String name;
        private String symbol;
        private String currencyCode;
        private Boolean ignoredForRollup;
        private Double currentQuantity;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        public Boolean getIgnoredForRollup() {
            return ignoredForRollup;
        }

        public void setIgnoredForRollup(Boolean ignoredForRollup) {
            this.ignoredForRollup = ignoredForRollup;
        }

        public Double getCurrentQuantity() {
            return currentQuantity;
        }

        public void setCurrentQuantity(Double currentQuantity) {
            this.currentQuantity = currentQuantity;
        }
    }

    public static class FinanceCustomPortfolioAccountOptionDTO {

        private String id;
        private String name;
        private Integer type;
        private String currencyCode;
        private Boolean closed;
        private BigDecimal currentBalance;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        public Boolean getClosed() {
            return closed;
        }

        public void setClosed(Boolean closed) {
            this.closed = closed;
        }

        public BigDecimal getCurrentBalance() {
            return currentBalance;
        }

        public void setCurrentBalance(BigDecimal currentBalance) {
            this.currentBalance = currentBalance;
        }
    }
}
