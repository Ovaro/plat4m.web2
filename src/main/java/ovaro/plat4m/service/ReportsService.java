package ovaro.plat4m.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceAccountType;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceTransferLink;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.domain.UserReportConfig;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.FinanceTransferLinkRepository;
import ovaro.plat4m.repository.FinanceUserSecurityRepository;
import ovaro.plat4m.repository.UserReportConfigRepository;
import ovaro.plat4m.repository.UserRepository;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.dto.IncomeExpenseReportDrilldownDTO;
import ovaro.plat4m.service.dto.IncomeExpenseReportDrilldownRequestDTO;
import ovaro.plat4m.service.dto.IncomeExpenseReportResultDTO;
import ovaro.plat4m.service.dto.ReportConfigDTO;
import ovaro.plat4m.service.dto.ReportDefinitionDTO;
import ovaro.plat4m.web.rest.errors.BadRequestAlertException;

@Service
@Transactional
public class ReportsService {

    public static final String REPORT_KEY_INCOME_EXPENSES = "income-expenses";

    private static final int CATEGORY_CLASSIFICATION_ID = 0;
    private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    private final UserReportConfigRepository userReportConfigRepository;
    private final UserRepository userRepository;
    private final FinanceAccountRepository financeAccountRepository;
    private final FinanceTransactionRepository financeTransactionRepository;
    private final FinanceTransferLinkRepository financeTransferLinkRepository;
    private final FinanceUserSecurityRepository financeUserSecurityRepository;
    private final ObjectMapper objectMapper;

