package ovaro.plat4m.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.domain.UserReportConfig;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.UserReportConfigRepository;
import ovaro.plat4m.repository.UserRepository;
import ovaro.plat4m.security.SecurityUtils;
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
    private final FinanceTransactionRepository financeTransactionRepository;
    private final ObjectMapper objectMapper;

    public ReportsService(
        UserReportConfigRepository userReportConfigRepository,
        UserRepository userRepository,
        FinanceTransactionRepository financeTransactionRepository,
        ObjectMapper objectMapper
    ) {
        this.userReportConfigRepository = userReportConfigRepository;
        this.userRepository = userRepository;
        this.financeTransactionRepository = financeTransactionRepository;
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

        List<FinanceTransaction> rawTransactions = financeTransactionRepository.findReportTransactions(
            user.getGuid().toString(),
            dateRange.start(),
            dateRange.end()
        );
        List<ReportTransaction> transactions = rawTransactions
            .stream()
            .map(this::mapReportTransaction)
            .filter(Objects::nonNull)
            .filter(transaction ->
                matchesFilters(transaction, selectedAccountIds, selectedCategoryIds, selectedPayeeIds, selectedFamilyMemberIds)
            )
            .toList();

        List<Bucket> buckets = buildBuckets(dateRange.start(), dateRange.end(), normalizedConfig.getColumnDimension());
        AggregationContext context = aggregateRows(normalizedConfig.getRowDimension(), buckets, transactions);
        return buildResult(normalizedConfig, user, dateRange, buckets, context);
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
        for (AggregatedRow row : context.leafRows()) {
            for (int i = 0; i < row.values().size(); i++) {
                columnTotals.set(i, columnTotals.get(i).add(row.values().get(i)));
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
        for (AggregatedRow aggregatedRow : context.displayRows()) {
            rows.add(toRowDto(aggregatedRow, columnTotals, context.grandTotal()));
        }
        rows.add(toGrandTotalRow(columnTotals, context.grandTotal()));
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

    private IncomeExpenseReportResultDTO.RowDTO toGrandTotalRow(List<BigDecimal> columnTotals, BigDecimal grandTotal) {
        IncomeExpenseReportResultDTO.RowDTO row = new IncomeExpenseReportResultDTO.RowDTO();
        row.setKey("grand-total");
        row.setLabel("Grand Total");
        row.setGrandTotal(true);
        row.setRowType("grandTotal");
        row.setTotal(grandTotal);
        row.setPercentOfTotal(grandTotal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.ONE);
        row.setValues(columnTotals);
        row.setValuePercents(
            columnTotals
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
            Comparator.comparing((MutableRow row) -> row.parentLabel() == null ? "" : row.parentLabel(), String.CASE_INSENSITIVE_ORDER)
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

    private ReportTransaction mapReportTransaction(FinanceTransaction transaction) {
        if (transaction == null || transaction.getDate() == null || transaction.getCategory() == null) {
            return null;
        }
        if (transaction.isTransfer() || transaction.isInvestment()) {
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

        BigDecimal value = transaction.getAmountBase() != null ? transaction.getAmountBase().abs() : transaction.getAmount().abs();
        return new ReportTransaction(
            transaction.getDate(),
            value,
            transaction.getAccountId(),
            transaction.getPayeeId(),
            transaction.getWho() == null ? null : transaction.getWho().getId().toString(),
            category.getId().toString(),
            root.getName().toLowerCase(Locale.ROOT),
            root.getName(),
            level2 == null ? null : level2.getId().toString(),
            level2 == null ? null : level2.getName(),
            level3 == null ? null : level3.getId().toString(),
            level3 == null ? null : level3.getName()
        );
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
        LocalDate date,
        BigDecimal value,
        String accountId,
        String payeeId,
        String familyMemberId,
        String categoryId,
        String rootKey,
        String rootLabel,
        String level2Id,
        String level2Label,
        String level3Id,
        String level3Label
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
