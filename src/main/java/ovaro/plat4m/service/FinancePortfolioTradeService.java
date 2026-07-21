package ovaro.plat4m.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.domain.FinanceInvestmentActivityType;
import ovaro.plat4m.domain.FinanceLot;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceLotRepository;
import ovaro.plat4m.repository.FinanceSecurityPriceRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.FinanceUserSecurityRepository;
import ovaro.plat4m.service.dto.FinancePortfolioTradeDTO;
import ovaro.plat4m.service.dto.FinancePortfolioTradeDTO.DividendIncomeDTO;
import ovaro.plat4m.service.dto.FinanceSecurityHoldingDTO;

@Service
public class FinancePortfolioTradeService {

    private final FinanceTransactionRepository transactionRepository;
    private final FinanceUserSecurityRepository userSecurityRepository;
    private final FinanceAccountRepository accountRepository;
    private final FinanceSecurityPriceRepository securityPriceRepository;
    private final FinanceLotRepository lotRepository;
    private final FinanceFXService financeFXService;
    private final FinanceAccountService financeAccountService;

    public FinancePortfolioTradeService(
        FinanceTransactionRepository transactionRepository,
        FinanceUserSecurityRepository userSecurityRepository,
        FinanceAccountRepository accountRepository,
        FinanceSecurityPriceRepository securityPriceRepository,
        FinanceLotRepository lotRepository,
        FinanceFXService financeFXService,
        FinanceAccountService financeAccountService
    ) {
        this.transactionRepository = transactionRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.accountRepository = accountRepository;
        this.securityPriceRepository = securityPriceRepository;
        this.lotRepository = lotRepository;
        this.financeFXService = financeFXService;
        this.financeAccountService = financeAccountService;
    }

    public List<FinancePortfolioTradeDTO> getTrades(User user, String accountId, boolean includeClosed) {
        return getTrades(user, accountId, includeClosed, null);
    }

    public List<FinancePortfolioTradeDTO> getTrades(User user, String accountId, boolean includeClosed, Set<String> customSecurityIds) {
        List<FinanceTransaction> transactions = this.transactionRepository.findPortfolioTradeTransactions(user.getGuid().toString());

        Set<String> includedSecurityIds =
            customSecurityIds != null ? customSecurityIds : getIncludedSecurityIds(user, accountId, includeClosed);
        Map<String, FinanceUserSecurity> userSecurities = loadUserSecurities(user, transactions, includedSecurityIds);
        Map<String, FinanceAccount> accounts = loadAccounts(user);
        Map<String, FinanceSecurityPrice> currentPrices = loadCurrentPrices(userSecurities.values());
        Map<String, Double> currentFxRates = new HashMap<>();
        Map<String, Map<LocalDate, Double>> historicalFxRates = new HashMap<>();
        Map<UUID, List<FinanceLot>> lotsByBuyTransactionId = loadLotsByBuyTransactionId(user, transactions);
        Map<UUID, FinanceTransaction> sellTransactions = loadSellTransactions(user, lotsByBuyTransactionId.values());

        List<FinancePortfolioTradeDTO> result = new ArrayList<>();
        List<TradeContext> tradeContexts = new ArrayList<>();
        for (FinanceTransaction transaction : transactions) {
            if (!isTradeActivity(transaction.getInvestmentActivityType())) {
                continue;
            }
            if (accountId != null && !accountId.equals(transaction.getAccountId())) {
                continue;
            }
            if (!includedSecurityIds.isEmpty() && !includedSecurityIds.contains(transaction.getSecurityId())) {
                continue;
            }

            FinanceUserSecurity userSecurity = userSecurities.get(transaction.getSecurityId());
            if (userSecurity == null) {
                continue;
            }

            FinancePortfolioTradeDTO dto = new FinancePortfolioTradeDTO();
            dto.setId(transaction.getId() != null ? transaction.getId().toString() : null);
            dto.setDate(transaction.getDate());
            dto.setType(transaction.getInvestmentActivityType().name);
            dto.setAccountId(transaction.getAccountId());
            FinanceAccount account = accounts.get(transaction.getAccountId());
            dto.setAccountName(account != null ? account.getName() : null);
            dto.setSecurityId(transaction.getSecurityId());
            dto.setSymbol(userSecurity.getSymbol());
            dto.setName(userSecurity.getName());
            dto.setCurrencyCode(userSecurity.getCurrencyCode());
            dto.setIgnoredForRollup(userSecurity.isIgnoredForRollup());
            dto.setIgnoredForRollupReason(userSecurity.getIgnoredForRollupReason());
            dto.setQuantity(transaction.getQuantity());
            dto.setBuyPrice(transaction.getPrice());

            Double buyFxRate = resolveHistoricalFxRate(user, transaction, userSecurity.getCurrencyCode(), historicalFxRates);
            dto.setBuyFxRate(buyFxRate);

            FinanceSecurityPrice currentPrice = currentPrices.get(transaction.getSecurityId());
            dto.setCurrentPrice(currentPrice != null && currentPrice.getPrice() != null ? currentPrice.getPrice().doubleValue() : null);

            Double currentFxRate = resolveCurrentFxRate(user, userSecurity.getCurrencyCode(), currentFxRates);
            dto.setCurrentFxRate(currentFxRate);

            applyDeltaMetrics(dto);
            applyRealizedLotMetrics(dto, transaction, user, userSecurity, lotsByBuyTransactionId, sellTransactions, historicalFxRates);
            result.add(dto);
            tradeContexts.add(new TradeContext(dto, transaction, userSecurity, lotsByBuyTransactionId.get(transaction.getId())));
        }

        applyDividendIncome(user, transactions, tradeContexts, historicalFxRates);

        return result;
    }