    public ReportsService(
        UserReportConfigRepository userReportConfigRepository,
        UserRepository userRepository,
        FinanceAccountRepository financeAccountRepository,
        FinanceTransactionRepository financeTransactionRepository,
        FinanceTransferLinkRepository financeTransferLinkRepository,
        FinanceUserSecurityRepository financeUserSecurityRepository,
        ObjectMapper objectMapper
    ) {
        this.userReportConfigRepository = userReportConfigRepository;
        this.userRepository = userRepository;
        this.financeAccountRepository = financeAccountRepository;
        this.financeTransactionRepository = financeTransactionRepository;
        this.financeTransferLinkRepository = financeTransferLinkRepository;
        this.financeUserSecurityRepository = financeUserSecurityRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ReportDefinitionDTO> getDefinitions() {
        ReportDefinitionDTO definition = new ReportDefinitionDTO();
        definition.setKey(REPORT_KEY_INCOME_EXPENSES);
        definition.setTitle("Income & Expenses");
        definition.setDescription("Review income and expenses with configurable rows, columns, filters, and views.");
        definition.setDefaultConfig(createDefaultIncomeExpenseConfig());
        return List.of(definition);
    }

    @Transactional(readOnly = true)
    public List<ReportConfigDTO> getConfigs(String reportKey) {
        String normalizedKey = normalizeReportKey(reportKey);
        String login = getCurrentUserLogin();
        return userReportConfigRepository
            .findByUserLoginAndReportKeyOrderByNameAsc(login, normalizedKey)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public ReportConfigDTO createConfig(ReportConfigDTO dto) {
        String login = getCurrentUserLogin();
        User user = getCurrentUser(login);
        String reportKey = normalizeReportKey(dto.getReportKey());
        String name = normalizeName(dto.getName());
        validateEditablePayload(dto, true);
        ensureUniqueName(login, reportKey, name, null);

        UserReportConfig entity = new UserReportConfig();
        entity.setId(UUID.randomUUID());
        entity.setUser(user);
        entity.setReportKey(reportKey);
        entity.setName(name);
        entity.setConfigJson(writeConfigJson(dto));
        try {
            return toDto(userReportConfigRepository.save(entity));
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestAlertException("A configuration with that name already exists", "reports", "duplicatename");
        }
    }

    public ReportConfigDTO updateConfig(UUID id, ReportConfigDTO dto) {
        String login = getCurrentUserLogin();
        UserReportConfig entity = userReportConfigRepository
            .findOneByIdAndUserLogin(id, login)
            .orElseThrow(() -> new BadRequestAlertException("Report configuration not found", "reports", "confignotfound"));
        String reportKey = normalizeReportKey(dto.getReportKey());
        String name = normalizeName(dto.getName());
        validateEditablePayload(dto, false);
        ensureUniqueName(login, reportKey, name, entity.getId());

        entity.setReportKey(reportKey);
        entity.setName(name);
        entity.setConfigJson(writeConfigJson(dto));
        try {
            return toDto(userReportConfigRepository.save(entity));
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestAlertException("A configuration with that name already exists", "reports", "duplicatename");
        }
    }

    @Transactional(readOnly = true)
    public IncomeExpenseReportResultDTO runIncomeExpenseReport(ReportConfigDTO config) {
        validateRunPayload(config);
        User user = getCurrentUser(getCurrentUserLogin());
        ReportConfigDTO normalizedConfig = normalizeForExecution(config);
        LocalDateRange dateRange = resolveDateRange(
            normalizedConfig.getDatePreset(),
            normalizedConfig.getStartDate(),
            normalizedConfig.getEndDate()
        );

        Set<String> selectedCategoryIds = new LinkedHashSet<>(normalizedConfig.getCategoryIds());
        Set<String> selectedFamilyMemberIds = new LinkedHashSet<>(normalizedConfig.getFamilyMemberIds());
        Set<String> selectedAccountIds = new LinkedHashSet<>(normalizedConfig.getAccountIds());
        Set<String> selectedPayeeIds = new LinkedHashSet<>(normalizedConfig.getPayeeIds());
        Map<String, Integer> accountTypesById = getAccountTypesById(user.getGuid().toString());

        List<FinanceTransaction> rawTransactions = financeTransactionRepository.findReportTransactions(
            user.getGuid().toString(),
            dateRange.start(),
            dateRange.end()
        );
        Map<UUID, BigDecimal> transferDerivedBaseAmounts = resolveTransferDerivedBaseAmounts(
            user.getGuid().toString(),
            user.getLocalCurrency(),
            rawTransactions
        );
        Map<UUID, FinanceTransaction> transferCounterpartsByTransactionId = resolveTransferCounterpartsByTransactionId(
            user.getGuid().toString(),
            rawTransactions
        );
        Map<String, String> securityNamesById = resolveSecurityNamesById(
            user.getGuid().toString(),
            rawTransactions,
            transferCounterpartsByTransactionId
        );
        List<ReportTransaction> transactions = rawTransactions
            .stream()
            .map(transaction ->
                mapReportTransaction(
                    transaction,
                    accountTypesById,
                    transferDerivedBaseAmounts,
                    transferCounterpartsByTransactionId,
                    securityNamesById
                )
            )
            .filter(Objects::nonNull)
            .filter(transaction ->
                matchesFilters(transaction, selectedAccountIds, selectedCategoryIds, selectedPayeeIds, selectedFamilyMemberIds)
            )
            .toList();

        List<Bucket> buckets = buildBuckets(dateRange.start(), dateRange.end(), normalizedConfig.getColumnDimension());
        AggregationContext context = aggregateRows(normalizedConfig.getRowDimension(), buckets, transactions);
        return buildResult(normalizedConfig, user, dateRange, buckets, context);
    }

    @Transactional(readOnly = true)
    public IncomeExpenseReportDrilldownDTO getIncomeExpenseDrilldown(IncomeExpenseReportDrilldownRequestDTO request) {
        if (request == null || request.getConfig() == null) {
            throw new BadRequestAlertException("A report configuration is required", "reports", "missingconfig");
        }
        if (request.getRowKey() == null || request.getRowKey().isBlank()) {
            throw new BadRequestAlertException("A report row is required", "reports", "missingrowkey");
        }

        ReportConfigDTO config = normalizeForExecution(request.getConfig());
        validateRunPayload(config);
        User user = getCurrentUser(getCurrentUserLogin());
        LocalDateRange dateRange = resolveDateRange(config.getDatePreset(), config.getStartDate(), config.getEndDate());

        Set<String> selectedCategoryIds = new LinkedHashSet<>(config.getCategoryIds());
        Set<String> selectedFamilyMemberIds = new LinkedHashSet<>(config.getFamilyMemberIds());
        Set<String> selectedAccountIds = new LinkedHashSet<>(config.getAccountIds());
        Set<String> selectedPayeeIds = new LinkedHashSet<>(config.getPayeeIds());
        Map<String, Integer> accountTypesById = getAccountTypesById(user.getGuid().toString());

        List<FinanceTransaction> rawTransactions = financeTransactionRepository
            .findReportTransactions(user.getGuid().toString(), dateRange.start(), dateRange.end())
            .stream()
            .toList();
        Map<UUID, BigDecimal> transferDerivedBaseAmounts = resolveTransferDerivedBaseAmounts(
            user.getGuid().toString(),
            user.getLocalCurrency(),
            rawTransactions
        );
        Map<UUID, FinanceTransaction> transferCounterpartsByTransactionId = resolveTransferCounterpartsByTransactionId(
            user.getGuid().toString(),
            rawTransactions
        );
        Map<String, String> securityNamesById = resolveSecurityNamesById(
            user.getGuid().toString(),
            rawTransactions,
            transferCounterpartsByTransactionId
        );

        List<ReportTransaction> mappedTransactions = rawTransactions
            .stream()
            .map(transaction ->
                mapReportTransaction(
                    transaction,
                    accountTypesById,
                    transferDerivedBaseAmounts,
                    transferCounterpartsByTransactionId,
                    securityNamesById
                )
            )
            .filter(Objects::nonNull)
            .filter(transaction ->
                matchesFilters(transaction, selectedAccountIds, selectedCategoryIds, selectedPayeeIds, selectedFamilyMemberIds)
            )
            .toList();

        List<Bucket> buckets = buildBuckets(dateRange.start(), dateRange.end(), config.getColumnDimension());
        String columnKey = request.getColumnKey() == null || request.getColumnKey().isBlank() ? null : request.getColumnKey().trim();

        List<IncomeExpenseReportDrilldownDTO.TransactionDTO> drilldownTransactions = mappedTransactions
            .stream()
            .filter(transaction -> matchesRowSelection(transaction, config.getRowDimension(), request.getRowKey()))
            .filter(transaction -> matchesBucketSelection(transaction, buckets, columnKey))
            .map(transaction -> mapDrilldownTransaction(transaction, request.getRowKey()))
            .sorted(
                Comparator.comparing(IncomeExpenseReportDrilldownDTO.TransactionDTO::getDate)
                    .thenComparing(transaction -> defaultIfBlank(transaction.getPayeeName(), ""))
                    .thenComparing(transaction -> defaultIfBlank(transaction.getCategoryName(), ""))
            )
            .toList();

        BigDecimal total = drilldownTransactions
            .stream()
            .map(IncomeExpenseReportDrilldownDTO.TransactionDTO::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        IncomeExpenseReportDrilldownDTO response = new IncomeExpenseReportDrilldownDTO();
        response.setTitle(
            defaultIfBlank(request.getRowLabel(), "Transactions") + " - " + defaultIfBlank(request.getColumnLabel(), "Total")
        );
        response.setRowKey(request.getRowKey());
        response.setRowLabel(defaultIfBlank(request.getRowLabel(), "Transactions"));
        response.setColumnKey(columnKey);
        response.setColumnLabel(defaultIfBlank(request.getColumnLabel(), "Total"));
        response.setCurrencyCode(user.getLocalCurrency() == null || user.getLocalCurrency().isBlank() ? "AUD" : user.getLocalCurrency());
        response.setTransactions(drilldownTransactions);
        response.setTotal(total);
        return response;
    }

    private IncomeExpenseReportResultDTO buildResult(
        ReportConfigDTO config,
        User user,
        LocalDateRange dateRange,
        List<Bucket> buckets,
        AggregationContext context
    ) {
        IncomeExpenseReportResultDTO result = new IncomeExpenseReportResultDTO();
        result.setTitle(config.getTitle());
        result.setCurrencyCode(user.getLocalCurrency() == null || user.getLocalCurrency().isBlank() ? "AUD" : user.getLocalCurrency());
        result.setRowDimension(config.getRowDimension());
        result.setColumnDimension(config.getColumnDimension());
        result.setDefaultView(config.getDefaultView());
        result.setShowPercentOfTotal(config.isShowPercentOfTotal());
        result.setStartDate(dateRange.start());
        result.setEndDate(dateRange.end());
        result.setGrandTotal(context.grandTotal());

        List<BigDecimal> columnTotals = new ArrayList<>(Collections.nCopies(buckets.size(), BigDecimal.ZERO));
        List<BigDecimal> incomeColumnTotals = new ArrayList<>(Collections.nCopies(buckets.size(), BigDecimal.ZERO));
        List<BigDecimal> expenseColumnTotals = new ArrayList<>(Collections.nCopies(buckets.size(), BigDecimal.ZERO));
        BigDecimal incomeTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;
        for (AggregatedRow row : context.leafRows()) {
            for (int i = 0; i < row.values().size(); i++) {
                columnTotals.set(i, columnTotals.get(i).add(row.values().get(i)));
                if (isIncomeRow(row)) {
                    incomeColumnTotals.set(i, incomeColumnTotals.get(i).add(row.values().get(i)));
                } else if (isExpenseRow(row)) {
                    expenseColumnTotals.set(i, expenseColumnTotals.get(i).add(row.values().get(i)));
                }
            }
            if (isIncomeRow(row)) {
                incomeTotal = incomeTotal.add(row.total());
            } else if (isExpenseRow(row)) {
                expenseTotal = expenseTotal.add(row.total());
            }
        }

        List<IncomeExpenseReportResultDTO.ColumnDTO> columns = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            Bucket bucket = buckets.get(i);
            IncomeExpenseReportResultDTO.ColumnDTO column = new IncomeExpenseReportResultDTO.ColumnDTO();
            column.setKey(bucket.key());
            column.setLabel(bucket.label());
            column.setStartDate(bucket.start());
            column.setEndDate(bucket.end());
            column.setTotal(columnTotals.get(i));
            columns.add(column);
        }
        result.setColumns(columns);

        List<IncomeExpenseReportResultDTO.RowDTO> rows = new ArrayList<>();
        String currentSection = null;
        for (AggregatedRow aggregatedRow : context.displayRows()) {
            String rowSection = normalizeSectionLabel(
                firstPresent(aggregatedRow.parentLabel(), aggregatedRow.groupLabel(), aggregatedRow.label())
            );
            if (currentSection != null && !currentSection.equals(rowSection)) {
                rows.add(
                    toSectionTotalRow(
                        currentSection,
                        incomeColumnTotals,
                        expenseColumnTotals,
                        incomeTotal,
                        expenseTotal,
                        context.grandTotal()
                    )
                );
            }
            rows.add(toRowDto(aggregatedRow, columnTotals, context.grandTotal()));
            currentSection = rowSection;
        }
        if (currentSection != null) {
            rows.add(
                toSectionTotalRow(currentSection, incomeColumnTotals, expenseColumnTotals, incomeTotal, expenseTotal, context.grandTotal())
            );
        }
        rows.add(toIncomeLessExpensesRow(incomeColumnTotals, expenseColumnTotals, incomeTotal, expenseTotal, context.grandTotal()));
        result.setRows(rows);

        result.setSeries(
            context
                .leafRows()
                .stream()
                .map(leaf -> {
                    IncomeExpenseReportResultDTO.SeriesDTO series = new IncomeExpenseReportResultDTO.SeriesDTO();
                    series.setName(leaf.label());
                    series.setData(leaf.values());
                    return series;
                })
                .toList()
        );

        result.setPie(
            context
                .leafRows()
                .stream()
                .map(leaf -> {
                    IncomeExpenseReportResultDTO.PieSegmentDTO segment = new IncomeExpenseReportResultDTO.PieSegmentDTO();
                    segment.setLabel(leaf.label());
                    segment.setValue(leaf.total());
                    segment.setPercentOfTotal(percent(leaf.total(), context.grandTotal()));
                    return segment;
                })
                .filter(segment -> segment.getValue().compareTo(BigDecimal.ZERO) != 0)
                .toList()
        );

        return result;
    }

    private IncomeExpenseReportResultDTO.RowDTO toRowDto(
        AggregatedRow aggregatedRow,
        List<BigDecimal> columnTotals,
        BigDecimal grandTotal
    ) {
        IncomeExpenseReportResultDTO.RowDTO row = new IncomeExpenseReportResultDTO.RowDTO();
        row.setKey(aggregatedRow.key());
        row.setLabel(aggregatedRow.label());
        row.setGroupLabel(aggregatedRow.groupLabel());
        row.setParentLabel(aggregatedRow.parentLabel());
        row.setSubtotal(aggregatedRow.subtotal());
        row.setGrandTotal(false);
        row.setRowType(aggregatedRow.rowType());
        row.setTotal(aggregatedRow.total());
        row.setPercentOfTotal(percent(aggregatedRow.total(), grandTotal));
        row.setValues(aggregatedRow.values());
        row.setValuePercents(
            aggregatedRow
                .values()
                .stream()
                .map(value -> percent(value, grandTotal))
                .toList()
        );
        return row;
    }

    private IncomeExpenseReportResultDTO.RowDTO toSectionTotalRow(
        String section,
        List<BigDecimal> incomeColumnTotals,
        List<BigDecimal> expenseColumnTotals,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal grandTotal
    ) {
        IncomeExpenseReportResultDTO.RowDTO row = new IncomeExpenseReportResultDTO.RowDTO();
        boolean income = "income".equals(section);
        List<BigDecimal> sectionValues = income ? incomeColumnTotals : expenseColumnTotals;
        BigDecimal sectionTotal = income ? incomeTotal : expenseTotal;
        String sectionLabel = income ? "Income Total" : "Expense Total";

        row.setKey("section-total:" + section);
        row.setLabel(sectionLabel);
        row.setGrandTotal(false);
        row.setRowType("sectionTotal");
        row.setTotal(sectionTotal);
        row.setPercentOfTotal(percent(sectionTotal, grandTotal));
        row.setValues(sectionValues);
        row.setValuePercents(
            sectionValues
                .stream()
                .map(value -> percent(value, grandTotal))
                .toList()
        );
        return row;
    }

    private IncomeExpenseReportResultDTO.RowDTO toIncomeLessExpensesRow(
        List<BigDecimal> incomeColumnTotals,
        List<BigDecimal> expenseColumnTotals,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal grandTotal
    ) {
        List<BigDecimal> netValues = new ArrayList<>(incomeColumnTotals.size());
        for (int i = 0; i < incomeColumnTotals.size(); i++) {
            netValues.add(incomeColumnTotals.get(i).subtract(expenseColumnTotals.get(i)));
        }
        BigDecimal netTotal = incomeTotal.subtract(expenseTotal);

        IncomeExpenseReportResultDTO.RowDTO row = new IncomeExpenseReportResultDTO.RowDTO();
        row.setKey("income-less-expenses");
        row.setLabel("Income less Expenses");
        row.setGrandTotal(false);
        row.setRowType("net");
        row.setTotal(netTotal);
        row.setPercentOfTotal(percent(netTotal, grandTotal));
        row.setValues(netValues);
        row.setValuePercents(
            netValues
                .stream()
                .map(value -> percent(value, grandTotal))
                .toList()
        );
        return row;
    }

    private AggregationContext aggregateRows(String rowDimension, List<Bucket> buckets, List<ReportTransaction> transactions) {
        LinkedHashMap<String, MutableRow> rows = new LinkedHashMap<>();
        LinkedHashMap<String, MutableRow> subtotals = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (ReportTransaction transaction : transactions) {
            RowDescriptor descriptor = describeRow(rowDimension, transaction);
            if (descriptor == null) {
                continue;
            }
            MutableRow row = rows.computeIfAbsent(descriptor.rowKey(), ignored -> {
                return new MutableRow(
                    descriptor.rowKey(),
                    descriptor.label(),
                    descriptor.groupLabel(),
                    descriptor.parentLabel(),
                    false,
                    descriptor.rowType(),
                    descriptor.subtotalKey(),
                    buckets.size()
                );
            });
            int bucketIndex = findBucketIndex(buckets, transaction.date());
            if (bucketIndex < 0) {
                continue;
            }
            row.add(bucketIndex, transaction.value());
            grandTotal = grandTotal.add(transaction.value());

            if (descriptor.subtotalKey() != null) {
                MutableRow subtotal = subtotals.computeIfAbsent(descriptor.subtotalKey(), ignored ->
                    new MutableRow(
                        descriptor.subtotalKey(),
                        descriptor.subtotalLabel(),
                        descriptor.subtotalGroupLabel(),
                        descriptor.subtotalParentLabel(),
                        true,
                        "subtotal",
                        null,
                        buckets.size()
                    )
                );
                subtotal.add(bucketIndex, transaction.value());
            }
        }

        List<MutableRow> leafRows = new ArrayList<>(rows.values());
        leafRows.sort(
            Comparator.comparingInt((MutableRow row) -> groupSortRank(row.parentLabel(), row.groupLabel(), row.label()))
                .thenComparing((MutableRow row) -> row.parentLabel() == null ? "" : row.parentLabel(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(row -> row.groupLabel() == null ? "" : row.groupLabel(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(MutableRow::label, String.CASE_INSENSITIVE_ORDER)
        );

        List<AggregatedRow> displayRows = new ArrayList<>();
        if ("categoryType".equals(rowDimension)) {
            displayRows.addAll(leafRows.stream().map(MutableRow::toImmutable).toList());
        } else {
            for (MutableRow row : leafRows) {
                displayRows.add(row.toImmutable());
                if (row.subtotalLookupKey() != null) {
                    MutableRow subtotal = subtotals.get(row.subtotalLookupKey());
                    if (subtotal != null && shouldEmitSubtotalAfterRow(row, leafRows)) {
                        displayRows.add(subtotal.toImmutable());
                    }
                }
            }
        }

        return new AggregationContext(displayRows, leafRows.stream().map(MutableRow::toImmutable).toList(), grandTotal);
    }

    private boolean shouldEmitSubtotalAfterRow(MutableRow row, List<MutableRow> leafRows) {
        int index = leafRows.indexOf(row);
        if (index < 0) {
            return false;
        }
        if (index == leafRows.size() - 1) {
            return row.subtotalLookupKey() != null;
        }
        return !Objects.equals(row.subtotalLookupKey(), leafRows.get(index + 1).subtotalLookupKey());
    }

    private int findBucketIndex(List<Bucket> buckets, LocalDate date) {
        for (int i = 0; i < buckets.size(); i++) {
            Bucket bucket = buckets.get(i);
            if (
                (date.isEqual(bucket.start()) || date.isAfter(bucket.start())) &&
                (date.isEqual(bucket.end()) || date.isBefore(bucket.end()))
            ) {
                return i;
            }
        }
        return -1;
    }

    private RowDescriptor describeRow(String rowDimension, ReportTransaction transaction) {
        return switch (rowDimension) {
            case "categoryType" -> new RowDescriptor(
                "type:" + transaction.rootKey(),
                transaction.rootLabel(),
                null,
                null,
                "row",
                null,
                null,
                null,
                null
            );
            case "category" -> describeCategoryRow(transaction);
            case "subcategory" -> describeSubcategoryRow(transaction);
            default -> throw new BadRequestAlertException("Unsupported row dimension", "reports", "invalidrowdimension");
        };
    }

    private RowDescriptor describeCategoryRow(ReportTransaction transaction) {
        if (transaction.level2Id() == null) {
            return null;
        }
        return new RowDescriptor(
            "category:" + transaction.level2Id(),
            transaction.level2Label(),
            transaction.rootLabel(),
            transaction.rootLabel(),
            "row",
            "subtotal:" + transaction.rootKey(),
            transaction.rootLabel() + " subtotal",
            transaction.rootLabel(),
            null
        );
    }

    private RowDescriptor describeSubcategoryRow(ReportTransaction transaction) {
        if (transaction.level2Id() == null) {
            return null;
        }
        if (transaction.level3Id() != null) {
            return new RowDescriptor(
                "subcategory:" + transaction.level3Id(),
                transaction.level3Label(),
                transaction.level2Label(),
                transaction.rootLabel(),
                "row",
                "subtotal:" + transaction.level2Id(),
                transaction.level2Label() + " subtotal",
                transaction.level2Label(),
                transaction.rootLabel()
            );
        }
        return new RowDescriptor(
            "subcategory:" + transaction.level2Id(),
            transaction.level2Label(),
            transaction.rootLabel(),
            transaction.rootLabel(),
            "row",
            "subtotal:" + transaction.rootKey(),
            transaction.rootLabel() + " subtotal",
            transaction.rootLabel(),
            null
        );
    }

    private List<Bucket> buildBuckets(LocalDate start, LocalDate end, String columnDimension) {
        List<Bucket> buckets = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            switch (columnDimension) {
                case "halfMonth" -> {
                    LocalDate bucketStart = cursor.getDayOfMonth() <= 15 ? cursor.withDayOfMonth(1) : cursor.withDayOfMonth(16);
                    LocalDate bucketEnd =
                        cursor.getDayOfMonth() <= 15
                            ? cursor.withDayOfMonth(Math.min(15, cursor.lengthOfMonth()))
                            : cursor.with(TemporalAdjusters.lastDayOfMonth());
                    buckets.add(
                        new Bucket(
                            bucketKey(bucketStart, columnDimension),
                            bucketHalfMonthLabel(bucketStart),
                            bucketStart,
                            min(bucketEnd, end)
                        )
                    );
                    cursor = bucketEnd.plusDays(1);
                }
                case "month" -> {
                    LocalDate bucketStart = cursor.withDayOfMonth(1);
                    LocalDate bucketEnd = cursor.with(TemporalAdjusters.lastDayOfMonth());
                    buckets.add(
                        new Bucket(
                            bucketKey(bucketStart, columnDimension),
                            bucketStart.format(MONTH_LABEL_FORMAT),
                            bucketStart,
                            min(bucketEnd, end)
                        )
                    );
                    cursor = bucketEnd.plusDays(1);
                }
                case "quarter" -> {
                    int quarterStartMonth = ((cursor.getMonthValue() - 1) / 3) * 3 + 1;
                    LocalDate bucketStart = LocalDate.of(cursor.getYear(), quarterStartMonth, 1);
                    LocalDate bucketEnd = bucketStart.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
                    String label = "Q" + (((quarterStartMonth - 1) / 3) + 1) + " " + bucketStart.getYear();
                    buckets.add(new Bucket(bucketKey(bucketStart, columnDimension), label, bucketStart, min(bucketEnd, end)));
                    cursor = bucketEnd.plusDays(1);
                }
                case "year" -> {
                    LocalDate bucketStart = LocalDate.of(cursor.getYear(), Month.JANUARY, 1);
                    LocalDate bucketEnd = LocalDate.of(cursor.getYear(), Month.DECEMBER, 31);
                    buckets.add(
                        new Bucket(
                            bucketKey(bucketStart, columnDimension),
                            String.valueOf(bucketStart.getYear()),
                            bucketStart,
                            min(bucketEnd, end)
                        )
                    );
                    cursor = bucketEnd.plusDays(1);
                }
                default -> throw new BadRequestAlertException("Unsupported column dimension", "reports", "invalidcolumndimension");
            }
        }
        return buckets;
    }

    private String bucketHalfMonthLabel(LocalDate date) {
        if (date.getDayOfMonth() == 1) {
            return "1-15 " + date.format(DateTimeFormatter.ofPattern("MMM yyyy"));
        }
        return (
            "16-" +
            date.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth() +
            " " +
            date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
        );
    }

    private String bucketKey(LocalDate start, String dimension) {
        return dimension + ":" + start;
    }

    private boolean matchesRowSelection(ReportTransaction transaction, String rowDimension, String rowKey) {
        if ("grand-total".equals(rowKey)) {
            return true;
        }
        if ("section-total:income".equals(rowKey)) {
            return isIncomeTransaction(transaction);
        }
        if ("section-total:expense".equals(rowKey) || "section-total:expenses".equals(rowKey)) {
            return isExpenseTransaction(transaction);
        }
        if ("income-less-expenses".equals(rowKey)) {
            return isIncomeTransaction(transaction) || isExpenseTransaction(transaction);
        }

        RowDescriptor descriptor = describeRow(rowDimension, transaction);
        if (descriptor == null) {
            return false;
        }
        if (rowKey.startsWith("subtotal:")) {
            return rowKey.equals(descriptor.subtotalKey());
        }
        return rowKey.equals(descriptor.rowKey());
    }

    private boolean matchesBucketSelection(ReportTransaction transaction, List<Bucket> buckets, String columnKey) {
        if (columnKey == null) {
            return true;
        }
        for (Bucket bucket : buckets) {
            if (
                columnKey.equals(bucket.key()) &&
                (transaction.date().isEqual(bucket.start()) || transaction.date().isAfter(bucket.start())) &&
                (transaction.date().isEqual(bucket.end()) || transaction.date().isBefore(bucket.end()))
            ) {
                return true;
            }
        }
        return false;
    }

    private IncomeExpenseReportDrilldownDTO.TransactionDTO mapDrilldownTransaction(ReportTransaction transaction, String rowKey) {
        IncomeExpenseReportDrilldownDTO.TransactionDTO dto = new IncomeExpenseReportDrilldownDTO.TransactionDTO();
        dto.setId(transaction.id());
        dto.setDate(transaction.date());
        dto.setAccountId(transaction.accountId());
        dto.setPayeeName(transaction.payeeName());
        dto.setCategoryName(transaction.displayCategoryLabel());
        dto.setFamilyMemberName(transaction.familyMemberName());
        dto.setMemo(transaction.memo());
        dto.setSectionLabel(transaction.rootLabel());
        dto.setAmount(contributionAmountForRow(transaction, rowKey));
        dto.setOriginalCurrencyCode(transaction.originalCurrencyCode());
        dto.setOriginalAmount(transaction.originalAmount());
        return dto;
    }

    private BigDecimal contributionAmountForRow(ReportTransaction transaction, String rowKey) {
        if ("income-less-expenses".equals(rowKey) && isExpenseTransaction(transaction)) {
            return transaction.value().negate();
        }
        return transaction.value();
    }

    private ReportTransaction mapReportTransaction(
        FinanceTransaction transaction,
        Map<String, Integer> accountTypesById,
        Map<UUID, BigDecimal> transferDerivedBaseAmounts,
        Map<UUID, FinanceTransaction> transferCounterpartsByTransactionId,
        Map<String, String> securityNamesById
    ) {
        if (transaction == null || transaction.getDate() == null || transaction.getCategory() == null) {
            return null;
        }
        FinanceCategory category = transaction.getCategory();
        if (category.getClassificationId() == null || category.getClassificationId() != CATEGORY_CLASSIFICATION_ID) {
            return null;
        }

        FinanceCategory root = category;
        FinanceCategory level2 = null;
        FinanceCategory level3 = null;
        while (root.getParent() != null) {
            if (level3 == null) {
                level3 = root;
            } else if (level2 == null) {
                level2 = root;
            }
            root = root.getParent();
        }
        if (level2 == null && level3 != null) {
            level2 = level3;
            level3 = null;
        }

        if (transaction.isTransfer() && !shouldIncludeTransferTransaction(transaction, root, accountTypesById)) {
            return null;
        }

        FinanceTransaction transferCounterpart =
            transaction.getId() == null ? null : transferCounterpartsByTransactionId.get(transaction.getId());
        String resolvedSecurityId = defaultIfBlank(
            transaction.getSecurityId(),
            transferCounterpart == null ? null : transferCounterpart.getSecurityId()
        );
        String resolvedSecurityName = defaultIfBlank(
            resolvedSecurityId == null ? null : securityNamesById.get(resolvedSecurityId),
            resolvedSecurityId
        );
        BigDecimal value = normalizeReportAmount(transaction, transferDerivedBaseAmounts);
        return new ReportTransaction(
            transaction.getId().toString(),
            transaction.getDate(),
            value,
            transaction.getAccountId(),
            buildDisplayPayeeName(
                transaction,
                level3 != null ? level3.getName() : (level2 != null ? level2.getName() : root.getName()),
                resolvedSecurityName
            ),
            transaction.getPayeeId(),
            transaction.getMemo(),
            transaction.getWho() == null ? null : transaction.getWho().getName(),
            transaction.getWho() == null ? null : transaction.getWho().getId().toString(),
            category.getId().toString(),
            root.getName().toLowerCase(Locale.ROOT),
            root.getName(),
            level2 == null ? null : level2.getId().toString(),
            level2 == null ? null : level2.getName(),
            level3 == null ? null : level3.getId().toString(),
            level3 == null ? null : level3.getName(),
            level3 != null ? level3.getName() : (level2 != null ? level2.getName() : root.getName()),
            resolvedSecurityName,
            transaction.getCurrencyCode(),
            normalizeOriginalAmount(transaction)
        );
    }

    private String buildDisplayPayeeName(FinanceTransaction transaction, String displayCategoryLabel, String securityName) {
        if (transaction == null) {
            return null;
        }

        String storedPayeeName = defaultIfBlank(transaction.getPayeeName(), null);
        if (!transaction.isTransfer() && storedPayeeName != null) {
            return transaction.getPayeeName();
        }

        String categoryLabel = defaultIfBlank(displayCategoryLabel, null);
        String resolvedSecurityName = defaultIfBlank(securityName, null);
        if (categoryLabel != null || resolvedSecurityName != null) {
            return Stream.of(categoryLabel, resolvedSecurityName).filter(Objects::nonNull).collect(Collectors.joining(": "));
        }

        return storedPayeeName;
    }

    private boolean shouldIncludeTransferTransaction(
        FinanceTransaction transaction,
        FinanceCategory rootCategory,
        Map<String, Integer> accountTypesById
    ) {
        Integer accountType = accountTypesById.get(transaction.getAccountId());
        if (accountType == null || accountType.intValue() != FinanceAccountType.INVESTMENT.getValue()) {
            return false;
        }

        return "income".equalsIgnoreCase(rootCategory.getName());
    }

    private BigDecimal normalizeReportAmount(FinanceTransaction transaction, Map<UUID, BigDecimal> transferDerivedBaseAmounts) {
        if (transaction == null) {
            return BigDecimal.ZERO;
        }
        if (transaction.getAmountBase() != null) {
            return transaction.getAmountBase().abs();
        }
        if (transaction.getId() != null) {
            BigDecimal transferDerivedAmount = transferDerivedBaseAmounts.get(transaction.getId());
            if (transferDerivedAmount != null) {
                return transferDerivedAmount.abs();
            }
        }
        if (transaction.getAmount() != null && transaction.getRateToBase() != null && Double.compare(transaction.getRateToBase(), 0d) > 0) {
            return transaction.getAmount().multiply(BigDecimal.valueOf(transaction.getRateToBase())).abs();
        }
        return transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount().abs();
    }

    private BigDecimal normalizeOriginalAmount(FinanceTransaction transaction) {
        if (transaction == null) {
            return BigDecimal.ZERO;
        }
        if (transaction.getAmount() != null) {
            return transaction.getAmount().abs();
        }
        return BigDecimal.ZERO;
    }

    private Map<UUID, BigDecimal> resolveTransferDerivedBaseAmounts(
        String userGuid,
        String baseCurrencyCode,
        List<FinanceTransaction> transactions
    ) {
        if (transactions.isEmpty()) {
            return Collections.emptyMap();
        }

        String normalizedBaseCurrencyCode = defaultIfBlank(baseCurrencyCode, "AUD");
        Map<UUID, FinanceTransaction> transactionsById = new LinkedHashMap<>();
        Set<UUID> unresolvedTransactionIds = new LinkedHashSet<>();

        for (FinanceTransaction transaction : transactions) {
            if (transaction.getId() != null) {
                transactionsById.put(transaction.getId(), transaction);
            }
            if (shouldResolveBaseAmountFromTransfer(transaction, normalizedBaseCurrencyCode)) {
                unresolvedTransactionIds.add(transaction.getId());
            }
        }

        if (unresolvedTransactionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<FinanceTransferLink> candidateLinks = financeTransferLinkRepository
            .findAllByUserGuid(userGuid)
            .stream()
            .filter(link -> unresolvedTransactionIds.contains(link.getFromId()) || unresolvedTransactionIds.contains(link.getLinkId()))
            .toList();

        Set<UUID> counterpartIds = new LinkedHashSet<>();
        for (FinanceTransferLink link : candidateLinks) {
            if (unresolvedTransactionIds.contains(link.getFromId())) {
                counterpartIds.add(link.getLinkId());
            }
            if (unresolvedTransactionIds.contains(link.getLinkId())) {
                counterpartIds.add(link.getFromId());
            }
        }

        if (!counterpartIds.isEmpty()) {
            financeTransactionRepository
                .findAllByUserGuidAndIdIn(userGuid, counterpartIds)
                .forEach(counterpart -> transactionsById.putIfAbsent(counterpart.getId(), counterpart));
        }

        Map<UUID, BigDecimal> derivedAmounts = new HashMap<>();
        for (FinanceTransferLink link : candidateLinks) {
            registerTransferDerivedAmount(link.getFromId(), link.getLinkId(), transactionsById, normalizedBaseCurrencyCode, derivedAmounts);
            registerTransferDerivedAmount(link.getLinkId(), link.getFromId(), transactionsById, normalizedBaseCurrencyCode, derivedAmounts);
        }

        return derivedAmounts;
    }

    private Map<UUID, FinanceTransaction> resolveTransferCounterpartsByTransactionId(
        String userGuid,
        List<FinanceTransaction> transactions
    ) {
        if (transactions.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> transactionIds = transactions.stream().map(FinanceTransaction::getId).filter(Objects::nonNull).toList();
        if (transactionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<FinanceTransferLink> transferLinks = financeTransferLinkRepository.findAllByUserGuidAndTransactionIds(
            userGuid,
            transactionIds
        );
        if (transferLinks.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, UUID> counterpartIdByTransactionId = new LinkedHashMap<>();
        Set<UUID> counterpartIds = new LinkedHashSet<>();
        for (FinanceTransferLink transferLink : transferLinks) {
            counterpartIdByTransactionId.put(transferLink.getFromId(), transferLink.getLinkId());
            counterpartIdByTransactionId.put(transferLink.getLinkId(), transferLink.getFromId());
            counterpartIds.add(transferLink.getFromId());
            counterpartIds.add(transferLink.getLinkId());
        }

        Map<UUID, FinanceTransaction> transactionsById = financeTransactionRepository
            .findAllByUserGuidAndIdIn(userGuid, counterpartIds)
            .stream()
            .filter(transaction -> transaction.getId() != null)
            .collect(Collectors.toMap(FinanceTransaction::getId, transaction -> transaction, (left, right) -> left, LinkedHashMap::new));

        Map<UUID, FinanceTransaction> counterpartsByTransactionId = new LinkedHashMap<>();
        for (Map.Entry<UUID, UUID> entry : counterpartIdByTransactionId.entrySet()) {
            FinanceTransaction counterpart = transactionsById.get(entry.getValue());
            if (counterpart != null) {
                counterpartsByTransactionId.put(entry.getKey(), counterpart);
            }
        }
        return counterpartsByTransactionId;
    }

    private Map<String, String> resolveSecurityNamesById(
        String userGuid,
        List<FinanceTransaction> transactions,
        Map<UUID, FinanceTransaction> transferCounterpartsByTransactionId
    ) {
        Set<UUID> securityIds = new LinkedHashSet<>();
        transactions.forEach(transaction -> addSecurityId(securityIds, transaction == null ? null : transaction.getSecurityId()));
        transferCounterpartsByTransactionId
            .values()
            .forEach(transaction -> addSecurityId(securityIds, transaction == null ? null : transaction.getSecurityId()));
        if (securityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return financeUserSecurityRepository
            .findAllByUserGuidAndIdIn(userGuid, securityIds)
            .stream()
            .filter(security -> security.getId() != null)
            .collect(
                Collectors.toMap(
                    security -> security.getId().toString(),
                    FinanceUserSecurity::getName,
                    (left, right) -> left,
                    LinkedHashMap::new
                )
            );
    }

    private void addSecurityId(Set<UUID> securityIds, String securityId) {
        if (securityId == null || securityId.isBlank()) {
            return;
        }

        try {
            securityIds.add(UUID.fromString(securityId));
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed security ids.
        }
    }

    private void registerTransferDerivedAmount(
        UUID sourceTransactionId,
        UUID counterpartTransactionId,
        Map<UUID, FinanceTransaction> transactionsById,
        String baseCurrencyCode,
        Map<UUID, BigDecimal> derivedAmounts
    ) {
        FinanceTransaction sourceTransaction = transactionsById.get(sourceTransactionId);
        if (!shouldResolveBaseAmountFromTransfer(sourceTransaction, baseCurrencyCode)) {
            return;
        }

        FinanceTransaction counterpartTransaction = transactionsById.get(counterpartTransactionId);
        if (counterpartTransaction == null) {
            return;
        }

        BigDecimal counterpartBaseAmount = counterpartTransaction.getAmountBase();
        if (counterpartBaseAmount != null) {
            derivedAmounts.put(sourceTransactionId, counterpartBaseAmount.abs());
            return;
        }

        if (sameCurrency(counterpartTransaction.getCurrencyCode(), baseCurrencyCode) && counterpartTransaction.getAmount() != null) {
            derivedAmounts.put(sourceTransactionId, counterpartTransaction.getAmount().abs());
        }
    }

    private boolean shouldResolveBaseAmountFromTransfer(FinanceTransaction transaction, String baseCurrencyCode) {
        return (
            transaction != null &&
            transaction.getId() != null &&
            transaction.getAmountBase() == null &&
            transaction.getAmount() != null &&
            !sameCurrency(transaction.getCurrencyCode(), baseCurrencyCode)
        );
    }

    private boolean sameCurrency(String left, String right) {
        return defaultIfBlank(left, "").equalsIgnoreCase(defaultIfBlank(right, ""));
    }

    private Map<String, Integer> getAccountTypesById(String userGuid) {
        return financeAccountRepository
            .findAllByUserGuid(userGuid)
            .stream()
            .filter(account -> account.getId() != null)
            .collect(LinkedHashMap::new, (map, account) -> map.put(account.getId().toString(), account.getType()), LinkedHashMap::putAll);
    }

    private boolean matchesFilters(
        ReportTransaction transaction,
        Set<String> accountIds,
        Set<String> categoryIds,
        Set<String> payeeIds,
        Set<String> familyMemberIds
    ) {
        if (!accountIds.isEmpty() && (transaction.accountId() == null || !accountIds.contains(transaction.accountId()))) {
            return false;
        }
        if (!categoryIds.isEmpty() && !categoryIds.contains(transaction.categoryId())) {
            return false;
        }
        if (!payeeIds.isEmpty() && transaction.payeeId() != null && !payeeIds.contains(transaction.payeeId())) {
            return false;
        }
        if (!familyMemberIds.isEmpty() && transaction.familyMemberId() != null && !familyMemberIds.contains(transaction.familyMemberId())) {
            return false;
        }
        return true;
    }

    private ReportConfigDTO normalizeForExecution(ReportConfigDTO input) {
        ReportConfigDTO config = new ReportConfigDTO();
        config.setReportKey(normalizeReportKey(input.getReportKey()));
        config.setName(input.getName());
        config.setTitle(input.getTitle() == null || input.getTitle().isBlank() ? "Income & Expenses" : input.getTitle().trim());
        config.setRowDimension(defaultIfBlank(input.getRowDimension(), "subcategory"));
        config.setColumnDimension(defaultIfBlank(input.getColumnDimension(), "month"));
        config.setDefaultView(defaultIfBlank(input.getDefaultView(), "report"));
        config.setShowPercentOfTotal(input.isShowPercentOfTotal());
        config.setDatePreset(defaultIfBlank(input.getDatePreset(), "6M"));
        config.setStartDate(input.getStartDate());
        config.setEndDate(input.getEndDate());
        config.setAccountIds(input.getAccountIds());
        config.setCategoryIds(input.getCategoryIds());
        config.setPayeeIds(input.getPayeeIds());
        config.setFamilyMemberIds(input.getFamilyMemberIds());
        return config;
    }

    private String normalizeReportKey(String reportKey) {
        String normalized = defaultIfBlank(reportKey, REPORT_KEY_INCOME_EXPENSES);
        if (!REPORT_KEY_INCOME_EXPENSES.equals(normalized)) {
            throw new BadRequestAlertException("Unsupported report key", "reports", "invalidreportkey");
        }
        return normalized;
    }

    private void validateRunPayload(ReportConfigDTO dto) {
        normalizeReportKey(dto.getReportKey());
        if (dto.getRowDimension() != null && !List.of("categoryType", "category", "subcategory").contains(dto.getRowDimension())) {
            throw new BadRequestAlertException("Unsupported row dimension", "reports", "invalidrowdimension");
        }
        if (dto.getColumnDimension() != null && !List.of("halfMonth", "month", "quarter", "year").contains(dto.getColumnDimension())) {
            throw new BadRequestAlertException("Unsupported column dimension", "reports", "invalidcolumndimension");
        }
        if (dto.getDefaultView() != null && !List.of("report", "bar", "line", "pie").contains(dto.getDefaultView())) {
            throw new BadRequestAlertException("Unsupported default view", "reports", "invaliddefaultview");
        }
    }

    private void validateEditablePayload(ReportConfigDTO dto, boolean creating) {
        validateRunPayload(dto);
        if (dto.isBuiltin()) {
            throw new BadRequestAlertException("Built-in report configurations cannot be saved directly", "reports", "builtinreadonly");
        }
        if (creating && dto.getId() != null) {
            throw new BadRequestAlertException("New report configuration cannot already have an id", "reports", "idexists");
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestAlertException("Report configuration name is required", "reports", "namerequired");
        }
        return name.trim();
    }

    private void ensureUniqueName(String login, String reportKey, String name, UUID excludeId) {
        if (userReportConfigRepository.existsNameForUserReport(login, reportKey, name, excludeId)) {
            throw new BadRequestAlertException("A configuration with that name already exists", "reports", "duplicatename");
        }
    }

    private ReportConfigDTO toDto(UserReportConfig entity) {
        ReportConfigPayload payload = readConfigJson(entity.getConfigJson());
        ReportConfigDTO dto = new ReportConfigDTO();
        dto.setId(entity.getId());
        dto.setReportKey(entity.getReportKey());
        dto.setName(entity.getName());
        dto.setTitle(payload.title);
        dto.setRowDimension(payload.rowDimension);
        dto.setColumnDimension(payload.columnDimension);
        dto.setShowPercentOfTotal(payload.showPercentOfTotal);
        dto.setDefaultView(payload.defaultView);
        dto.setDatePreset(payload.datePreset);
        dto.setStartDate(payload.startDate);
        dto.setEndDate(payload.endDate);
        dto.setBuiltin(false);
        dto.setEditable(true);
        dto.setAccountIds(payload.accountIds);
        dto.setCategoryIds(payload.categoryIds);
        dto.setPayeeIds(payload.payeeIds);
        dto.setFamilyMemberIds(payload.familyMemberIds);
        return dto;
    }

    private String writeConfigJson(ReportConfigDTO dto) {
        ReportConfigPayload payload = new ReportConfigPayload();
        payload.title = dto.getTitle();
        payload.rowDimension = dto.getRowDimension();
        payload.columnDimension = dto.getColumnDimension();
        payload.showPercentOfTotal = dto.isShowPercentOfTotal();
        payload.defaultView = dto.getDefaultView();
        payload.datePreset = dto.getDatePreset();
        payload.startDate = dto.getStartDate();
        payload.endDate = dto.getEndDate();
        payload.accountIds = dto.getAccountIds();
        payload.categoryIds = dto.getCategoryIds();
        payload.payeeIds = dto.getPayeeIds();
        payload.familyMemberIds = dto.getFamilyMemberIds();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BadRequestAlertException("Report configuration could not be saved", "reports", "invalidconfig");
        }
    }

    private ReportConfigPayload readConfigJson(String json) {
        if (json == null || json.isBlank()) {
            return new ReportConfigPayload();
        }
        try {
            return objectMapper.readValue(json, ReportConfigPayload.class);
        } catch (Exception e) {
            return new ReportConfigPayload();
        }
    }

    private ReportConfigDTO createDefaultIncomeExpenseConfig() {
        ReportConfigDTO dto = new ReportConfigDTO();
        dto.setReportKey(REPORT_KEY_INCOME_EXPENSES);
        dto.setName("Default");
        dto.setTitle("Income & Expenses");
        dto.setRowDimension("subcategory");
        dto.setColumnDimension("month");
        dto.setDefaultView("report");
        dto.setDatePreset("6M");
        dto.setShowPercentOfTotal(false);
        dto.setBuiltin(true);
        dto.setEditable(false);
        return dto;
    }

    private LocalDateRange resolveDateRange(String preset, LocalDate startDate, LocalDate endDate) {
        String normalizedPreset = defaultIfBlank(preset, "6M");
        LocalDate today = LocalDate.now();
        if ("custom".equalsIgnoreCase(normalizedPreset)) {
            if (startDate == null || endDate == null) {
                throw new BadRequestAlertException("Custom reports require a start and end date", "reports", "customdaterequired");
            }
            if (endDate.isBefore(startDate)) {
                throw new BadRequestAlertException("End date cannot be before the start date", "reports", "invaliddaterange");
            }
            return new LocalDateRange(startDate, endDate);
        }
        return switch (normalizedPreset.toUpperCase(Locale.ROOT)) {
            case "1M" -> new LocalDateRange(today.minusMonths(1).plusDays(1), today);
            case "3M" -> new LocalDateRange(today.minusMonths(3).plusDays(1), today);
            case "6M" -> new LocalDateRange(today.minusMonths(6).plusDays(1), today);
            case "12M" -> new LocalDateRange(today.minusMonths(12).plusDays(1), today);
            case "2Y" -> new LocalDateRange(today.minusYears(2).plusDays(1), today);
            case "5Y" -> new LocalDateRange(today.minusYears(5).plusDays(1), today);
            case "7Y" -> new LocalDateRange(today.minusYears(7).plusDays(1), today);
            case "10Y" -> new LocalDateRange(today.minusYears(10).plusDays(1), today);
            case "YTD" -> new LocalDateRange(LocalDate.of(today.getYear(), 1, 1), today);
            case "ALL" -> new LocalDateRange(LocalDate.of(2000, 1, 1), today);
            default -> throw new BadRequestAlertException("Unsupported date preset", "reports", "invaliddatepreset");
        };
    }

    private String getCurrentUserLogin() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user login not found", "reports", "currentusernotfound")
        );
    }

    private User getCurrentUser(String login) {
        return userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", "reports", "currentusernotfound"));
    }

    private BigDecimal percent(BigDecimal value, BigDecimal total) {
        if (value == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(total, 6, RoundingMode.HALF_UP);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean isIncomeRow(AggregatedRow row) {
        return "income".equals(normalizeSectionLabel(firstPresent(row.parentLabel(), row.groupLabel(), row.label())));
    }

    private boolean isExpenseRow(AggregatedRow row) {
        String section = normalizeSectionLabel(firstPresent(row.parentLabel(), row.groupLabel(), row.label()));
        return "expense".equals(section) || "expenses".equals(section);
    }

    private boolean isIncomeTransaction(ReportTransaction transaction) {
        return "income".equals(normalizeSectionLabel(transaction.rootLabel()));
    }

    private boolean isExpenseTransaction(ReportTransaction transaction) {
        String section = normalizeSectionLabel(transaction.rootLabel());
        return "expense".equals(section) || "expenses".equals(section);
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeSectionLabel(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int groupSortRank(String parentLabel, String groupLabel, String label) {
        String candidate =
            parentLabel != null && !parentLabel.isBlank()
                ? parentLabel
                : (groupLabel != null && !groupLabel.isBlank() ? groupLabel : label);

        if (candidate == null) {
            return 99;
        }

        return switch (candidate.trim().toLowerCase(Locale.ROOT)) {
            case "income" -> 0;
            case "expense", "expenses" -> 1;
            default -> 99;
        };
    }

    private LocalDate min(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private static class ReportConfigPayload {

        public String title;
        public String rowDimension;
        public String columnDimension;
        public boolean showPercentOfTotal;
        public String defaultView;
        public String datePreset;
        public LocalDate startDate;
        public LocalDate endDate;
        public List<String> accountIds = new ArrayList<>();
        public List<String> categoryIds = new ArrayList<>();
        public List<String> payeeIds = new ArrayList<>();
        public List<String> familyMemberIds = new ArrayList<>();
    }

    private record LocalDateRange(LocalDate start, LocalDate end) {}

    private record Bucket(String key, String label, LocalDate start, LocalDate end) {}

    private record ReportTransaction(
        String id,
        LocalDate date,
        BigDecimal value,
        String accountId,
        String payeeName,
        String payeeId,
        String memo,
        String familyMemberName,
        String familyMemberId,
        String categoryId,
        String rootKey,
        String rootLabel,
        String level2Id,
        String level2Label,
        String level3Id,
        String level3Label,
        String displayCategoryLabel,
        String securityName,
        String originalCurrencyCode,
        BigDecimal originalAmount
    ) {}

    private record RowDescriptor(
        String rowKey,
        String label,
        String groupLabel,
        String parentLabel,
        String rowType,
        String subtotalKey,
        String subtotalLabel,
        String subtotalGroupLabel,
        String subtotalParentLabel
    ) {}

    private record AggregationContext(List<AggregatedRow> displayRows, List<AggregatedRow> leafRows, BigDecimal grandTotal) {}

    private record AggregatedRow(
        String key,
        String label,
        String groupLabel,
        String parentLabel,
        boolean subtotal,
        String rowType,
        List<BigDecimal> values,
        BigDecimal total
    ) {}

    private static class MutableRow {

        private final String key;
        private final String label;
        private final String groupLabel;
        private final String parentLabel;
        private final boolean subtotal;
        private final String rowType;
        private final List<BigDecimal> values;
        private final String subtotalLookupKey;

        private MutableRow(
            String key,
            String label,
            String groupLabel,
            String parentLabel,
            boolean subtotal,
            String rowType,
            String subtotalLookupKey,
            int columnCount
        ) {
            this.key = key;
            this.label = label;
            this.groupLabel = groupLabel;
            this.parentLabel = parentLabel;
            this.subtotal = subtotal;
            this.rowType = rowType;
            this.values = new ArrayList<>(Collections.nCopies(columnCount, BigDecimal.ZERO));
            this.subtotalLookupKey = subtotalLookupKey;
        }

        private void add(int index, BigDecimal value) {
            values.set(index, values.get(index).add(value));
        }

        private String key() {
            return key;
        }

        private String label() {
            return label;
        }

        private String groupLabel() {
            return groupLabel;
        }

        private String parentLabel() {
            return parentLabel;
        }

        private String subtotalLookupKey() {
            return subtotalLookupKey;
        }

        private AggregatedRow toImmutable() {
            BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return new AggregatedRow(key, label, groupLabel, parentLabel, subtotal, rowType, List.copyOf(values), total);
        }
    }
}
