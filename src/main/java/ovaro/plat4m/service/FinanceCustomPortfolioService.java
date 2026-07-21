package ovaro.plat4m.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceAccountType;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinanceCustomPortfolio;
import ovaro.plat4m.domain.FinanceCustomPortfolioStrategy;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceCustomPortfolioRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.FinanceUserSecurityRepository;
import ovaro.plat4m.service.dto.FinanceCustomPortfolioDTO;
import ovaro.plat4m.service.dto.FinanceCustomPortfolioOptionsDTO;
import ovaro.plat4m.service.dto.FinanceCustomPortfolioOptionsDTO.FinanceCustomPortfolioAccountOptionDTO;
import ovaro.plat4m.service.dto.FinanceCustomPortfolioOptionsDTO.FinanceCustomPortfolioSecurityOptionDTO;
import ovaro.plat4m.service.dto.FinanceInvestmentPortfolioSummaryDTO;
import ovaro.plat4m.service.dto.FinanceSecurityHoldingDTO;
import ovaro.plat4m.service.dto.FinanceSnapshot;
import ovaro.plat4m.service.dto.FinanceSnapshotsPerResourceDTO;

@Service
@Transactional
public class FinanceCustomPortfolioService {

    private static final BigDecimal DEFAULT_EXPECTED_RETURN_CAGR = new BigDecimal("0.075");

    private final FinanceCustomPortfolioRepository customPortfolioRepository;
    private final FinanceUserSecurityRepository userSecurityRepository;
    private final FinanceAccountRepository accountRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final FinanceAccountService accountService;
    private final FinanceFXService fxService;

    public FinanceCustomPortfolioService(
        FinanceCustomPortfolioRepository customPortfolioRepository,
        FinanceUserSecurityRepository userSecurityRepository,
        FinanceAccountRepository accountRepository,
        FinanceTransactionRepository transactionRepository,
        FinanceAccountService accountService,
        FinanceFXService fxService
    ) {
        this.customPortfolioRepository = customPortfolioRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.fxService = fxService;
    }