    private Map<UUID, List<FinanceLot>> loadLotsByBuyTransactionId(User user, List<FinanceTransaction> transactions) {
        Set<UUID> tradeTransactionIds = new HashSet<>();
        for (FinanceTransaction transaction : transactions) {
            if (transaction.getId() == null || !isTradeActivity(transaction.getInvestmentActivityType())) {
                continue;
            }
            tradeTransactionIds.add(transaction.getId());
        }

        Map<UUID, List<FinanceLot>> result = new HashMap<>();
        if (tradeTransactionIds.isEmpty()) {
            return result;
        }

        for (FinanceLot lot : this.lotRepository.findBuyLotsByUserGuidAndTransactionIds(user.getGuid().toString(), tradeTransactionIds)) {
            UUID buyTransactionId = firstNonNull(lot.getBuyTransactionId(), lot.getOpenTransactionId());
            if (buyTransactionId == null) {
                continue;
            }
            result.computeIfAbsent(buyTransactionId, ignored -> new ArrayList<>()).add(lot);
        }
        return result;
    }

    private Map<UUID, FinanceTransaction> loadSellTransactions(User user, Collection<List<FinanceLot>> lotGroups) {
        Set<UUID> sellTransactionIds = new HashSet<>();
        for (List<FinanceLot> lots : lotGroups) {
            for (FinanceLot lot : lots) {
                UUID sellTransactionId = firstNonNull(lot.getSellTransactionId(), lot.getCloseTransactionId());
                if (sellTransactionId != null) {
                    sellTransactionIds.add(sellTransactionId);
                }
            }
        }

        Map<UUID, FinanceTransaction> result = new HashMap<>();
        if (sellTransactionIds.isEmpty()) {
            return result;
        }

        for (FinanceTransaction transaction : this.transactionRepository.findAllByUserGuidAndIdIn(
            user.getGuid().toString(),
            sellTransactionIds
        )) {
            result.put(transaction.getId(), transaction);
        }
        return result;
    }

    private Set<String> getIncludedSecurityIds(User user, String accountId, boolean includeClosed) {
        Collection<FinanceSecurityHoldingDTO> holdings = financeAccountService.investmentAccountHoldings(
            user,
            accountId,
            includeClosed,
            LocalDate.now()
        );
        Set<String> includedSecurityIds = new HashSet<>();
        for (FinanceSecurityHoldingDTO holding : holdings) {
            includedSecurityIds.add(holding.getId());
        }
        return includedSecurityIds;
    }

