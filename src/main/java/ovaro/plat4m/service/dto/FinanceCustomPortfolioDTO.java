package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import ovaro.plat4m.domain.FinanceCustomPortfolioStrategy;

public class FinanceCustomPortfolioDTO {

    private UUID id;
    private String name;
    private String description;
    private FinanceCustomPortfolioStrategy strategy;
    private String customStrategy;
    private BigDecimal expectedReturnCagr;
    private Set<UUID> securityIds = new LinkedHashSet<>();
    private Set<UUID> accountIds = new LinkedHashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FinanceCustomPortfolioStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(FinanceCustomPortfolioStrategy strategy) {
        this.strategy = strategy;
    }

    public String getCustomStrategy() {
        return customStrategy;
    }

    public void setCustomStrategy(String customStrategy) {
        this.customStrategy = customStrategy;
    }

    public BigDecimal getExpectedReturnCagr() {
        return expectedReturnCagr;
    }

    public void setExpectedReturnCagr(BigDecimal expectedReturnCagr) {
        this.expectedReturnCagr = expectedReturnCagr;
    }

    public Set<UUID> getSecurityIds() {
        return securityIds;
    }

    public void setSecurityIds(Set<UUID> securityIds) {
        this.securityIds = securityIds;
    }

    public Set<UUID> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(Set<UUID> accountIds) {
        this.accountIds = accountIds;
    }
}