    @Transactional(readOnly = true)
    public List<FinanceCustomPortfolioDTO> findAll(User user) {
        return this.customPortfolioRepository.findAllByUserGuidOrderByNameAsc(user.getGuid().toString()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FinanceCustomPortfolioDTO findOne(User user, UUID id) {
        return toDto(getPortfolio(user, id));
    }

    public FinanceCustomPortfolioDTO create(User user, FinanceCustomPortfolioDTO dto) {
        validateName(user, dto, null);
        FinanceCustomPortfolio portfolio = new FinanceCustomPortfolio();
        portfolio.setUserGuid(user.getGuid().toString());
        applyDto(user, portfolio, dto);
        return toDto(this.customPortfolioRepository.save(portfolio));
    }

    public FinanceCustomPortfolioDTO update(User user, UUID id, FinanceCustomPortfolioDTO dto) {
        FinanceCustomPortfolio portfolio = getPortfolio(user, id);
        validateName(user, dto, id);
        applyDto(user, portfolio, dto);
        return toDto(this.customPortfolioRepository.save(portfolio));
    }

    public void delete(User user, UUID id) {
        FinanceCustomPortfolio portfolio = getPortfolio(user, id);
        this.customPortfolioRepository.delete(portfolio);
    }

    @Transactional(readOnly = true)
    public FinanceCustomPortfolioOptionsDTO getOptions(User user) {
        FinanceCustomPortfolioOptionsDTO dto = new FinanceCustomPortfolioOptionsDTO();

        Map<String, FinanceSecurityHoldingDTO> currentHoldingsById = this.accountService
            .investmentAccountHoldings(user, null, true, null)
            .stream()
            .collect(Collectors.toMap(FinanceSecurityHoldingDTO::getId, holding -> holding, (left, right) -> left));

        List<FinanceCustomPortfolioSecurityOptionDTO> securities = this.userSecurityRepository
            .findByUserGuid(user.getGuid().toString())
            .stream()
            .sorted(Comparator.comparing(FinanceUserSecurity::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
            .map(security -> {
                FinanceSecurityHoldingDTO holding = currentHoldingsById.get(security.getId().toString());
                FinanceCustomPortfolioSecurityOptionDTO option = new FinanceCustomPortfolioSecurityOptionDTO();
                option.setId(security.getId().toString());
                option.setName(security.getName());
                option.setSymbol(security.getSymbol());
                option.setCurrencyCode(security.getCurrencyCode());
                option.setIgnoredForRollup(security.isIgnoredForRollup());
                option.setCurrentQuantity(holding != null ? holding.getQuantity() : 0.0);
                return option;
            })
            .toList();
        dto.setSecurities(securities);

        List<FinanceCustomPortfolioAccountOptionDTO> accounts = this.accountRepository
            .findAllByUserGuid(user.getGuid().toString())
            .stream()
            .filter(this::isAllowedCashAccount)
            .sorted(Comparator.comparing(FinanceAccount::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
            .map(account -> {
                FinanceCustomPortfolioAccountOptionDTO option = new FinanceCustomPortfolioAccountOptionDTO();
                option.setId(account.getId().toString());
                option.setName(account.getName());
                option.setType(account.getType());
                option.setCurrencyCode(account.getCurrencyCode());
                option.setClosed(account.isClosed());
                option.setCurrentBalance(currentAccountBalance(account));
                return option;
            })
            .toList();
        dto.setAccounts(accounts);

        return dto;
    }

    @Transactional(readOnly = true)
    public Set<String> getSecurityIdStrings(User user, UUID portfolioId, boolean includeIgnoredForRollup) {
        FinanceCustomPortfolio portfolio = getPortfolio(user, portfolioId);
        if (portfolio.getSecurityIds() == null || portfolio.getSecurityIds().isEmpty()) {
            return Set.of();
        }
        return this.userSecurityRepository
            .findAllByUserGuidAndIdIn(user.getGuid().toString(), portfolio.getSecurityIds())
            .stream()
            .filter(security -> includeIgnoredForRollup || !security.isIgnoredForRollup())
            .map(security -> security.getId().toString())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public List<FinanceUserSecurity> getUserSecurities(User user, UUID portfolioId, boolean includeIgnoredForRollup) {
        FinanceCustomPortfolio portfolio = getPortfolio(user, portfolioId);
        if (portfolio.getSecurityIds() == null || portfolio.getSecurityIds().isEmpty()) {
            return List.of();
        }
        return this.userSecurityRepository
            .findAllByUserGuidAndIdIn(user.getGuid().toString(), portfolio.getSecurityIds())
            .stream()
            .filter(security -> includeIgnoredForRollup || !security.isIgnoredForRollup())
            .toList();
    }

    @Transactional(readOnly = true)
    public Collection<FinanceSecurityHoldingDTO> getHoldings(User user, UUID portfolioId, boolean includeClosed, LocalDate date) {
        FinanceCustomPortfolio portfolio = getPortfolio(user, portfolioId);
        Set<String> securityIds = getSecurityIdStrings(user, portfolioId, true);
        List<FinanceSecurityHoldingDTO> holdings = this.accountService
            .investmentAccountHoldings(user, null, includeClosed, null)
            .stream()
            .filter(holding -> securityIds.contains(holding.getId()))
            .peek(holding -> {
                if (holding.getPositionType() == null) {
                    holding.setPositionType("security");
                }
            })
            .collect(Collectors.toCollection(ArrayList::new));

        holdings.addAll(getCashHoldings(user, portfolio, date));
        return holdings;
    }

    @Transactional(readOnly = true)
    public List<FinanceSecurityHoldingDTO> getCashHoldings(User user, FinanceCustomPortfolio portfolio, LocalDate date) {
        List<FinanceAccount> accounts = getCashAccounts(user, portfolio);
        Map<String, BigDecimal> interestByAccount = getInterestByAccount(user, accounts, date, LocalDate.now());
        Map<String, Double> cashReturnByAccount = getCashReturnPercentByAccount(user, accounts, date, LocalDate.now(), interestByAccount);
        List<FinanceSecurityHoldingDTO> result = new ArrayList<>();

        for (FinanceAccount account : accounts) {
            FinanceSecurityHoldingDTO holding = new FinanceSecurityHoldingDTO();
            holding.setPositionType("cash");
            holding.setId(account.getId().toString());
            holding.setAccountId(account.getId().toString());
            holding.setAccountName(account.getName());
            holding.setName(account.getName());
            holding.setSymbol("CASH");
            holding.setUserSymbol("CASH");
            holding.setCurrencyCode(account.getCurrencyCode());
            holding.setType(account.getType());
            holding.setTypeName(FinanceAccountType.getValueString(account.getType()));
            holding.setSector("Cash");
            holding.setIndustry("Banking");
            holding.setExchangeName("Cash");
            holding.setQuantity(1);
            holding.setPrice(currentAccountBalance(account));
            holding.setValue(currentAccountBalance(account));
            holding.setCashInterest(interestByAccount.getOrDefault(account.getId().toString(), BigDecimal.ZERO));
            holding.setCashReturnPercent(cashReturnByAccount.get(account.getId().toString()));
            addFx(user, holding, LocalDate.now());
            result.add(holding);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public FinanceInvestmentPortfolioSummaryDTO addCashToSummary(
        User user,
        UUID portfolioId,
        FinanceInvestmentPortfolioSummaryDTO summary,
        LocalDate periodStart
    ) {
        FinanceCustomPortfolio portfolio = getPortfolio(user, portfolioId);
        List<FinanceAccount> accounts = getCashAccounts(user, portfolio);
        BigDecimal cashValue = localCashValue(user, accounts, LocalDate.now());
        Map<String, BigDecimal> interestByAccount = getInterestByAccount(user, accounts, periodStart, LocalDate.now());
        BigDecimal cashInterest = interestByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageCash = averageLocalCashValue(user, accounts, periodStart, LocalDate.now());
        Double cashReturnPercent =
            averageCash.compareTo(BigDecimal.ZERO) == 0
                ? null
                : annualizeCashReturn(
                      cashInterest.divide(averageCash, 8, RoundingMode.HALF_UP).doubleValue(),
                      periodStart,
                      LocalDate.now()
                  );

        summary.setExpectedReturnCagr(portfolio.getExpectedReturnCagr());
        summary.setCashValue(cashValue);
        summary.setCashInterest(cashInterest);
        summary.setCashReturnPercent(cashReturnPercent);
        summary.setTotalValue(nullToZero(summary.getTotalValue()).add(cashValue));
        summary.setTotalIncome(nullToZero(summary.getTotalIncome()).add(cashInterest));
        summary.setTotalReturn(nullToZero(summary.getTotalReturn()).add(cashInterest));
        return summary;
    }

    @Transactional(readOnly = true)
    public Collection<FinanceSnapshotsPerResourceDTO> getCashHistory(User user, UUID portfolioId, LocalDate start, LocalDate end) {
        FinanceCustomPortfolio portfolio = getPortfolio(user, portfolioId);
        Set<String> accountIds = getCashAccounts(user, portfolio)
            .stream()
            .map(account -> account.getId().toString())
            .collect(Collectors.toSet());
        if (accountIds.isEmpty()) {
            return List.of();
        }

        return this.accountService
            .getAllAccountsMonthlyRunningBalance(user, start, end)
            .stream()
            .filter(resource -> accountIds.contains(resource.getId()))
            .peek(resource -> {
                resource.setType("cash");
                if (resource.getSnapshots() != null) {
                    for (FinanceSnapshot snapshot : resource.getSnapshots()) {
                        snapshot.setValue(toLocalValue(snapshot.getValue(), snapshot.getFxToLocal()));
                        snapshot.setFxToLocal(null);
                        snapshot.setCurrencyIsoCode(user.getLocalCurrency());
                    }
                }
            })
            .toList();
    }

    private FinanceCustomPortfolio getPortfolio(User user, UUID id) {
        return this.customPortfolioRepository
            .findByIdAndUserGuid(id, user.getGuid().toString())
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));
    }

    private void validateName(User user, FinanceCustomPortfolioDTO dto, UUID existingId) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name is required");
        }
        boolean duplicate =
            existingId == null
                ? this.customPortfolioRepository.existsByUserGuidAndNameIgnoreCase(user.getGuid().toString(), dto.getName().trim())
                : this.customPortfolioRepository.existsByUserGuidAndNameIgnoreCaseAndIdNot(
                      user.getGuid().toString(),
                      dto.getName().trim(),
                      existingId
                  );
        if (duplicate) {
            throw new IllegalArgumentException("Portfolio name already exists");
        }
    }

    private void applyDto(User user, FinanceCustomPortfolio portfolio, FinanceCustomPortfolioDTO dto) {
        FinanceCustomPortfolioStrategy strategy = dto.getStrategy() != null ? dto.getStrategy() : FinanceCustomPortfolioStrategy.BALANCED;
        if (
            strategy == FinanceCustomPortfolioStrategy.CUSTOM &&
            (dto.getCustomStrategy() == null || dto.getCustomStrategy().trim().isEmpty())
        ) {
            throw new IllegalArgumentException("Custom strategy is required");
        }

        portfolio.setName(dto.getName().trim());
        portfolio.setDescription(trimToNull(dto.getDescription()));
        portfolio.setStrategy(strategy);
        portfolio.setCustomStrategy(strategy == FinanceCustomPortfolioStrategy.CUSTOM ? trimToNull(dto.getCustomStrategy()) : null);
        portfolio.setExpectedReturnCagr(dto.getExpectedReturnCagr() != null ? dto.getExpectedReturnCagr() : DEFAULT_EXPECTED_RETURN_CAGR);
        portfolio.setSecurityIds(validSecurityIds(user, dto.getSecurityIds()));
        portfolio.setAccountIds(validCashAccountIds(user, dto.getAccountIds()));
    }

    private Set<UUID> validSecurityIds(User user, Set<UUID> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> validIds = this.userSecurityRepository
            .findAllByUserGuidAndIdIn(user.getGuid().toString(), requestedIds)
            .stream()
            .map(FinanceUserSecurity::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (validIds.size() != requestedIds.size()) {
            throw new IllegalArgumentException("One or more selected investments could not be found");
        }
        return validIds;
    }

    private Set<UUID> validCashAccountIds(User user, Set<UUID> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Set<UUID> validIds = new LinkedHashSet<>();
        for (UUID accountId : requestedIds) {
            FinanceAccount account = this.accountRepository
                .findByIdAndUserGuid(accountId, user.getGuid().toString())
                .orElseThrow(() -> new IllegalArgumentException("One or more selected bank accounts could not be found"));
            if (!isAllowedCashAccount(account)) {
                throw new IllegalArgumentException("Only bank and cash accounts can be added to a portfolio");
            }
            validIds.add(account.getId());
        }
        return validIds;
    }

    private FinanceCustomPortfolioDTO toDto(FinanceCustomPortfolio portfolio) {
        FinanceCustomPortfolioDTO dto = new FinanceCustomPortfolioDTO();
        dto.setId(portfolio.getId());
        dto.setName(portfolio.getName());
        dto.setDescription(portfolio.getDescription());
        dto.setStrategy(portfolio.getStrategy());
        dto.setCustomStrategy(portfolio.getCustomStrategy());
        dto.setExpectedReturnCagr(portfolio.getExpectedReturnCagr());
        dto.setSecurityIds(portfolio.getSecurityIds() != null ? new LinkedHashSet<>(portfolio.getSecurityIds()) : new LinkedHashSet<>());
        dto.setAccountIds(portfolio.getAccountIds() != null ? new LinkedHashSet<>(portfolio.getAccountIds()) : new LinkedHashSet<>());
        return dto;
    }

    private List<FinanceAccount> getCashAccounts(User user, FinanceCustomPortfolio portfolio) {
        if (portfolio.getAccountIds() == null || portfolio.getAccountIds().isEmpty()) {
            return List.of();
        }

        List<FinanceAccount> accounts = new ArrayList<>();
        for (UUID accountId : portfolio.getAccountIds()) {
            Optional<FinanceAccount> account = this.accountRepository.findByIdAndUserGuid(accountId, user.getGuid().toString());
            if (account.isPresent() && isAllowedCashAccount(account.get())) {
                accounts.add(account.get());
            }
        }
        return accounts;
    }

    private boolean isAllowedCashAccount(FinanceAccount account) {
        return (
            account.getType() != null &&
            (account.getType().equals(FinanceAccountType.BANKING.getValue()) ||
                account.getType().equals(FinanceAccountType.CASH.getValue()))
        );
    }

    private BigDecimal currentAccountBalance(FinanceAccount account) {
        BigDecimal balance = nullToZero(account.getStartingBalance());
        BigDecimal transactionSum = this.transactionRepository.findSumTransactionsForAccount(account.getId().toString());
        return balance.add(nullToZero(transactionSum));
    }

    private void addFx(User user, FinanceSecurityHoldingDTO holding, LocalDate date) {
        if (
            holding.getCurrencyCode() == null ||
            user.getLocalCurrency() == null ||
            user.getLocalCurrency().equals(holding.getCurrencyCode())
        ) {
            return;
        }
        FinanceFX fx = this.fxService.getLatestFX(holding.getCurrencyCode(), user.getLocalCurrency(), date);
        if (fx != null) {
            holding.setFxDateTime(fx.getDate());
            holding.setFxRateToLocal(fx.getRate());
        }
    }

    private Map<String, BigDecimal> getInterestByAccount(User user, List<FinanceAccount> accounts, LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> result = new HashMap<>();
        Set<String> accountIds = accounts
            .stream()
            .map(account -> account.getId().toString())
            .collect(Collectors.toCollection(HashSet::new));
        if (accountIds.isEmpty()) {
            return result;
        }

        for (FinanceTransaction transaction : this.transactionRepository.findPositiveAccountTransactionsForReturn(
            user.getGuid().toString(),
            accountIds,
            startDate != null ? startDate : LocalDate.of(1900, 1, 1),
            endDate
        )) {
            if (!isInterestIncome(transaction.getCategory())) {
                continue;
            }
            BigDecimal amount = toLocalTransactionAmount(user, transaction);
            result.merge(transaction.getAccountId(), amount, BigDecimal::add);
        }
        return result;
    }

    private boolean isInterestIncome(FinanceCategory category) {
        if (category == null) {
            return false;
        }

        String path = categoryPath(category).toLowerCase();
        return path.contains("investment income") || (path.contains("income") && path.contains("interest"));
    }

    private String categoryPath(FinanceCategory category) {
        List<String> parts = new ArrayList<>();
        FinanceCategory current = category;
        while (current != null) {
            if (current.getName() != null) {
                parts.add(current.getName());
            }
            current = current.getParent();
        }
        return String.join(" / ", parts);
    }

    private BigDecimal toLocalTransactionAmount(User user, FinanceTransaction transaction) {
        if (transaction.getAmountBase() != null) {
            return transaction.getAmountBase();
        }
        BigDecimal amount = nullToZero(transaction.getAmount());
        if (
            transaction.getCurrencyCode() == null ||
            user.getLocalCurrency() == null ||
            user.getLocalCurrency().equals(transaction.getCurrencyCode())
        ) {
            return amount;
        }
        if (transaction.getRateToBase() != null) {
            return amount.multiply(BigDecimal.valueOf(transaction.getRateToBase()));
        }
        FinanceFX fx = this.fxService.getLatestFX(transaction.getCurrencyCode(), user.getLocalCurrency(), transaction.getDate());
        return fx != null ? amount.multiply(BigDecimal.valueOf(fx.getRate())) : amount;
    }

    private Map<String, Double> getCashReturnPercentByAccount(
        User user,
        List<FinanceAccount> accounts,
        LocalDate start,
        LocalDate end,
        Map<String, BigDecimal> interestByAccount
    ) {
        Map<String, Double> result = new HashMap<>();
        for (FinanceAccount account : accounts) {
            BigDecimal average = averageLocalCashValue(user, List.of(account), start, end);
            if (average.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal interest = interestByAccount.getOrDefault(account.getId().toString(), BigDecimal.ZERO);
            result.put(
                account.getId().toString(),
                annualizeCashReturn(interest.divide(average, 8, RoundingMode.HALF_UP).doubleValue(), start, end)
            );
        }
        return result;
    }

    private double annualizeCashReturn(double periodReturn, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return periodReturn;
        }
        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) {
            return periodReturn;
        }
        return periodReturn / (days / 365.0);
    }

    private BigDecimal localCashValue(User user, List<FinanceAccount> accounts, LocalDate date) {
        return accounts
            .stream()
            .map(account -> toLocalValue(currentAccountBalance(account), currentFxRate(user, account, date)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal averageLocalCashValue(User user, List<FinanceAccount> accounts, LocalDate start, LocalDate end) {
        Collection<FinanceSnapshotsPerResourceDTO> snapshots = this.accountService.getAllAccountsMonthlyRunningBalance(user, start, end);
        Set<String> accountIds = accounts
            .stream()
            .map(account -> account.getId().toString())
            .collect(Collectors.toSet());
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (FinanceSnapshotsPerResourceDTO resource : snapshots) {
            if (!accountIds.contains(resource.getId()) || resource.getSnapshots() == null) {
                continue;
            }
            for (FinanceSnapshot snapshot : resource.getSnapshots()) {
                sum = sum.add(toLocalValue(snapshot.getValue(), snapshot.getFxToLocal()));
                count++;
            }
        }
        return count == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP);
    }

    private Double currentFxRate(User user, FinanceAccount account, LocalDate date) {
        if (
            account.getCurrencyCode() == null ||
            user.getLocalCurrency() == null ||
            user.getLocalCurrency().equals(account.getCurrencyCode())
        ) {
            return null;
        }
        FinanceFX fx = this.fxService.getLatestFX(account.getCurrencyCode(), user.getLocalCurrency(), date);
        return fx != null ? fx.getRate() : null;
    }

    private BigDecimal toLocalValue(BigDecimal value, Double fxRate) {
        BigDecimal localValue = nullToZero(value);
        if (fxRate != null && fxRate.doubleValue() != 0.0) {
            localValue = localValue.multiply(BigDecimal.valueOf(fxRate));
        }
        return localValue;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