    private Map<String, FinanceUserSecurity> loadUserSecurities(
        User user,
        List<FinanceTransaction> transactions,
        Set<String> includedSecurityIds
    ) {
        Set<UUID> securityIds = new HashSet<>();
        for (FinanceTransaction transaction : transactions) {
            if (transaction.getSecurityId() == null || transaction.getSecurityId().isBlank()) {
                continue;
            }
            if (!includedSecurityIds.isEmpty() && !includedSecurityIds.contains(transaction.getSecurityId())) {
                continue;
            }
            securityIds.add(UUID.fromString(transaction.getSecurityId()));
        }

        Map<String, FinanceUserSecurity> result = new LinkedHashMap<>();
        for (FinanceUserSecurity security : this.userSecurityRepository.findAllByUserGuidAndIdIn(user.getGuid().toString(), securityIds)) {
            result.put(security.getId().toString(), security);
        }
        return result;
    }

    private Map<String, FinanceAccount> loadAccounts(User user) {
        Map<String, FinanceAccount> result = new HashMap<>();
        for (FinanceAccount account : this.accountRepository.findAllByUserGuid(user.getGuid().toString())) {
            result.put(account.getId().toString(), account);
        }
        return result;
    }

    private Map<String, FinanceSecurityPrice> loadCurrentPrices(Collection<FinanceUserSecurity> userSecurities) {
        List<UUID> ids = new ArrayList<>();
        for (FinanceUserSecurity security : userSecurities) {
            ids.add(security.getId());
        }
        Map<String, FinanceSecurityPrice> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        for (FinanceSecurityPrice price : this.securityPriceRepository.findLatestBySecurityIdIn(ids)) {
            if (price.getSymbol() == null) {
                continue;
            }
            for (FinanceUserSecurity security : userSecurities) {
                if (security.getSecurity() != null && price.getSymbol().equals(security.getSecurity().getSymbol())) {
                    result.put(security.getId().toString(), price);
                }
            }
        }
        return result;
    }

    private Double resolveCurrentFxRate(User user, String currencyCode, Map<String, Double> currentFxRates) {
        if (currencyCode == null || currencyCode.isBlank() || currencyCode.equalsIgnoreCase(user.getLocalCurrency())) {
            return 1.0;
        }
        Double cached = currentFxRates.get(currencyCode);
        if (cached != null) {
            return cached;
        }
        FinanceFX fx = this.financeFXService.getLatestFX(currencyCode, user.getLocalCurrency(), LocalDate.now());
        Double rate = fx != null ? fx.getRate() : null;
        currentFxRates.put(currencyCode, rate);
        return rate;
    }

    private Double resolveHistoricalFxRate(
        User user,
        FinanceTransaction transaction,
        String currencyCode,
        Map<String, Map<LocalDate, Double>> historicalFxRates
    ) {
        if (currencyCode == null || currencyCode.isBlank() || currencyCode.equalsIgnoreCase(user.getLocalCurrency())) {
            return 1.0;
        }
        if (transaction.getRateToBase() != null && transaction.getRateToBase() > 0) {
            return transaction.getRateToBase();
        }
        LocalDate date = transaction.getDate();
        if (date == null) {
            return null;
        }
        Map<LocalDate, Double> perDate = historicalFxRates.computeIfAbsent(currencyCode, ignored -> new HashMap<>());
        if (perDate.containsKey(date)) {
            return perDate.get(date);
        }
        FinanceFX fx = this.financeFXService.getLatestFX(currencyCode, user.getLocalCurrency(), date);
        Double rate = fx != null ? fx.getRate() : null;
        perDate.put(date, rate);
        return rate;
    }

    private boolean isTradeActivity(FinanceInvestmentActivityType type) {
        return (
            type == FinanceInvestmentActivityType.BUY ||
            type == FinanceInvestmentActivityType.REINVEST_DIVIDEND_COMBINED ||
            type == FinanceInvestmentActivityType.DIVDEND_REINVESTMENT
        );
    }

    private boolean isDividendIncomeActivity(FinanceInvestmentActivityType type) {
        return (
            type == FinanceInvestmentActivityType.DIVIDEND ||
            type == FinanceInvestmentActivityType.REINVEST_DIVIDEND_COMBINED ||
            type == FinanceInvestmentActivityType.DIVDEND_REINVESTMENT
        );
    }

