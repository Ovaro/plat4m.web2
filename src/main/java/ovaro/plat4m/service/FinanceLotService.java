package ovaro.plat4m.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceInvestmentActivityType;
import ovaro.plat4m.domain.FinanceLot;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceLotRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.FinanceUserSecurityRepository;
import ovaro.plat4m.service.dto.FinanceLotGroupDTO;
import ovaro.plat4m.service.dto.FinanceLotViewDTO;

@Service
public class FinanceLotService {

    private static final int QUERY_ID_BATCH_SIZE = 10_000;

    private final FinanceLotRepository financeLotRepository;
    private final FinanceUserSecurityRepository financeUserSecurityRepository;
    private final FinanceAccountRepository financeAccountRepository;
    private final FinanceTransactionRepository financeTransactionRepository;

    public FinanceLotService(
        FinanceLotRepository financeLotRepository,
        FinanceUserSecurityRepository financeUserSecurityRepository,
        FinanceAccountRepository financeAccountRepository,
        FinanceTransactionRepository financeTransactionRepository
    ) {
        this.financeLotRepository = financeLotRepository;
        this.financeUserSecurityRepository = financeUserSecurityRepository;
        this.financeAccountRepository = financeAccountRepository;
        this.financeTransactionRepository = financeTransactionRepository;
    }

