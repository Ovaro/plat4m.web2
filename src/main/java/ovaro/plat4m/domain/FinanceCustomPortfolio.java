package ovaro.plat4m.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "fin_custom_portfolio", uniqueConstraints = @UniqueConstraint(columnNames = { "user_guid", "name" }))
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceCustomPortfolio implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    @Column(name = "user_guid", nullable = false)
    private String userGuid;

    @NotNull
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinanceCustomPortfolioStrategy strategy = FinanceCustomPortfolioStrategy.BALANCED;

    @Column(name = "custom_strategy", length = 2000)
    private String customStrategy;

    @Column(name = "expected_return_cagr", nullable = false, precision = 10, scale = 6)
    private BigDecimal expectedReturnCagr = new BigDecimal("0.075");

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fin_custom_portfolio_security", joinColumns = @JoinColumn(name = "portfolio_id"))
    @Column(name = "user_security_id", nullable = false)
    private Set<UUID> securityIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fin_custom_portfolio_account", joinColumns = @JoinColumn(name = "portfolio_id"))
    @Column(name = "account_id", nullable = false)
    private Set<UUID> accountIds = new LinkedHashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserGuid() {
        return userGuid;
    }

    public void setUserGuid(String userGuid) {
        this.userGuid = userGuid;
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