    private void applyDividendIncome(
        User user,
        List<FinanceTransaction> transactions,
        List<TradeContext> tradeContexts,
        Map<String, Map<LocalDate, Double>> historicalFxRates
    ) {
        Map<String, List<TradeContext>> tradesByAccountAndSecurity = new HashMap<>();
        for (TradeContext context : tradeContexts) {
            String key = accountSecurityKey(context.transaction.getAccountId(), context.transaction.getSecurityId());
            tradesByAccountAndSecurity.computeIfAbsent(key, ignored -> new ArrayList<>()).add(context);
        }

        for (FinanceTransaction dividendTransaction : transactions) {
            if (!isDividendIncomeActivity(dividendTransaction.getInvestmentActivityType())) {
                continue;
            }
            if (dividendTransaction.getDate() == null || dividendTransaction.getSecurityId() == null) {
                continue;
            }

            List<TradeContext> matchingTrades = tradesByAccountAndSecurity.get(
                accountSecurityKey(dividendTransaction.getAccountId(), dividendTransaction.getSecurityId())
            );
            if (matchingTrades == null || matchingTrades.isEmpty()) {
                continue;
            }

            Map<TradeContext, Double> eligibleQuantities = new LinkedHashMap<>();
            double totalEligibleQuantity = 0;
            for (TradeContext context : matchingTrades) {
                double eligibleQuantity = eligibleQuantityOnDate(context, dividendTransaction.getDate());
                if (eligibleQuantity <= 0) {
                    continue;
                }
                eligibleQuantities.put(context, eligibleQuantity);
                totalEligibleQuantity += eligibleQuantity;
            }
            if (totalEligibleQuantity <= 0) {
                continue;
            }

            DividendAmount dividendAmount = resolveDividendAmount(
                user,
                dividendTransaction,
                matchingTrades.get(0).userSecurity,
                historicalFxRates
            );
            if (dividendAmount.sourceAmount == null && dividendAmount.baseAmount == null) {
                continue;
            }

            for (Map.Entry<TradeContext, Double> entry : eligibleQuantities.entrySet()) {
                double allocationRatio = entry.getValue() / totalEligibleQuantity;
                DividendIncomeDTO dividend = new DividendIncomeDTO();
                dividend.setId(dividendTransaction.getId() != null ? dividendTransaction.getId().toString() : null);
                dividend.setDate(dividendTransaction.getDate());
                dividend.setType(dividendTransaction.getInvestmentActivityType().name);
                dividend.setSourceCurrencyCode(dividendAmount.sourceCurrencyCode);
                dividend.setBaseCurrencyCode(user.getLocalCurrency());
                dividend.setFxRate(dividendAmount.fxRate);
                dividend.setTransactionSourceAmount(dividendAmount.sourceAmount);
                dividend.setTransactionAmountWithoutFx(dividendAmount.sourceAmount);
                dividend.setTransactionAmountWithFx(dividendAmount.baseAmount);
                dividend.setSourceAmount(dividendAmount.sourceAmount != null ? dividendAmount.sourceAmount * allocationRatio : null);
                dividend.setAmountWithoutFx(dividendAmount.sourceAmount != null ? dividendAmount.sourceAmount * allocationRatio : null);
                dividend.setAmountWithFx(dividendAmount.baseAmount != null ? dividendAmount.baseAmount * allocationRatio : null);
                dividend.setAllocationRatio(allocationRatio);
                addDividendToTrade(entry.getKey().dto, dividend);
            }
        }
    }

    private DividendAmount resolveDividendAmount(
        User user,
        FinanceTransaction dividendTransaction,
        FinanceUserSecurity userSecurity,
        Map<String, Map<LocalDate, Double>> historicalFxRates
    ) {
        String sourceCurrencyCode = firstNonBlank(dividendTransaction.getCurrencyCode(), userSecurity.getCurrencyCode());
        Double sourceAmount = absDouble(dividendTransaction.getAmount());
        Double baseAmount = absDouble(dividendTransaction.getAmountBase());
        Double fxRate = resolveHistoricalFxRate(user, dividendTransaction, sourceCurrencyCode, historicalFxRates);

        if (baseAmount == null && sourceAmount != null && fxRate != null) {
            baseAmount = sourceAmount * fxRate;
        }
        if (sourceAmount == null && baseAmount != null && fxRate != null && fxRate != 0) {
            sourceAmount = baseAmount / fxRate;
        }

        return new DividendAmount(sourceCurrencyCode, sourceAmount, baseAmount, fxRate);
    }