    public List<FinanceLotGroupDTO> getLots(User user, String userSecurityId) {
        FinanceUserSecurity security = this.financeUserSecurityRepository
            .findByIdAndUserGuid(UUID.fromString(userSecurityId), user.getGuid().toString())
            .orElseThrow(() -> new IllegalArgumentException("Investment not found"));

        List<FinanceLot> lots = this.financeLotRepository.findByUserGuidAndSecurityIdOrderByOpenDateAscIdAsc(
            user.getGuid().toString(),
            userSecurityId
        );
        Map<UUID, FinanceLot> lotsById = new HashMap<>();
        for (FinanceLot lot : lots) {
            lotsById.put(lot.getId(), lot);
        }

        Map<String, String> accountNames = new HashMap<>();
        for (FinanceAccount account : this.financeAccountRepository.findAllByUserGuid(user.getGuid().toString())) {
            accountNames.put(account.getId().toString(), account.getName());
        }
        Map<UUID, FinanceTransaction> transactions = loadTransactionsById(user, lots);

        Map<UUID, FinanceLotGroupDTO> groups = new HashMap<>();
        for (FinanceLot lot : lots) {
            FinanceLot root = resolveRootLot(lot, lotsById);
            FinanceLotGroupDTO group = groups.get(root.getId());
            if (group == null) {
                group = new FinanceLotGroupDTO();
                group.setOriginalLot(toView(root, root, accountNames, security.getName(), transactions));
                groups.put(root.getId(), group);
            }
            group.getLots().add(toView(lot, root, accountNames, security.getName(), transactions));
            if (lot.getCloseDate() == null) {
                FinanceLotViewDTO remaining = toView(lot, root, accountNames, security.getName(), transactions);
                if (group.getRemainingLot() == null || compareLotViews(remaining, group.getRemainingLot()) > 0) {
                    group.setRemainingLot(remaining);
                }
            }
        }

        List<FinanceLotGroupDTO> results = new ArrayList<>(groups.values());
        for (FinanceLotGroupDTO group : results) {
            group
                .getLots()
                .sort(
                    Comparator.comparing(FinanceLotViewDTO::getOpenDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FinanceLotViewDTO::getBuyDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FinanceLotViewDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                );
        }
        results.sort(
            Comparator.comparing(
                (FinanceLotGroupDTO group) -> group.getOriginalLot().getOpenDate(),
                Comparator.nullsLast(Comparator.naturalOrder())
            )
                .thenComparing(group -> group.getOriginalLot().getBuyDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(group -> group.getOriginalLot().getId(), Comparator.nullsLast(Comparator.naturalOrder()))
        );
        return results;
    }

    public Map<UUID, BigDecimal> getRealisedGainLossBySellTransaction(User user, Collection<FinanceTransaction> transactions) {
        Set<UUID> sellTransactionIds = new HashSet<>();
        for (FinanceTransaction transaction : transactions) {
            if (isSellInvestmentTransaction(transaction)) {
                sellTransactionIds.add(transaction.getId());
            }
        }
        if (sellTransactionIds.isEmpty()) {
            return Map.of();
        }

        List<FinanceLot> lots = findSellLotsByTransactionIds(user, sellTransactionIds);
        Map<UUID, FinanceTransaction> transactionsById = loadTransactionsById(user, lots);
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (FinanceLot lot : lots) {
            UUID transactionId = lot.getSellTransactionId() != null ? lot.getSellTransactionId() : lot.getCloseTransactionId();
            if (transactionId == null || !sellTransactionIds.contains(transactionId)) {
                continue;
            }
            BigDecimal gainLoss = calculateRealisedGainLossForReport(lot, transactionsById);
            if (gainLoss != null) {
                result.merge(transactionId, gainLoss, BigDecimal::add);
            }
        }
        return result;
    }

    public List<FinanceLotViewDTO> getLotsForSellTransaction(User user, UUID transactionId) {
        List<FinanceLot> lots = this.financeLotRepository.findSellLotsByUserGuidAndTransactionId(user.getGuid().toString(), transactionId);
        if (lots.isEmpty()) {
            return List.of();
        }

        Map<UUID, FinanceLot> lotsById = new HashMap<>();
        for (FinanceLot lot : lots) {
            lotsById.put(lot.getId(), lot);
        }
        Set<UUID> rootIds = new HashSet<>();
        for (FinanceLot lot : lots) {
            addIfPresent(rootIds, lot.getLotOpenId());
        }
        if (!rootIds.isEmpty()) {
            for (FinanceLot rootLot : this.financeLotRepository.findAllByUserGuidAndIdIn(user.getGuid().toString(), rootIds)) {
                lotsById.put(rootLot.getId(), rootLot);
            }
        }

        Map<String, String> accountNames = new HashMap<>();
        for (FinanceAccount account : this.financeAccountRepository.findAllByUserGuid(user.getGuid().toString())) {
            accountNames.put(account.getId().toString(), account.getName());
        }
        Map<String, String> securityNames = resolveSecurityNames(user, lots);
        Map<UUID, FinanceTransaction> transactions = loadTransactionsById(user, new ArrayList<>(lotsById.values()));

        List<FinanceLotViewDTO> result = new ArrayList<>();
        String baseCurrencyCode = resolveBaseCurrencyCode(user);
        for (FinanceLot lot : lots) {
            FinanceLot root = resolveRootLot(lot, lotsById);
            result.add(
                toView(
                    lot,
                    root,
                    accountNames,
                    securityNames.getOrDefault(lot.getSecurityId(), lot.getSecurityId()),
                    transactions,
                    true,
                    baseCurrencyCode
                )
            );
        }
        return result;
    }

    private Map<UUID, FinanceTransaction> loadTransactionsById(User user, List<FinanceLot> lots) {
        Set<UUID> ids = new HashSet<>();
        for (FinanceLot lot : lots) {
            addIfPresent(ids, lot.getBuyTransactionId());
            addIfPresent(ids, lot.getSellTransactionId());
            addIfPresent(ids, lot.getOpenTransactionId());
            addIfPresent(ids, lot.getCloseTransactionId());
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<FinanceTransaction> transactions = findTransactionsByIds(user, ids);
        Map<UUID, FinanceTransaction> result = new HashMap<>();
        for (FinanceTransaction transaction : transactions) {
            result.put(transaction.getId(), transaction);
        }
        return result;
    }

    private List<FinanceLot> findSellLotsByTransactionIds(User user, Collection<UUID> transactionIds) {
        Map<UUID, FinanceLot> lotsById = new HashMap<>();
        for (List<UUID> batch : partitionIds(transactionIds)) {
            this.financeLotRepository.findSellLotsByUserGuidAndTransactionIds(user.getGuid().toString(), batch).forEach(lot -> {
                if (lot.getId() != null) {
                    lotsById.putIfAbsent(lot.getId(), lot);
                }
            });
        }
        return new ArrayList<>(lotsById.values());
    }

    private List<FinanceTransaction> findTransactionsByIds(User user, Collection<UUID> transactionIds) {
        List<FinanceTransaction> transactions = new ArrayList<>();
        for (List<UUID> batch : partitionIds(transactionIds)) {
            transactions.addAll(this.financeTransactionRepository.findAllByUserGuidAndIdIn(user.getGuid().toString(), batch));
        }
        return transactions;
    }

    private List<FinanceUserSecurity> findSecuritiesByIds(User user, Collection<UUID> securityIds) {
        List<FinanceUserSecurity> securities = new ArrayList<>();
        for (List<UUID> batch : partitionIds(securityIds)) {
            securities.addAll(this.financeUserSecurityRepository.findAllByUserGuidAndIdIn(user.getGuid().toString(), batch));
        }
        return securities;
    }

    private List<List<UUID>> partitionIds(Collection<UUID> ids) {
        List<UUID> normalizedIds = new ArrayList<>();
        Set<UUID> seenIds = new HashSet<>();
        for (UUID id : ids) {
            if (id != null && seenIds.add(id)) {
                normalizedIds.add(id);
            }
        }

        List<List<UUID>> batches = new ArrayList<>();
        for (int index = 0; index < normalizedIds.size(); index += QUERY_ID_BATCH_SIZE) {
            batches.add(normalizedIds.subList(index, Math.min(index + QUERY_ID_BATCH_SIZE, normalizedIds.size())));
        }
        return batches;
    }

    private void addIfPresent(Collection<UUID> ids, UUID id) {
        if (id != null) {
            ids.add(id);
        }
    }

    private int compareLotViews(FinanceLotViewDTO left, FinanceLotViewDTO right) {
        Comparator<FinanceLotViewDTO> comparator = Comparator.comparing(
            FinanceLotViewDTO::getOpenDate,
            Comparator.nullsLast(Comparator.naturalOrder())
        )
            .thenComparing(FinanceLotViewDTO::getBuyDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(FinanceLotViewDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        return comparator.compare(left, right);
    }

    private FinanceLot resolveRootLot(FinanceLot lot, Map<UUID, FinanceLot> lotsById) {
        FinanceLot current = lot;
        while (current.getLotOpenId() != null) {
            FinanceLot parent = lotsById.get(current.getLotOpenId());
            if (parent == null || parent.getId().equals(current.getId())) {
                break;
            }
            current = parent;
        }
        return current;
    }

    private FinanceLotViewDTO toView(
        FinanceLot lot,
        FinanceLot originalLot,
        Map<String, String> accountNames,
        String securityName,
        Map<UUID, FinanceTransaction> transactions
    ) {
        return toView(lot, originalLot, accountNames, securityName, transactions, false, null);
    }

    private FinanceLotViewDTO toView(
        FinanceLot lot,
        FinanceLot originalLot,
        Map<String, String> accountNames,
        String securityName,
        Map<UUID, FinanceTransaction> transactions,
        boolean baseCurrencyAmounts,
        String baseCurrencyCode
    ) {
        FinanceLotViewDTO dto = new FinanceLotViewDTO();
        dto.setId(lot.getId().toString());
        dto.setSourceId(lot.getSourceId());
        dto.setQuantity(lot.getQuantity());
        dto.setLotType(lot.getLotType());
        dto.setAccountId(lot.getAccountId());
        dto.setAccountName(lot.getAccountId() != null ? accountNames.get(lot.getAccountId()) : null);
        dto.setSecurityId(lot.getSecurityId());
        dto.setSecurityName(securityName);
        dto.setBuyDate(lot.getBuyDate());
        dto.setSellDate(lot.getSellDate());
        dto.setOpenDate(lot.getOpenDate());
        dto.setCloseDate(lot.getCloseDate());
        dto.setBuyTransactionId(lot.getBuyTransactionId() != null ? lot.getBuyTransactionId().toString() : null);
        dto.setSellTransactionId(lot.getSellTransactionId() != null ? lot.getSellTransactionId().toString() : null);
        dto.setOpenTransactionId(lot.getOpenTransactionId() != null ? lot.getOpenTransactionId().toString() : null);
        dto.setCloseTransactionId(lot.getCloseTransactionId() != null ? lot.getCloseTransactionId().toString() : null);
        applyOriginalLotContext(dto, lot, originalLot, transactions, baseCurrencyAmounts);
        applyTaxLotTransactionDetails(dto, lot, transactions, baseCurrencyAmounts, baseCurrencyCode);
        applyRealisedGainLoss(dto, lot, transactions, baseCurrencyAmounts);
        return dto;
    }

    private void applyTaxLotTransactionDetails(
        FinanceLotViewDTO dto,
        FinanceLot lot,
        Map<UUID, FinanceTransaction> transactions,
        boolean baseCurrencyAmounts,
        String baseCurrencyCode
    ) {
        FinanceTransaction buyTransaction = getBuyTransaction(lot, transactions);
        FinanceTransaction sellTransaction = getSellTransaction(lot, transactions);

        if (buyTransaction != null) {
            dto.setBuyPrice(resolveTransactionPrice(buyTransaction, baseCurrencyAmounts));
            dto.setBuyFxRate(buyTransaction.getRateToBase());
            dto.setBuyCurrencyCode(baseCurrencyAmounts ? baseCurrencyCode : buyTransaction.getCurrencyCode());
            dto.setBuyCharges(proratedCharges(buyTransaction, lot.getQuantity(), baseCurrencyAmounts));
        }

        if (sellTransaction != null) {
            dto.setSellPrice(resolveTransactionPrice(sellTransaction, baseCurrencyAmounts));
            dto.setSellFxRate(sellTransaction.getRateToBase());
            dto.setSellCurrencyCode(baseCurrencyAmounts ? baseCurrencyCode : sellTransaction.getCurrencyCode());
            dto.setSellCharges(proratedCharges(sellTransaction, lot.getQuantity(), baseCurrencyAmounts));
        }
    }

    private void applyOriginalLotContext(
        FinanceLotViewDTO dto,
        FinanceLot lot,
        FinanceLot originalLot,
        Map<UUID, FinanceTransaction> transactions,
        boolean baseCurrencyAmounts
    ) {
        LocalDate originalBuyDate = originalLot.getBuyDate() != null ? originalLot.getBuyDate() : originalLot.getOpenDate();
        Double originalPrice = resolveOriginalPrice(originalLot, transactions, baseCurrencyAmounts);
        Double displayQuantity = baseCurrencyAmounts
            ? firstNonZero(lot.getQuantity(), originalLot.getQuantity())
            : originalLot.getQuantity();
        dto.setOriginalLotId(originalLot.getId() != null ? originalLot.getId().toString() : null);
        dto.setOriginalSourceId(originalLot.getSourceId());
        dto.setOriginalBuyDate(originalBuyDate);
        dto.setOriginalQuantity(originalLot.getQuantity());
        dto.setOriginalPrice(originalPrice);
        dto.setLotKey(buildLotKey(originalBuyDate, displayQuantity, originalPrice));
    }

    private Double resolveOriginalPrice(FinanceLot originalLot, Map<UUID, FinanceTransaction> transactions, boolean baseCurrencyAmounts) {
        FinanceTransaction transaction = transactions.get(originalLot.getBuyTransactionId());
        if (transaction == null) {
            transaction = transactions.get(originalLot.getOpenTransactionId());
        }
        if (transaction != null) {
            Double price = resolveTransactionPrice(transaction, baseCurrencyAmounts);
            if (price != null) {
                return price;
            }
        }
        if (transaction == null || originalLot.getQuantity() == null || originalLot.getQuantity() == 0.0) {
            return null;
        }
        BigDecimal amount = baseCurrencyAmounts
            ? reportAmount(transaction)
            : transaction.getAmount() == null
                ? null
                : transaction.getAmount().abs();
        return amount == null
            ? null
            : amount.divide(BigDecimal.valueOf(Math.abs(originalLot.getQuantity())), MathContext.DECIMAL64).doubleValue();
    }

    private String buildLotKey(LocalDate buyDate, Double quantity, Double price) {
        return String.format(
            "%s | %s @ %s",
            buyDate != null ? buyDate : "unknown date",
            quantity != null ? quantity : "unknown quantity",
            price != null ? price : "unknown price"
        );
    }

    private Double firstNonZero(Double preferred, Double fallback) {
        if (preferred != null && Double.compare(preferred, 0d) != 0) {
            return preferred;
        }
        return fallback;
    }

    private void applyRealisedGainLoss(
        FinanceLotViewDTO dto,
        FinanceLot lot,
        Map<UUID, FinanceTransaction> transactions,
        boolean baseCurrencyAmounts
    ) {
        BigDecimal gainLoss = baseCurrencyAmounts
            ? calculateRealisedGainLossForReport(lot, transactions)
            : calculateRealisedGainLoss(lot, transactions);
        if (gainLoss == null) {
            return;
        }

        dto.setCostBasis(baseCurrencyAmounts ? calculateReportCostBasis(lot, transactions) : calculateCostBasis(lot, transactions));
        dto.setSaleProceeds(
            baseCurrencyAmounts ? calculateReportSaleProceeds(lot, transactions) : calculateSaleProceeds(lot, transactions)
        );
        dto.setRealisedGainLoss(gainLoss);
    }

    private BigDecimal calculateRealisedGainLoss(FinanceLot lot, Map<UUID, FinanceTransaction> transactions) {
        BigDecimal costBasis = calculateCostBasis(lot, transactions);
        BigDecimal saleProceeds = calculateSaleProceeds(lot, transactions);
        if (costBasis == null || saleProceeds == null) {
            return null;
        }
        return saleProceeds.subtract(costBasis);
    }

    private BigDecimal calculateRealisedGainLossForReport(FinanceLot lot, Map<UUID, FinanceTransaction> transactions) {
        BigDecimal costBasis = calculateReportCostBasis(lot, transactions);
        BigDecimal saleProceeds = calculateReportSaleProceeds(lot, transactions);
        if (costBasis == null || saleProceeds == null) {
            return null;
        }
        return saleProceeds.subtract(costBasis);
    }

    private BigDecimal calculateCostBasis(FinanceLot lot, Map<UUID, FinanceTransaction> transactions) {
        FinanceTransaction buyTransaction = getBuyTransaction(lot, transactions);
        return buyTransaction == null ? null : proratedAmount(buyTransaction, lot.getQuantity());
    }

    private BigDecimal calculateSaleProceeds(FinanceLot lot, Map<UUID, FinanceTransaction> transactions) {
        FinanceTransaction sellTransaction = getSellTransaction(lot, transactions);
        return sellTransaction == null ? null : proratedAmount(sellTransaction, lot.getQuantity());
    }

    private BigDecimal calculateReportCostBasis(FinanceLot lot, Map<UUID, FinanceTransaction> transactions) {
        FinanceTransaction buyTransaction = getBuyTransaction(lot, transactions);
        return buyTransaction == null ? null : proratedReportAmount(buyTransaction, lot.getQuantity());
    }

    private BigDecimal calculateReportSaleProceeds(FinanceLot lot, Map<UUID, FinanceTransaction> transactions) {
        FinanceTransaction sellTransaction = getSellTransaction(lot, transactions);
        return sellTransaction == null ? null : proratedReportAmount(sellTransaction, lot.getQuantity());
    }

    private FinanceTransaction getBuyTransaction(FinanceLot lot, Map<UUID, FinanceTransaction> transactions) {
        FinanceTransaction buyTransaction = transactions.get(lot.getBuyTransactionId());
        return buyTransaction != null ? buyTransaction : transactions.get(lot.getOpenTransactionId());
    }

    private FinanceTransaction getSellTransaction(FinanceLot lot, Map<UUID, FinanceTransaction> transactions) {
        FinanceTransaction sellTransaction = transactions.get(lot.getSellTransactionId());
        return sellTransaction != null ? sellTransaction : transactions.get(lot.getCloseTransactionId());
    }

    private boolean isSellInvestmentTransaction(FinanceTransaction transaction) {
        return (
            transaction != null &&
            transaction.getId() != null &&
            transaction.getSecurityId() != null &&
            transaction.getInvestmentActivityType() == FinanceInvestmentActivityType.SELL
        );
    }

    private Map<String, String> resolveSecurityNames(User user, List<FinanceLot> lots) {
        Set<UUID> securityIds = new HashSet<>();
        for (FinanceLot lot : lots) {
            if (lot.getSecurityId() == null) {
                continue;
            }
            try {
                securityIds.add(UUID.fromString(lot.getSecurityId()));
            } catch (IllegalArgumentException ignored) {
                // Keep the raw security id as a fallback label if it is not a UUID.
            }
        }
        if (securityIds.isEmpty()) {
            return Map.of();
        }

        Map<String, String> result = new HashMap<>();
        for (FinanceUserSecurity security : findSecuritiesByIds(user, securityIds)) {
            result.put(security.getId().toString(), security.getName());
        }
        return result;
    }

    private BigDecimal proratedAmount(FinanceTransaction transaction, Double lotQuantity) {
        if (transaction.getAmount() == null || lotQuantity == null) {
            return null;
        }
        BigDecimal amount = transaction.getAmount().abs();
        if (transaction.getQuantity() == null || transaction.getQuantity() == 0.0) {
            return amount;
        }
        BigDecimal ratio = BigDecimal.valueOf(Math.abs(lotQuantity)).divide(
            BigDecimal.valueOf(Math.abs(transaction.getQuantity())),
            MathContext.DECIMAL64
        );
        return amount.multiply(ratio, MathContext.DECIMAL64);
    }

    private BigDecimal proratedReportAmount(FinanceTransaction transaction, Double lotQuantity) {
        BigDecimal amount = reportAmount(transaction);
        if (amount == null || lotQuantity == null) {
            return null;
        }
        if (transaction.getQuantity() == null || transaction.getQuantity() == 0.0) {
            return amount;
        }
        BigDecimal ratio = BigDecimal.valueOf(Math.abs(lotQuantity)).divide(
            BigDecimal.valueOf(Math.abs(transaction.getQuantity())),
            MathContext.DECIMAL64
        );
        return amount.multiply(ratio, MathContext.DECIMAL64);
    }

    private BigDecimal proratedCharges(FinanceTransaction transaction, Double lotQuantity, boolean baseCurrencyAmounts) {
        if (
            transaction.getAmount() == null ||
            transaction.getPrice() == null ||
            transaction.getQuantity() == null ||
            transaction.getQuantity() == 0.0 ||
            lotQuantity == null
        ) {
            return null;
        }

        BigDecimal grossTradeAmount = BigDecimal.valueOf(Math.abs(transaction.getQuantity())).multiply(
            BigDecimal.valueOf(Math.abs(transaction.getPrice())),
            MathContext.DECIMAL64
        );
        BigDecimal totalAmount = baseCurrencyAmounts ? reportAmount(transaction) : transaction.getAmount().abs();
        if (totalAmount == null) {
            return null;
        }
        if (baseCurrencyAmounts) {
            BigDecimal rateToBase = resolveRateToBase(transaction);
            if (rateToBase != null) {
                grossTradeAmount = grossTradeAmount.multiply(rateToBase, MathContext.DECIMAL64);
            }
        }
        BigDecimal totalCharges = totalAmount.subtract(grossTradeAmount).abs();
        BigDecimal ratio = BigDecimal.valueOf(Math.abs(lotQuantity)).divide(
            BigDecimal.valueOf(Math.abs(transaction.getQuantity())),
            MathContext.DECIMAL64
        );
        return totalCharges.multiply(ratio, MathContext.DECIMAL64);
    }

    private Double resolveTransactionPrice(FinanceTransaction transaction, boolean baseCurrencyAmounts) {
        if (transaction.getPrice() != null) {
            BigDecimal price = BigDecimal.valueOf(transaction.getPrice());
            if (baseCurrencyAmounts) {
                BigDecimal rateToBase = resolveRateToBase(transaction);
                if (rateToBase != null) {
                    price = price.multiply(rateToBase, MathContext.DECIMAL64);
                }
            }
            return price.doubleValue();
        }
        if (transaction.getQuantity() == null || transaction.getQuantity() == 0.0) {
            return null;
        }
        BigDecimal amount = baseCurrencyAmounts
            ? reportAmount(transaction)
            : transaction.getAmount() == null
                ? null
                : transaction.getAmount().abs();
        return amount == null
            ? null
            : amount.divide(BigDecimal.valueOf(Math.abs(transaction.getQuantity())), MathContext.DECIMAL64).doubleValue();
    }

    private BigDecimal resolveRateToBase(FinanceTransaction transaction) {
        if (transaction.getRateToBase() != null && Double.compare(transaction.getRateToBase(), 0d) > 0) {
            return BigDecimal.valueOf(transaction.getRateToBase());
        }
        if (
            transaction.getAmountBase() == null ||
            transaction.getAmount() == null ||
            BigDecimal.ZERO.compareTo(transaction.getAmount()) == 0
        ) {
            return null;
        }
        return transaction.getAmountBase().abs().divide(transaction.getAmount().abs(), MathContext.DECIMAL64);
    }

    private String resolveBaseCurrencyCode(User user) {
        return user.getLocalCurrency() == null || user.getLocalCurrency().isBlank() ? "AUD" : user.getLocalCurrency();
    }

    private BigDecimal reportAmount(FinanceTransaction transaction) {
        if (transaction.getAmountBase() != null) {
            return transaction.getAmountBase().abs();
        }
        if (transaction.getAmount() != null && transaction.getRateToBase() != null && Double.compare(transaction.getRateToBase(), 0d) > 0) {
            return transaction.getAmount().multiply(BigDecimal.valueOf(transaction.getRateToBase())).abs();
        }
        return transaction.getAmount() == null ? null : transaction.getAmount().abs();
    }
}