    private void addDividendToTrade(FinancePortfolioTradeDTO dto, DividendIncomeDTO dividend) {
        dto.getDividends().add(dividend);
        if (dividend.getAmountWithFx() != null) {
            dto.setDividendIncomeAmountWithFx(
                (dto.getDividendIncomeAmountWithFx() == null ? 0 : dto.getDividendIncomeAmountWithFx()) + dividend.getAmountWithFx()
            );
        }
        if (dividend.getAmountWithoutFx() != null) {
            dto.setDividendIncomeAmountWithoutFx(
                (dto.getDividendIncomeAmountWithoutFx() == null ? 0 : dto.getDividendIncomeAmountWithoutFx()) +
                    dividend.getAmountWithoutFx()
            );
        }
    }

    private double eligibleQuantityOnDate(TradeContext context, LocalDate date) {
        if (context.transaction.getDate() == null || !context.transaction.getDate().isBefore(date)) {
            return 0;
        }

        double eligibleQuantity = context.transaction.getQuantity() == null ? 0 : Math.abs(context.transaction.getQuantity());
        if (eligibleQuantity <= 0) {
            return 0;
        }

        if (context.lots == null || context.lots.isEmpty()) {
            return eligibleQuantity;
        }

        for (FinanceLot lot : context.lots) {
            LocalDate sellDate = firstNonNull(lot.getSellDate(), lot.getCloseDate());
            if (sellDate == null || sellDate.isAfter(date)) {
                continue;
            }
            Double soldQuantity = positiveQuantity(lot.getQuantity());
            if (soldQuantity != null) {
                eligibleQuantity -= soldQuantity;
            }
        }
        return Math.max(0, eligibleQuantity);
    }

    private String accountSecurityKey(String accountId, String securityId) {
        return (accountId == null ? "" : accountId) + "|" + (securityId == null ? "" : securityId);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private Double absDouble(BigDecimal value) {
        return value == null ? null : Math.abs(value.doubleValue());
    }

    private void applyDeltaMetrics(FinancePortfolioTradeDTO dto) {
        Double quantity = dto.getQuantity();
        Double buyPrice = dto.getBuyPrice();
        Double currentPrice = dto.getCurrentPrice();
        if (quantity == null || buyPrice == null || currentPrice == null) {
            return;
        }

        double costWithoutFx = quantity * buyPrice;
        double currentWithoutFx = quantity * currentPrice;
        dto.setDeltaAmountWithoutFx(currentWithoutFx - costWithoutFx);
        dto.setDeltaPercentWithoutFx(costWithoutFx != 0 ? ((currentWithoutFx - costWithoutFx) / costWithoutFx) * 100 : null);

        Double buyFxRate = dto.getBuyFxRate();
        Double currentFxRate = dto.getCurrentFxRate();
        if (buyFxRate == null || currentFxRate == null) {
            return;
        }

        double costWithFx = costWithoutFx * buyFxRate;
        double currentWithFx = currentWithoutFx * currentFxRate;
        dto.setDeltaAmountWithFx(currentWithFx - costWithFx);
        dto.setDeltaPercentWithFx(costWithFx != 0 ? ((currentWithFx - costWithFx) / costWithFx) * 100 : null);
    }

    private void applyRealizedLotMetrics(
        FinancePortfolioTradeDTO dto,
        FinanceTransaction buyTransaction,
        User user,
        FinanceUserSecurity userSecurity,
        Map<UUID, List<FinanceLot>> lotsByBuyTransactionId,
        Map<UUID, FinanceTransaction> sellTransactions,
        Map<String, Map<LocalDate, Double>> historicalFxRates
    ) {
        if (buyTransaction.getId() == null || dto.getQuantity() == null || dto.getBuyPrice() == null) {
            return;
        }

        List<FinanceLot> lots = lotsByBuyTransactionId.get(buyTransaction.getId());
        if (lots == null || lots.isEmpty()) {
            return;
        }

        RealizedLotMetrics metrics = new RealizedLotMetrics();
        for (FinanceLot lot : lots) {
            UUID sellTransactionId = firstNonNull(lot.getSellTransactionId(), lot.getCloseTransactionId());
            if (sellTransactionId == null) {
                continue;
            }
            FinanceTransaction sellTransaction = sellTransactions.get(sellTransactionId);
            if (sellTransaction == null || sellTransaction.getPrice() == null) {
                continue;
            }

            Double lotQuantity = positiveQuantity(lot.getQuantity());
            if (lotQuantity == null) {
                lotQuantity = positiveQuantity(sellTransaction.getQuantity());
            }
            if (lotQuantity == null) {
                continue;
            }

            Double sellFxRate = resolveHistoricalFxRate(user, sellTransaction, userSecurity.getCurrencyCode(), historicalFxRates);
            metrics.add(
                lotQuantity,
                dto.getBuyPrice(),
                dto.getBuyFxRate(),
                sellTransaction.getPrice(),
                sellFxRate,
                buyTransaction.getDate(),
                firstNonNull(lot.getSellDate(), firstNonNull(lot.getCloseDate(), sellTransaction.getDate()))
            );
        }

        if (metrics.soldQuantity <= 0) {
            return;
        }

        double buyQuantity = Math.abs(dto.getQuantity());
        double openQuantity = Math.max(0, buyQuantity - metrics.soldQuantity);
        LocalDate buyDate = buyTransaction.getDate();
        if (openQuantity > 0 && buyDate != null) {
            metrics.addOpenHoldingPeriod(openQuantity, buyDate, LocalDate.now());
        }
        dto.setSold(true);
        dto.setSellQuantity(metrics.soldQuantity);
        dto.setOpenQuantity(openQuantity);
        dto.setSellDate(metrics.latestSellDate);
        dto.setSellPrice(metrics.soldQuantity > 0 ? metrics.weightedSellPrice / metrics.soldQuantity : null);
        dto.setSellFxRate(metrics.sellFxWeight > 0 ? metrics.weightedSellFxRate / metrics.sellFxWeight : null);
        dto.setHoldingYears(metrics.holdingQuantityWeight > 0 ? metrics.weightedHoldingYears / metrics.holdingQuantityWeight : null);

        double totalCostWithoutFx = buyQuantity * dto.getBuyPrice();
        dto.setRealizedDeltaAmountWithoutFx(metrics.realizedDeltaWithoutFx);
        dto.setRealizedDeltaPercentWithoutFx(totalCostWithoutFx != 0 ? (metrics.realizedDeltaWithoutFx / totalCostWithoutFx) * 100 : null);

        Double buyFxRate = dto.getBuyFxRate();
        if (buyFxRate != null && metrics.hasFx) {
            double totalCostWithFx = totalCostWithoutFx * buyFxRate;
            dto.setRealizedDeltaAmountWithFx(metrics.realizedDeltaWithFx);
            dto.setRealizedDeltaPercentWithFx(totalCostWithFx != 0 ? (metrics.realizedDeltaWithFx / totalCostWithFx) * 100 : null);
        }

        applyCombinedSoldAndOpenDelta(dto, openQuantity, metrics, totalCostWithoutFx);
    }

    private void applyCombinedSoldAndOpenDelta(
        FinancePortfolioTradeDTO dto,
        double openQuantity,
        RealizedLotMetrics metrics,
        double totalCostWithoutFx
    ) {
        double openDeltaWithoutFx = 0;
        if (openQuantity > 0 && dto.getCurrentPrice() != null && dto.getBuyPrice() != null) {
            openDeltaWithoutFx = (openQuantity * dto.getCurrentPrice()) - (openQuantity * dto.getBuyPrice());
        }
        double combinedDeltaWithoutFx = metrics.realizedDeltaWithoutFx + openDeltaWithoutFx;
        dto.setDeltaAmountWithoutFx(combinedDeltaWithoutFx);
        dto.setDeltaPercentWithoutFx(totalCostWithoutFx != 0 ? (combinedDeltaWithoutFx / totalCostWithoutFx) * 100 : null);

        Double buyFxRate = dto.getBuyFxRate();
        Double currentFxRate = dto.getCurrentFxRate();
        if (buyFxRate == null || (!metrics.hasFx && (openQuantity <= 0 || currentFxRate == null))) {
            return;
        }

        double openDeltaWithFx = 0;
        if (openQuantity > 0 && dto.getCurrentPrice() != null && dto.getBuyPrice() != null && currentFxRate != null) {
            double openCostWithFx = openQuantity * dto.getBuyPrice() * buyFxRate;
            double openCurrentWithFx = openQuantity * dto.getCurrentPrice() * currentFxRate;
            openDeltaWithFx = openCurrentWithFx - openCostWithFx;
        }

        double totalCostWithFx = totalCostWithoutFx * buyFxRate;
        double combinedDeltaWithFx = metrics.realizedDeltaWithFx + openDeltaWithFx;
        dto.setDeltaAmountWithFx(combinedDeltaWithFx);
        dto.setDeltaPercentWithFx(totalCostWithFx != 0 ? (combinedDeltaWithFx / totalCostWithFx) * 100 : null);
    }

    private Double positiveQuantity(Double quantity) {
        if (quantity == null || quantity == 0) {
            return null;
        }
        return Math.abs(quantity);
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private static class RealizedLotMetrics {

        private double soldQuantity;
        private double realizedDeltaWithoutFx;
        private double realizedDeltaWithFx;
        private double weightedSellPrice;
        private double weightedSellFxRate;
        private double sellFxWeight;
        private double weightedHoldingYears;
        private double holdingQuantityWeight;
        private boolean hasFx;
        private LocalDate latestSellDate;

        private void add(
            double quantity,
            double buyPrice,
            Double buyFxRate,
            double sellPrice,
            Double sellFxRate,
            LocalDate buyDate,
            LocalDate sellDate
        ) {
            double costWithoutFx = quantity * buyPrice;
            double proceedsWithoutFx = quantity * sellPrice;

            soldQuantity += quantity;
            realizedDeltaWithoutFx += proceedsWithoutFx - costWithoutFx;
            weightedSellPrice += sellPrice * quantity;
            addHoldingPeriod(quantity, buyDate, sellDate);
            if (sellDate != null && (latestSellDate == null || sellDate.isAfter(latestSellDate))) {
                latestSellDate = sellDate;
            }

            if (buyFxRate != null && sellFxRate != null) {
                double costWithFx = costWithoutFx * buyFxRate;
                double proceedsWithFx = proceedsWithoutFx * sellFxRate;
                realizedDeltaWithFx += proceedsWithFx - costWithFx;
                weightedSellFxRate += sellFxRate * quantity;
                sellFxWeight += quantity;
                hasFx = true;
            }
        }

        private void addOpenHoldingPeriod(double quantity, LocalDate buyDate, LocalDate asAtDate) {
            addHoldingPeriod(quantity, buyDate, asAtDate);
        }

        private void addHoldingPeriod(double quantity, LocalDate buyDate, LocalDate exitDate) {
            if (buyDate == null || exitDate == null) {
                return;
            }
            double years = Math.max(0, ChronoUnit.DAYS.between(buyDate, exitDate) / 365.25);
            weightedHoldingYears += years * quantity;
            holdingQuantityWeight += quantity;
        }
    }

    private static class TradeContext {

        private final FinancePortfolioTradeDTO dto;
        private final FinanceTransaction transaction;
        private final FinanceUserSecurity userSecurity;
        private final List<FinanceLot> lots;

        private TradeContext(
            FinancePortfolioTradeDTO dto,
            FinanceTransaction transaction,
            FinanceUserSecurity userSecurity,
            List<FinanceLot> lots
        ) {
            this.dto = dto;
            this.transaction = transaction;
            this.userSecurity = userSecurity;
            this.lots = lots;
        }
    }

    private record DividendAmount(String sourceCurrencyCode, Double sourceAmount, Double baseAmount, Double fxRate) {}
}
