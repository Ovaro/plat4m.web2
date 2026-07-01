package ovaro.plat4m.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinancePayee;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceTransactionImport;
import ovaro.plat4m.domain.FinanceTransactionImportMemory;
import ovaro.plat4m.domain.FinanceTransactionImportRow;
import ovaro.plat4m.domain.FinanceTransactionImportStatus;
import ovaro.plat4m.domain.FinanceTransactionImportTransferMemory;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceCategoryRepository;
import ovaro.plat4m.repository.FinancePayeeRepository;
import ovaro.plat4m.repository.FinanceTransactionImportMemoryRepository;
import ovaro.plat4m.repository.FinanceTransactionImportRepository;
import ovaro.plat4m.repository.FinanceTransactionImportRowRepository;
import ovaro.plat4m.repository.FinanceTransactionImportTransferMemoryRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.service.AIService.AiServiceCallType;
import ovaro.plat4m.service.dto.FinanceResourceDTO;
import ovaro.plat4m.service.dto.FinanceTransactionImportCommitRequestDTO;
import ovaro.plat4m.service.dto.FinanceTransactionImportCommitResponseDTO;
import ovaro.plat4m.service.dto.FinanceTransactionImportDTO;
import ovaro.plat4m.service.dto.FinanceTransactionImportDraftRequestDTO;
import ovaro.plat4m.service.dto.FinanceTransactionImportHistoryDTO;
import ovaro.plat4m.service.dto.FinanceTransactionImportRowDTO;
import ovaro.plat4m.service.dto.FinanceTransactionImportRowUpdateDTO;
import ovaro.plat4m.service.dto.FinanceTransactionUpdateDTO;

@Service
@Transactional
public class FinanceTransactionImportService {

    private static final Logger log = LoggerFactory.getLogger(FinanceTransactionImportService.class);
    private static final String TRANSACTION_KIND_TRANSFER = "TRANSFER";
    private static final String TRANSACTION_KIND_DEPOSIT = "DEPOSIT";
    private static final String TRANSACTION_KIND_WITHDRAWAL = "WITHDRAWAL";

    private final AIService aiService;
    private final ObjectMapper objectMapper;
    private final FinanceAccountRepository accountRepository;
    private final FinancePayeeRepository payeeRepository;
    private final FinanceCategoryRepository categoryRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final FinanceTransactionService transactionService;
    private final FinanceTransactionImportRepository importRepository;
    private final FinanceTransactionImportRowRepository rowRepository;
    private final FinanceTransactionImportMemoryRepository memoryRepository;
    private final FinanceTransactionImportTransferMemoryRepository transferMemoryRepository;

    public FinanceTransactionImportService(
        AIService aiService,
        ObjectMapper objectMapper,
        FinanceAccountRepository accountRepository,
        FinancePayeeRepository payeeRepository,
        FinanceCategoryRepository categoryRepository,
        FinanceTransactionRepository transactionRepository,
        FinanceTransactionService transactionService,
        FinanceTransactionImportRepository importRepository,
        FinanceTransactionImportRowRepository rowRepository,
        FinanceTransactionImportMemoryRepository memoryRepository,
        FinanceTransactionImportTransferMemoryRepository transferMemoryRepository
    ) {
        this.aiService = aiService;
        this.objectMapper = objectMapper;
        this.accountRepository = accountRepository;
        this.payeeRepository = payeeRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.importRepository = importRepository;
        this.rowRepository = rowRepository;
        this.memoryRepository = memoryRepository;
        this.transferMemoryRepository = transferMemoryRepository;
    }

    public FinanceTransactionImportDTO createDraft(User user, String accountId, FinanceTransactionImportDraftRequestDTO request) {
        String rawContent = trimToNull(request.getRawContent());
        String rawHtml = trimToNull(request.getRawHtml());
        if (rawContent == null) {
            throw new IllegalArgumentException("Pasted transaction content is required");
        }

        log.info(
            "createDraft:start userGuid={} accountId={} rawLength={} rawHtmlLength={} expectedEndingBalancePresent={}",
            user.getGuid(),
            accountId,
            rawContent.length(),
            rawHtml == null ? 0 : rawHtml.length(),
            request.getExpectedEndingBalance() != null
        );

        FinanceAccount account = findUserAccount(user, accountId);
        List<ParsedImportRow> parsedRows = parseRowsWithAi(account, rawContent, rawHtml);
        if (parsedRows.isEmpty()) {
            throw new IllegalArgumentException("The AI import could not find any transaction rows in the pasted content.");
        }

        FinanceTransactionImport transactionImport = new FinanceTransactionImport();
        transactionImport.setUserGuid(user.getGuid().toString());
        transactionImport.setAccountId(account.getId().toString());
        transactionImport.setAccountName(account.getName());
        transactionImport.setSourceLabel("Pasted web transactions");
        transactionImport.setRawContent(buildStoredRawContent(rawContent, rawHtml));
        transactionImport.setStatus(FinanceTransactionImportStatus.DRAFT);
        transactionImport.setExpectedEndingBalance(request.getExpectedEndingBalance());
        transactionImport.setCreatedDateTime(ZonedDateTime.now());
        transactionImport = this.importRepository.save(transactionImport);

        List<FinanceTransactionImportRow> rows = buildDraftRows(user, transactionImport, parsedRows);
        this.rowRepository.saveAll(rows);
        refreshImportSummary(user, transactionImport, rows);
        log.info(
            "createDraft:complete userGuid={} accountId={} importId={} rows={} flaggedRows={} correlationStatus={}",
            user.getGuid(),
            accountId,
            transactionImport.getId(),
            rows.size(),
            transactionImport.getFlaggedRows(),
            transactionImport.getCorrelationStatus()
        );
        return mapImport(transactionImport, rows);
    }

    public FinanceTransactionImportDTO getImport(User user, UUID importId) {
        FinanceTransactionImport transactionImport = findImport(user, importId);
        log.info("getImport userGuid={} importId={} status={}", user.getGuid(), importId, transactionImport.getStatus());
        List<FinanceTransactionImportRow> rows = this.rowRepository.findAllByTransactionImport_IdOrderByRowIndexAsc(importId);
        applyDuplicateChecks(user, transactionImport.getAccountId(), rows);
        refreshImportSummary(user, transactionImport, rows);
        return mapImport(transactionImport, rows);
    }

    public FinanceTransactionImportRowDTO updateImportRow(
        User user,
        UUID importId,
        UUID rowId,
        FinanceTransactionImportRowUpdateDTO update
    ) {
        findImport(user, importId);
        FinanceTransactionImportRow row = this.rowRepository
            .findByIdAndUserGuid(rowId, user.getGuid().toString())
            .filter(candidate -> importId.equals(candidate.getTransactionImport().getId()))
            .orElseThrow(() -> new IllegalArgumentException("Import row not found"));

        if (update.getDate() != null) {
            row.setDate(update.getDate());
        }
        if (update.getAmount() != null) {
            row.setAmount(update.getAmount());
        }
        if (update.getPayeeText() != null) {
            row.setPayeeText(trimToNull(update.getPayeeText()));
        }
        if (update.getMemo() != null) {
            row.setMemo(trimToNull(update.getMemo()));
        }
        if (update.getExternalTransferLike() != null) {
            row.setExternalTransferLike(Boolean.TRUE.equals(update.getExternalTransferLike()));
            if (row.isExternalTransferLike()) {
                row.setTransactionKind(isDeposit(row.getAmount()) ? TRANSACTION_KIND_DEPOSIT : TRANSACTION_KIND_WITHDRAWAL);
                row.setResolvedTransferAccountId(null);
                row.setResolvedTransferAccountName(null);
            }
        }

        if (update.getIgnored() != null) {
            row.setIgnored(Boolean.TRUE.equals(update.getIgnored()));
            if (row.isIgnored()) {
                row.setAccepted(false);
                row.setDuplicateConfirmed(false);
            }
        }

        if (update.getDuplicateConfirmed() != null && !update.getDuplicateConfirmed()) {
            row.setDuplicateConfirmed(false);
            row.setAccepted(false);
        }

        if (update.getDuplicateRejected() != null) {
            row.setDuplicateRejected(Boolean.TRUE.equals(update.getDuplicateRejected()));
            if (row.isDuplicateRejected()) {
                row.setDuplicateConfirmed(false);
                row.setAccepted(false);
                row.setIgnored(false);
            }
        }

        if (Boolean.TRUE.equals(update.getApplyDuplicateResolution())) {
            row.setDuplicateRejected(false);
            applyDuplicateResolution(user, row);
        } else if (update.getResolvedTransferAccountId() != null) {
            applyResolvedTransferAccount(user, row, trimToNull(update.getResolvedTransferAccountId()));
        } else if (update.getResolvedPayeeId() != null) {
            applyResolvedPayee(user, row, trimToNull(update.getResolvedPayeeId()));
        } else if (row.getResolvedPayeeId() == null) {
            autoResolveRow(user, row, loadPayees(user), loadCategories(user), loadAccounts(user));
        }

        if (!Boolean.TRUE.equals(update.getApplyDuplicateResolution()) && update.getResolvedCategoryId() != null) {
            applyResolvedCategory(user, row, trimToNull(update.getResolvedCategoryId()));
        }

        if (update.getAccepted() != null) {
            row.setAccepted(Boolean.TRUE.equals(update.getAccepted()));
            if (row.isAccepted()) {
                row.setIgnored(false);
            }
        }
        recomputeRowFlags(user, row, loadPayees(user), loadCategories(user), loadAccounts(user));
        this.rowRepository.save(row);
        List<FinanceTransactionImportRow> importRows = this.rowRepository.findAllByTransactionImport_IdOrderByRowIndexAsc(importId);
        applyDuplicateChecks(user, row.getTransactionImport().getAccountId(), importRows);
        refreshImportSummary(user, row.getTransactionImport(), importRows);
        FinanceTransactionImportRow refreshedRow = importRows
            .stream()
            .filter(candidate -> rowId.equals(candidate.getId()))
            .findFirst()
            .orElse(row);
        log.info(
            "updateImportRow:complete importId={} rowId={} duplicateSuspected={} duplicateConfirmed={} accepted={} ignored={} resolvedPayeeId={} resolvedCategoryId={}",
            importId,
            rowId,
            refreshedRow.isDuplicateSuspected(),
            refreshedRow.isDuplicateConfirmed(),
            refreshedRow.isAccepted(),
            refreshedRow.isIgnored(),
            refreshedRow.getResolvedPayeeId(),
            refreshedRow.getResolvedCategoryId()
        );
        return mapRow(refreshedRow);
    }

    public FinanceTransactionImportCommitResponseDTO commitImport(
        User user,
        UUID importId,
        FinanceTransactionImportCommitRequestDTO request
    ) {
        FinanceTransactionImport transactionImport = findImport(user, importId);
        boolean autoAcceptUnhandled = request != null && Boolean.TRUE.equals(request.getAutoAcceptUnhandled());
        log.info(
            "commitImport:start importId={} userGuid={} status={} autoAcceptUnhandled={}",
            importId,
            user.getGuid(),
            transactionImport.getStatus(),
            autoAcceptUnhandled
        );
        if (transactionImport.getStatus() != FinanceTransactionImportStatus.DRAFT) {
            throw new IllegalStateException("Only draft imports can be committed");
        }

        List<FinanceTransactionImportRow> rows = this.rowRepository.findAllByTransactionImport_IdOrderByRowIndexAsc(importId);
        if (rows.isEmpty()) {
            throw new IllegalStateException("This import does not contain any rows");
        }

        boolean rowStateChanged = false;
        for (FinanceTransactionImportRow row : rows) {
            if (row.isIgnored()) {
                continue;
            }
            if (row.isDuplicateSuspected() && row.isDuplicateStrongMatch() && !row.isDuplicateConfirmed()) {
                applyDuplicateResolution(user, row);
                rowStateChanged = true;
                continue;
            }
            if (autoAcceptUnhandled && !row.isAccepted() && !row.isDuplicateSuspected()) {
                row.setAccepted(true);
                rowStateChanged = true;
            }
        }
        if (rowStateChanged) {
            this.rowRepository.saveAll(rows);
            refreshImportSummary(user, transactionImport, rows);
        }

        for (FinanceTransactionImportRow row : rows) {
            if (row.isDuplicateConfirmed() || row.isIgnored()) {
                continue;
            }
            boolean requiresAttention =
                row.isTransferNeedsReview() ||
                row.isPayeeNeedsReview() ||
                row.isCategoryNeedsReview() ||
                row.isDateNeedsReview() ||
                row.isAmountNeedsReview() ||
                row.isBalanceMismatch() ||
                row.isDuplicateSuspected();
            if (requiresAttention && !row.isAccepted()) {
                log.warn(
                    "commitImport:blocked importId={} rowId={} rowIndex={} duplicateSuspected={} duplicateConfirmed={} accepted={} ignored={} payeeNeedsReview={} categoryNeedsReview={} dateNeedsReview={} amountNeedsReview={} balanceMismatch={}",
                    importId,
                    row.getId(),
                    row.getRowIndex(),
                    row.isDuplicateSuspected(),
                    row.isDuplicateConfirmed(),
                    row.isAccepted(),
                    row.isIgnored(),
                    row.isPayeeNeedsReview(),
                    row.isCategoryNeedsReview(),
                    row.isDateNeedsReview(),
                    row.isAmountNeedsReview(),
                    row.isBalanceMismatch()
                );
                throw new IllegalStateException("Resolve or accept all flagged rows before importing.");
            }
        }

        int createdTransactionCount = 0;
        List<FinanceTransaction> createdTransactions = new ArrayList<>();
        for (FinanceTransactionImportRow row : rows) {
            if (row.isDuplicateConfirmed() || row.isIgnored()) {
                continue;
            }
            FinanceTransactionUpdateDTO update = new FinanceTransactionUpdateDTO();
            update.setDate(row.getDate());
            update.setAmount(row.getAmount());
            update.setMemo(row.getMemo());
            update.setCleared(Boolean.FALSE);
            update.setPayeeId(row.getResolvedPayeeId());
            update.setPayeeName(row.getResolvedPayeeId() == null ? row.getPayeeText() : row.getResolvedPayeeName());
            if (isInternalTransferRow(row)) {
                update.setTransferredAccountId(row.getResolvedTransferAccountId());
                update.setCategoryId(null);
            } else {
                update.setCategoryId(row.getResolvedCategoryId());
            }
            FinanceTransaction createdTransaction = this.transactionService.createImportedTransaction(
                user,
                transactionImport.getAccountId(),
                update,
                transactionImport.getId()
            );
            createdTransactions.add(createdTransaction);
            persistImportMemory(user, transactionImport.getAccountId(), row);
            createdTransactionCount++;
        }

        transactionImport.setStatus(FinanceTransactionImportStatus.IMPORTED);
        transactionImport.setImportedDateTime(ZonedDateTime.now());
        this.importRepository.save(transactionImport);
        log.info("commitImport:complete importId={} createdTransactionCount={}", importId, createdTransactionCount);

        FinanceTransactionImportCommitResponseDTO response = new FinanceTransactionImportCommitResponseDTO();
        response.setImportId(transactionImport.getId().toString());
        response.setCreatedTransactionCount(createdTransactionCount);
        response.setCorrelationStatus(transactionImport.getCorrelationStatus());
        response.setCorrelationMessage(transactionImport.getCorrelationMessage());
        FinanceTransaction focusTransaction = determineFocusTransaction(createdTransactions);
        if (focusTransaction != null) {
            response.setFocusTransactionId(focusTransaction.getId().toString());
            response.setFocusRowIndex(
                Math.toIntExact(
                    this.transactionRepository.countTransactionsBeforeInDefaultSort(
                        user.getGuid().toString(),
                        transactionImport.getAccountId(),
                        focusTransaction.getDate(),
                        focusTransaction.getNumber(),
                        focusTransaction.getId().toString()
                    )
                )
            );
        }
        return response;
    }

    public void backOutImport(User user, UUID importId) {
        FinanceTransactionImport transactionImport = findImport(user, importId);
        if (transactionImport.getStatus() != FinanceTransactionImportStatus.IMPORTED) {
            throw new IllegalStateException("Only imported batches can be backed out");
        }

        List<FinanceTransaction> importedTransactions = this.transactionRepository.findAllByImportId(importId);
        for (FinanceTransaction transaction : importedTransactions) {
            this.transactionService.deleteTransaction(user, transaction.getAccountId(), transaction.getId());
        }

        transactionImport.setStatus(FinanceTransactionImportStatus.BACKED_OUT);
        transactionImport.setBackedOutDateTime(ZonedDateTime.now());
        this.importRepository.save(transactionImport);
    }

    public void discardDraftImport(User user, UUID importId) {
        FinanceTransactionImport transactionImport = findImport(user, importId);
        if (transactionImport.getStatus() != FinanceTransactionImportStatus.DRAFT) {
            throw new IllegalStateException("Only draft imports can be discarded");
        }

        this.rowRepository.deleteAllByTransactionImport_Id(importId);
        this.importRepository.delete(transactionImport);
        log.info("discardDraftImport importId={} userGuid={}", importId, user.getGuid());
    }

    @Transactional(readOnly = true)
    public List<FinanceTransactionImportHistoryDTO> listImports(User user, String accountId) {
        List<FinanceTransactionImport> imports =
            trimToNull(accountId) == null
                ? this.importRepository.findAllByUserGuidOrderByCreatedDateTimeDesc(user.getGuid().toString())
                : this.importRepository.findAllByUserGuidAndAccountIdOrderByCreatedDateTimeDesc(user.getGuid().toString(), accountId);
        List<FinanceTransactionImport> visibleImports = imports
            .stream()
            .filter(transactionImport -> transactionImport.getStatus() != FinanceTransactionImportStatus.DRAFT)
            .toList();
        log.info("listImports userGuid={} accountId={} count={}", user.getGuid(), accountId, visibleImports.size());

        return visibleImports.stream().map(this::mapHistory).toList();
    }

    private FinanceTransactionImport findImport(User user, UUID importId) {
        return this.importRepository
            .findByIdAndUserGuid(importId, user.getGuid().toString())
            .orElseThrow(() -> new IllegalArgumentException("Import not found"));
    }

    private FinanceAccount findUserAccount(User user, String accountId) {
        return this.accountRepository
            .findById(UUID.fromString(accountId))
            .filter(account -> user.getGuid().toString().equals(account.getUserGuid()))
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private List<ParsedImportRow> parseRowsWithAi(FinanceAccount account, String rawContent, String rawHtml) {
        String prompt = buildPrompt(account, rawContent, rawHtml);
        log.info(
            "parseRowsWithAi:start accountId={} currency={} promptLength={}",
            account.getId(),
            account.getCurrencyCode(),
            prompt.length()
        );
        String responseText = this.aiService
            .generateText(AiServiceCallType.TRANSACTION_IMPORT, prompt)
            .orElseThrow(() ->
                new IllegalStateException("Gemini is not configured yet. Add a Gemini API key in Settings > AI before using imports.")
            )
            .text();
        log.info("parseRowsWithAi:response accountId={} responseLength={}", account.getId(), responseText.length());

        try {
            JsonNode root = this.objectMapper.readTree(stripCodeFence(responseText));
            JsonNode rowsNode = root.isArray() ? root : root.path("transactions");
            if (!rowsNode.isArray()) {
                throw new IllegalArgumentException("The AI import response did not contain a transactions array.");
            }

            List<ParsedImportRow> rows = new ArrayList<>();
            for (JsonNode node : rowsNode) {
                ParsedImportRow row = new ParsedImportRow();
                row.date = parseDate(node.path("date").asText(null));
                row.sourceDateText = trimToNull(node.path("dateText").asText(null));
                row.yearExplicit =
                    node.has("yearExplicit") && !node.get("yearExplicit").isNull() ? node.get("yearExplicit").asBoolean() : null;
                row.transactionKind = trimToNull(node.path("transactionKind").asText(null));
                row.transferAccountText = trimToNull(node.path("transferAccountText").asText(null));
                row.amount = decimalValue(node.get("amount"));
                row.payeeText = trimToNull(node.path("payeeName").asText(null));
                row.memo = trimToNull(node.path("memo").asText(null));
                row.runningBalance = decimalValue(node.get("runningBalance"));
                row.categoryGuess = trimToNull(node.path("categoryGuess").asText(null));
                row.confidence = node.hasNonNull("confidence") ? node.get("confidence").asDouble() : null;
                rows.add(row);
            }
            inferMissingImportYears(account, rows);
            log.info("parseRowsWithAi:parsedRows accountId={} count={}", account.getId(), rows.size());
            return rows;
        } catch (Exception ex) {
            log.error("parseRowsWithAi:parseError accountId={}", account.getId(), ex);
            throw new IllegalArgumentException("The AI import response could not be parsed as transaction JSON.", ex);
        }
    }

    private List<FinanceTransactionImportRow> buildDraftRows(
        User user,
        FinanceTransactionImport transactionImport,
        List<ParsedImportRow> parsedRows
    ) {
        List<FinancePayee> payees = loadPayees(user);
        List<FinanceCategory> categories = loadCategories(user);
        List<FinanceAccount> accounts = loadAccounts(user);
        List<FinanceTransactionImportRow> rows = new ArrayList<>();

        int index = 0;
        for (ParsedImportRow parsedRow : parsedRows) {
            FinanceTransactionImportRow row = new FinanceTransactionImportRow();
            row.setUserGuid(user.getGuid().toString());
            row.setTransactionImport(transactionImport);
            row.setRowIndex(index++);
            row.setDate(parsedRow.date);
            row.setAmount(parsedRow.amount);
            row.setTransactionKind(determineTransactionKind(parsedRow));
            row.setPayeeText(parsedRow.payeeText);
            row.setTransferAccountText(parsedRow.transferAccountText);
            row.setMemo(parsedRow.memo);
            row.setRunningBalance(parsedRow.runningBalance);
            row.setAiCategoryGuess(parsedRow.categoryGuess);
            row.setAiConfidence(parsedRow.confidence);
            autoResolveRow(user, row, payees, categories, accounts);
            row.setDateNeedsReview(row.getDate() == null);
            row.setAmountNeedsReview(row.getAmount() == null);
            rows.add(row);
        }

        applyDuplicateChecks(user, transactionImport.getAccountId(), rows);
        defaultAcceptDuplicateRows(user, rows);
        applyCorrelationChecks(transactionImport, rows);
        return rows;
    }

    private void autoResolveRow(
        User user,
        FinanceTransactionImportRow row,
        List<FinancePayee> payees,
        List<FinanceCategory> categories,
        List<FinanceAccount> accounts
    ) {
        applyTransferResolution(user, row, accounts);
        if (isInternalTransferRow(row)) {
            row.setPayeeNeedsReview(false);
            row.setCategoryNeedsReview(false);
            return;
        }

        row.setResolvedPayeeId(null);
        row.setResolvedPayeeName(null);
        row.setResolvedCategoryId(null);
        row.setResolvedCategoryName(null);

        String normalizedPayee = normalizePayeeText(row.getPayeeText());
        if (normalizedPayee == null) {
            row.setPayeeNeedsReview(true);
            row.setCategoryNeedsReview(true);
            return;
        }

        Optional<FinanceTransactionImportMemory> memory = this.memoryRepository.findFirstByUserGuidAndAccountIdAndNormalizedPayeeText(
            user.getGuid().toString(),
            row.getTransactionImport().getAccountId(),
            normalizedPayee
        );
        if (memory.isEmpty()) {
            memory = this.memoryRepository.findFirstByUserGuidAndAccountIdIsNullAndNormalizedPayeeText(
                user.getGuid().toString(),
                normalizedPayee
            );
        }

        if (memory.isPresent()) {
            applyResolvedPayee(user, row, memory.get().getPayeeId());
            applyResolvedCategory(user, row, memory.get().getCategoryId());
            row.setPayeeNeedsReview(row.getResolvedPayeeId() == null);
            row.setCategoryNeedsReview(row.getResolvedCategoryId() == null);
            return;
        }

        FinancePayee payeeMatch = resolvePayeeMatch(normalizedPayee, payees);
        if (payeeMatch != null) {
            row.setResolvedPayeeId(payeeMatch.getId().toString());
            row.setResolvedPayeeName(payeeMatch.getName());
            row.setPayeeNeedsReview(false);

            FinanceResourceDTO lastCategory = getLastCategoryForPayeeBranch(user, payeeMatch, isDeposit(row.getAmount()));
            if (lastCategory != null) {
                row.setResolvedCategoryId(lastCategory.getId());
                row.setResolvedCategoryName(lastCategory.getName());
                row.setCategoryNeedsReview(false);
                return;
            }
        } else {
            row.setPayeeNeedsReview(true);
        }

        FinanceCategory categoryGuess = resolveCategoryGuess(row.getAiCategoryGuess(), categories);
        if (categoryGuess != null) {
            row.setResolvedCategoryId(categoryGuess.getId().toString());
            row.setResolvedCategoryName(buildCategoryDisplayName(categoryGuess));
        }
        row.setCategoryNeedsReview(true);
    }

    private void applyResolvedPayee(User user, FinanceTransactionImportRow row, String payeeId) {
        if (payeeId == null) {
            row.setResolvedPayeeId(null);
            row.setResolvedPayeeName(null);
            row.setPayeeNeedsReview(true);
            return;
        }

        FinancePayee payee = this.payeeRepository
            .findById(UUID.fromString(payeeId))
            .filter(candidate -> user.getGuid().toString().equals(candidate.getUserGuid()))
            .orElseThrow(() -> new IllegalArgumentException("Payee not found"));
        row.setResolvedPayeeId(payee.getId().toString());
        row.setResolvedPayeeName(payee.getName());
        row.setPayeeNeedsReview(false);

        if (row.getResolvedCategoryId() == null) {
            FinanceResourceDTO lastCategory = getLastCategoryForPayeeBranch(user, payee, isDeposit(row.getAmount()));
            if (lastCategory != null) {
                row.setResolvedCategoryId(lastCategory.getId());
                row.setResolvedCategoryName(lastCategory.getName());
                row.setCategoryNeedsReview(false);
            } else {
                row.setCategoryNeedsReview(true);
            }
        }
    }

    private void applyResolvedCategory(User user, FinanceTransactionImportRow row, String categoryId) {
        if (categoryId == null) {
            row.setResolvedCategoryId(null);
            row.setResolvedCategoryName(null);
            row.setCategoryNeedsReview(true);
            return;
        }

        FinanceCategory category = this.categoryRepository
            .findById(UUID.fromString(categoryId))
            .filter(candidate -> user.getGuid().toString().equals(candidate.getUserGuid()))
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        row.setResolvedCategoryId(category.getId().toString());
        row.setResolvedCategoryName(buildCategoryDisplayName(category));
        row.setCategoryNeedsReview(false);
    }

    private void applyDuplicateResolution(User user, FinanceTransactionImportRow row) {
        FinanceTransaction duplicateTransaction = resolveDuplicateTransaction(user, row);
        if (duplicateTransaction.getTransferredAccountId() != null) {
            FinanceAccount matchedAccount = this.accountRepository
                .findById(UUID.fromString(duplicateTransaction.getTransferredAccountId()))
                .filter(candidate -> user.getGuid().toString().equals(candidate.getUserGuid()))
                .orElse(null);
            row.setTransactionKind(TRANSACTION_KIND_TRANSFER);
            row.setResolvedTransferAccountId(duplicateTransaction.getTransferredAccountId());
            row.setResolvedTransferAccountName(matchedAccount == null ? null : matchedAccount.getName());
            row.setTransferNeedsReview(false);
            row.setExternalTransferLike(false);
            row.setResolvedCategoryId(null);
            row.setResolvedCategoryName(null);
            row.setPayeeNeedsReview(false);
            row.setCategoryNeedsReview(false);
        }

        FinancePayee duplicatePayee = null;
        if (trimToNull(duplicateTransaction.getPayeeId()) != null) {
            duplicatePayee = this.payeeRepository
                .findById(UUID.fromString(duplicateTransaction.getPayeeId()))
                .filter(candidate -> user.getGuid().toString().equals(candidate.getUserGuid()))
                .orElse(null);
        }

        if (duplicatePayee != null) {
            FinancePayee canonicalPayee = resolveCanonicalPayee(user.getGuid().toString(), duplicatePayee);
            row.setResolvedPayeeId(canonicalPayee.getId().toString());
            row.setResolvedPayeeName(canonicalPayee.getName());
            row.setPayeeNeedsReview(false);
            ensureDuplicatePayeeVariant(user, row, canonicalPayee);
        }

        if (duplicateTransaction.getCategory() != null) {
            row.setResolvedCategoryId(duplicateTransaction.getCategory().getId().toString());
            row.setResolvedCategoryName(buildCategoryDisplayName(duplicateTransaction.getCategory()));
            row.setCategoryNeedsReview(false);
        }

        row.setDuplicateConfirmed(true);
        row.setDuplicateRejected(false);
        row.setAccepted(true);
        row.setIgnored(false);
    }

    private void recomputeRowFlags(
        User user,
        FinanceTransactionImportRow row,
        List<FinancePayee> payees,
        List<FinanceCategory> categories,
        List<FinanceAccount> accounts
    ) {
        row.setDateNeedsReview(row.getDate() == null);
        row.setAmountNeedsReview(row.getAmount() == null);
        applyTransferResolution(user, row, accounts);
        if (isInternalTransferRow(row)) {
            row.setPayeeNeedsReview(false);
            row.setCategoryNeedsReview(false);
        } else if (row.getResolvedPayeeId() == null && normalizePayeeText(row.getPayeeText()) != null) {
            FinancePayee payeeMatch = resolvePayeeMatch(normalizePayeeText(row.getPayeeText()), payees);
            row.setPayeeNeedsReview(payeeMatch == null);
        } else {
            row.setPayeeNeedsReview(row.getResolvedPayeeId() == null && !isTransferRow(row));
        }

        if (isInternalTransferRow(row)) {
            row.setResolvedCategoryId(null);
            row.setResolvedCategoryName(null);
            row.setCategoryNeedsReview(false);
        } else if (row.getResolvedCategoryId() == null && trimToNull(row.getAiCategoryGuess()) != null) {
            FinanceCategory categoryGuess = resolveCategoryGuess(row.getAiCategoryGuess(), categories);
            if (categoryGuess != null) {
                row.setResolvedCategoryId(categoryGuess.getId().toString());
                row.setResolvedCategoryName(buildCategoryDisplayName(categoryGuess));
            }
            row.setCategoryNeedsReview(true);
        } else if (row.getResolvedCategoryId() == null) {
            row.setCategoryNeedsReview(true);
        } else {
            row.setCategoryNeedsReview(false);
        }
        if (row.isIgnored()) {
            row.setAccepted(false);
            return;
        }
    }

    private void applyDuplicateChecks(User user, String accountId, List<FinanceTransactionImportRow> rows) {
        FinanceAccount sourceAccount = findUserAccount(user, accountId);
        LocalDate minDate = rows
            .stream()
            .map(FinanceTransactionImportRow::getDate)
            .filter(Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(null);
        LocalDate maxDate = rows
            .stream()
            .map(FinanceTransactionImportRow::getDate)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);
        if (minDate == null || maxDate == null) {
            return;
        }

        List<FinanceTransaction> existing = this.transactionRepository.findAllByUserGuidAndAccountIdAndDateBetween(
            user.getGuid().toString(),
            accountId,
            minDate.minusDays(2),
            maxDate.plusDays(2)
        );
        log.info(
            "duplicateDiagnostics:preload accountId={} userGuid={} startDate={} endDate={} loadedCount={}",
            accountId,
            user.getGuid(),
            minDate.minusDays(2),
            maxDate.plusDays(2),
            existing.size()
        );

        for (FinanceTransactionImportRow row : rows) {
            String normalizedRowPayee = normalizePayeeText(
                row.getResolvedPayeeName() != null ? row.getResolvedPayeeName() : row.getPayeeText()
            );
            if (row.isDuplicateRejected()) {
                logDuplicateRowAnalysis(sourceAccount, row, normalizedRowPayee, List.of(), List.of(), null, false, "duplicateRejected");
                clearDuplicateState(row, true);
                continue;
            }

            List<FinanceTransaction> nearbyDateMatches = existing
                .stream()
                .filter(transaction -> !transaction.isVoided() && !transaction.isSplitChild() && !transaction.isRecurring())
                .filter(transaction -> isWithinDuplicateDateWindow(transaction.getDate(), row.getDate()))
                .filter(transaction -> duplicateCandidateMatchesRow(row, transaction))
                .toList();

            List<FinanceTransaction> exactDateAmountMatches = nearbyDateMatches
                .stream()
                .filter(transaction -> sameAmount(transaction.getAmount(), row.getAmount()))
                .toList();

            FinanceTransaction strongDuplicate = exactDateAmountMatches
                .stream()
                .filter(transaction ->
                    duplicatePayeesLikelyMatch(sourceAccount, row, normalizePayeeText(transaction.getPayeeName()), normalizedRowPayee)
                )
                .max(
                    Comparator.comparingInt((FinanceTransaction transaction) ->
                        duplicatePayeeMatchScore(sourceAccount, row, normalizePayeeText(transaction.getPayeeName()), normalizedRowPayee)
                    ).thenComparingInt(transaction -> -duplicateDateClosenessScore(transaction.getDate(), row.getDate()))
                )
                .orElse(null);

            FinanceTransaction duplicate = strongDuplicate;
            boolean strongMatch = true;
            if (duplicate == null) {
                duplicate = exactDateAmountMatches
                    .stream()
                    .max(
                        Comparator.comparingInt((FinanceTransaction transaction) ->
                            duplicatePayeeMatchScore(sourceAccount, row, normalizePayeeText(transaction.getPayeeName()), normalizedRowPayee)
                        ).thenComparingInt(transaction -> -duplicateDateClosenessScore(transaction.getDate(), row.getDate()))
                    )
                    .orElse(null);
                strongMatch = false;
            }

            logDuplicateRowAnalysis(
                sourceAccount,
                row,
                normalizedRowPayee,
                exactDateAmountMatches,
                nearbyDateMatches,
                duplicate,
                strongMatch,
                "evaluated"
            );
            row.setDuplicateSuspected(duplicate != null);
            row.setDuplicateStrongMatch(duplicate != null && strongMatch);
            if (duplicate != null) {
                row.setDuplicateTransactionId(duplicate.getId().toString());
                row.setDuplicateTransactionDate(duplicate.getDate());
                row.setDuplicateTransactionAmount(duplicate.getAmount());
                row.setDuplicateTransactionPayeeName(trimToNull(duplicate.getPayeeName()));
                if (!strongMatch) {
                    log.info(
                        "duplicateDiagnostics:weakMatch rowId={} rowIndex={} date={} amount={} payee='{}' normalizedPayee='{}' chosenCandidate={}",
                        row.getId(),
                        row.getRowIndex(),
                        row.getDate(),
                        row.getAmount(),
                        trimToNull(row.getPayeeText()),
                        normalizedRowPayee,
                        formatDuplicateCandidateDiagnostic(
                            sourceAccount,
                            row,
                            duplicate,
                            normalizedRowPayee,
                            row.getResolvedCategoryId(),
                            row.getDate()
                        )
                    );
                }
            } else {
                logDuplicateMissDiagnostics(sourceAccount, row, normalizedRowPayee, exactDateAmountMatches, nearbyDateMatches);
                clearDuplicateState(row, false);
            }
        }
    }

    private void logDuplicateRowAnalysis(
        FinanceAccount sourceAccount,
        FinanceTransactionImportRow row,
        String normalizedRowPayee,
        List<FinanceTransaction> exactDateAmountMatches,
        List<FinanceTransaction> nearbyDateMatches,
        FinanceTransaction selectedDuplicate,
        boolean strongMatch,
        String reason
    ) {
        FinanceTransaction bestAmountCandidate = exactDateAmountMatches
            .stream()
            .max(
                Comparator.comparingInt((FinanceTransaction transaction) ->
                    duplicatePayeeMatchScore(sourceAccount, row, normalizePayeeText(transaction.getPayeeName()), normalizedRowPayee)
                ).thenComparingInt(transaction -> -duplicateDateClosenessScore(transaction.getDate(), row.getDate()))
            )
            .orElse(null);
        FinanceTransaction bestNearbyCandidate = nearbyDateMatches
            .stream()
            .max(
                Comparator.comparingInt((FinanceTransaction transaction) ->
                    duplicatePayeeMatchScore(sourceAccount, row, normalizePayeeText(transaction.getPayeeName()), normalizedRowPayee)
                ).thenComparingInt(transaction -> -duplicateDateClosenessScore(transaction.getDate(), row.getDate()))
            )
            .orElse(null);

        log.info(
            "duplicateDiagnostics:rowAnalysis rowId={} rowIndex={} reason={} date={} amount={} payee='{}' normalizedPayee='{}' resolvedCategoryId={} nearbyDateMatchCount={} sameAmountMatchCount={} selectedDuplicateId={} strongMatch={} bestAmountCandidate={} bestNearbyCandidate={}",
            row.getId(),
            row.getRowIndex(),
            reason,
            row.getDate(),
            row.getAmount(),
            trimToNull(row.getPayeeText()),
            normalizedRowPayee,
            row.getResolvedCategoryId(),
            nearbyDateMatches.size(),
            exactDateAmountMatches.size(),
            selectedDuplicate == null ? null : selectedDuplicate.getId(),
            selectedDuplicate != null && strongMatch,
            bestAmountCandidate == null
                ? null
                : formatDuplicateCandidateDiagnostic(
                      sourceAccount,
                      row,
                      bestAmountCandidate,
                      normalizedRowPayee,
                      row.getResolvedCategoryId(),
                      row.getDate()
                  ),
            bestNearbyCandidate == null
                ? null
                : formatDuplicateCandidateDiagnostic(
                      sourceAccount,
                      row,
                      bestNearbyCandidate,
                      normalizedRowPayee,
                      row.getResolvedCategoryId(),
                      row.getDate()
                  )
        );
    }

    private void applyCorrelationChecks(FinanceTransactionImport transactionImport, List<FinanceTransactionImportRow> rows) {
        List<FinanceTransactionImportRow> datedRows = rows
            .stream()
            .filter(row -> row.getDate() != null && row.getAmount() != null)
            .sorted(Comparator.comparing(FinanceTransactionImportRow::getDate).thenComparing(FinanceTransactionImportRow::getRowIndex))
            .toList();

        if (datedRows.isEmpty()) {
            transactionImport.setCorrelationStatus("UNCORRELATED");
            transactionImport.setCorrelationMessage("No dated rows were available to correlate.");
            return;
        }

        BigDecimal openingBalance = Optional.ofNullable(
            this.transactionRepository.findSumTransactionsForAccountUpToDate(transactionImport.getAccountId(), datedRows.get(0).getDate())
        ).orElse(BigDecimal.ZERO);

        boolean hasRunningBalances = rows.stream().anyMatch(row -> row.getRunningBalance() != null);
        if (hasRunningBalances) {
            BigDecimal computed = openingBalance;
            boolean mismatch = false;
            for (FinanceTransactionImportRow row : datedRows) {
                computed = computed.add(row.getAmount());
                if (row.getRunningBalance() != null && !sameAmount(computed, row.getRunningBalance())) {
                    row.setBalanceMismatch(true);
                    mismatch = true;
                }
            }
            transactionImport.setCorrelationStatus(mismatch ? "RUNNING_BALANCE_MISMATCH" : "RUNNING_BALANCE_MATCH");
            transactionImport.setCorrelationMessage(
                mismatch
                    ? "At least one pasted running balance did not align with the reconstructed account balance."
                    : "Running balances aligned with the reconstructed account balance."
            );
            return;
        }

        if (transactionImport.getExpectedEndingBalance() != null) {
            BigDecimal computedEndingBalance = openingBalance;
            for (FinanceTransactionImportRow row : datedRows) {
                computedEndingBalance = computedEndingBalance.add(row.getAmount());
            }
            boolean matches = sameAmount(computedEndingBalance, transactionImport.getExpectedEndingBalance());
            if (!matches) {
                datedRows.get(datedRows.size() - 1).setBalanceMismatch(true);
            }
            transactionImport.setCorrelationStatus(matches ? "EXPECTED_ENDING_BALANCE_MATCH" : "EXPECTED_ENDING_BALANCE_MISMATCH");
            transactionImport.setCorrelationMessage(
                matches
                    ? "The supplied ending balance matched the reconstructed result."
                    : "The supplied ending balance did not match the reconstructed result."
            );
            return;
        }

        transactionImport.setCorrelationStatus("UNCORRELATED");
        transactionImport.setCorrelationMessage(
            "No running balance or expected ending balance was available, so the import could not be correlated automatically."
        );
    }

    private void refreshImportSummary(User user, FinanceTransactionImport transactionImport, List<FinanceTransactionImportRow> rows) {
        applyCorrelationChecks(transactionImport, rows);
        int flaggedRows = 0;
        for (FinanceTransactionImportRow row : rows) {
            if (rowRequiresAttention(row)) {
                flaggedRows++;
            }
        }
        this.rowRepository.saveAll(rows);
        transactionImport.setTotalRows(rows.size());
        transactionImport.setFlaggedRows(flaggedRows);
        this.importRepository.save(transactionImport);
    }

    private void defaultAcceptDuplicateRows(User user, List<FinanceTransactionImportRow> rows) {
        for (FinanceTransactionImportRow row : rows) {
            if (row.isIgnored() || row.isDuplicateConfirmed() || !row.isDuplicateSuspected() || !row.isDuplicateStrongMatch()) {
                continue;
            }
            applyDuplicateResolution(user, row);
        }
    }

    private void clearDuplicateState(FinanceTransactionImportRow row, boolean preserveRejected) {
        row.setDuplicateSuspected(false);
        row.setDuplicateStrongMatch(false);
        row.setDuplicateTransactionId(null);
        row.setDuplicateTransactionDate(null);
        row.setDuplicateTransactionAmount(null);
        row.setDuplicateTransactionPayeeName(null);
        row.setDuplicateConfirmed(false);
        if (!preserveRejected) {
            row.setDuplicateRejected(false);
        }
    }

    private FinanceTransaction determineFocusTransaction(List<FinanceTransaction> createdTransactions) {
        return createdTransactions
            .stream()
            .filter(transaction -> transaction.getDate() != null)
            .max(
                Comparator.comparing(FinanceTransaction::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(FinanceTransaction::getNumber, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(FinanceTransaction::getId, Comparator.nullsLast(Comparator.naturalOrder()))
            )
            .orElse(null);
    }

    private void persistImportMemory(User user, String accountId, FinanceTransactionImportRow row) {
        if (isInternalTransferRow(row)) {
            persistTransferImportMemory(user, accountId, row);
            return;
        }
        String normalizedPayee = normalizePayeeText(row.getPayeeText());
        if (normalizedPayee == null || row.getResolvedPayeeId() == null) {
            return;
        }

        FinanceTransactionImportMemory memory = this.memoryRepository
            .findFirstByUserGuidAndAccountIdAndNormalizedPayeeText(user.getGuid().toString(), accountId, normalizedPayee)
            .orElseGet(FinanceTransactionImportMemory::new);
        memory.setUserGuid(user.getGuid().toString());
        memory.setAccountId(accountId);
        memory.setNormalizedPayeeText(normalizedPayee);
        memory.setPayeeId(row.getResolvedPayeeId());
        memory.setCategoryId(row.getResolvedCategoryId());
        memory.setUsageCount(memory.getUsageCount() == null ? 1 : memory.getUsageCount() + 1);
        if (memory.getCreatedDateTime() == null) {
            memory.setCreatedDateTime(ZonedDateTime.now());
        }
        memory.setLastUsedDateTime(ZonedDateTime.now());
        this.memoryRepository.save(memory);
    }

    private void persistTransferImportMemory(User user, String accountId, FinanceTransactionImportRow row) {
        String normalizedTransferText = normalizePayeeText(row.getTransferAccountText());
        if (normalizedTransferText == null || row.getResolvedTransferAccountId() == null) {
            return;
        }

        FinanceTransactionImportTransferMemory memory = this.transferMemoryRepository
            .findFirstByUserGuidAndSourceAccountIdIsNullAndNormalizedTransferText(user.getGuid().toString(), normalizedTransferText)
            .orElseGet(FinanceTransactionImportTransferMemory::new);
        memory.setUserGuid(user.getGuid().toString());
        memory.setSourceAccountId(null);
        memory.setNormalizedTransferText(normalizedTransferText);
        memory.setTransferAccountId(row.getResolvedTransferAccountId());
        memory.setUsageCount(memory.getUsageCount() == null ? 1 : memory.getUsageCount() + 1);
        if (memory.getCreatedDateTime() == null) {
            memory.setCreatedDateTime(ZonedDateTime.now());
        }
        memory.setLastUsedDateTime(ZonedDateTime.now());
        this.transferMemoryRepository.save(memory);
    }

    private FinanceTransactionImportDTO mapImport(FinanceTransactionImport transactionImport, List<FinanceTransactionImportRow> rows) {
        FinanceTransactionImportDTO dto = new FinanceTransactionImportDTO();
        dto.setImportId(transactionImport.getId().toString());
        dto.setAccountId(transactionImport.getAccountId());
        dto.setAccountName(transactionImport.getAccountName());
        dto.setStatus(transactionImport.getStatus().name());
        dto.setCorrelationStatus(transactionImport.getCorrelationStatus());
        dto.setCorrelationMessage(transactionImport.getCorrelationMessage());
        dto.setTotalRows(transactionImport.getTotalRows());
        dto.setFlaggedRows(transactionImport.getFlaggedRows());
        dto.setCreatedDateTime(transactionImport.getCreatedDateTime() == null ? null : transactionImport.getCreatedDateTime().toString());
        dto.setRows(rows.stream().map(this::mapRow).toList());
        return dto;
    }

    private FinanceTransactionImportHistoryDTO mapHistory(FinanceTransactionImport transactionImport) {
        FinanceTransactionImportHistoryDTO dto = new FinanceTransactionImportHistoryDTO();
        dto.setImportId(transactionImport.getId().toString());
        dto.setAccountId(transactionImport.getAccountId());
        dto.setAccountName(transactionImport.getAccountName());
        dto.setStatus(transactionImport.getStatus().name());
        dto.setCreatedDateTime(transactionImport.getCreatedDateTime() == null ? null : transactionImport.getCreatedDateTime().toString());
        dto.setTotalRows(transactionImport.getTotalRows());
        dto.setFlaggedRows(transactionImport.getFlaggedRows());
        dto.setCorrelationStatus(transactionImport.getCorrelationStatus());
        return dto;
    }

    private FinanceTransactionImportRowDTO mapRow(FinanceTransactionImportRow row) {
        FinanceTransactionImportRowDTO dto = new FinanceTransactionImportRowDTO();
        dto.setId(row.getId().toString());
        dto.setRowIndex(row.getRowIndex());
        dto.setDate(row.getDate());
        dto.setAmount(row.getAmount());
        dto.setTransactionKind(row.getTransactionKind());
        dto.setPayeeText(row.getPayeeText());
        dto.setTransferAccountText(row.getTransferAccountText());
        dto.setMemo(row.getMemo());
        dto.setRunningBalance(row.getRunningBalance());
        dto.setAiCategoryGuess(row.getAiCategoryGuess());
        dto.setAiConfidence(row.getAiConfidence());
        dto.setResolvedPayeeId(row.getResolvedPayeeId());
        dto.setResolvedPayeeName(row.getResolvedPayeeName());
        dto.setResolvedCategoryId(row.getResolvedCategoryId());
        dto.setResolvedCategoryName(row.getResolvedCategoryName());
        dto.setResolvedTransferAccountId(row.getResolvedTransferAccountId());
        dto.setResolvedTransferAccountName(row.getResolvedTransferAccountName());
        dto.setPayeeNeedsReview(row.isPayeeNeedsReview());
        dto.setCategoryNeedsReview(row.isCategoryNeedsReview());
        dto.setTransferNeedsReview(row.isTransferNeedsReview());
        dto.setExternalTransferLike(row.isExternalTransferLike());
        dto.setDateNeedsReview(row.isDateNeedsReview());
        dto.setAmountNeedsReview(row.isAmountNeedsReview());
        dto.setBalanceMismatch(row.isBalanceMismatch());
        dto.setDuplicateSuspected(row.isDuplicateSuspected());
        dto.setDuplicateStrongMatch(row.isDuplicateStrongMatch());
        dto.setDuplicateTransactionId(row.getDuplicateTransactionId());
        dto.setDuplicateTransactionDate(row.getDuplicateTransactionDate());
        dto.setDuplicateTransactionAmount(row.getDuplicateTransactionAmount());
        dto.setDuplicateTransactionPayeeName(row.getDuplicateTransactionPayeeName());
        dto.setDuplicateConfirmed(row.isDuplicateConfirmed());
        dto.setDuplicateRejected(row.isDuplicateRejected());
        dto.setAccepted(row.isAccepted());
        dto.setIgnored(row.isIgnored());
        return dto;
    }

    private boolean rowRequiresAttention(FinanceTransactionImportRow row) {
        if (row.isDuplicateConfirmed() || row.isIgnored()) {
            return false;
        }
        return (
            row.isTransferNeedsReview() ||
            row.isPayeeNeedsReview() ||
            row.isCategoryNeedsReview() ||
            row.isDateNeedsReview() ||
            row.isAmountNeedsReview() ||
            row.isBalanceMismatch() ||
            row.isDuplicateSuspected()
        );
    }

    private boolean isTransferRow(FinanceTransactionImportRow row) {
        return TRANSACTION_KIND_TRANSFER.equalsIgnoreCase(trimToNull(row.getTransactionKind()));
    }

    private boolean isInternalTransferRow(FinanceTransactionImportRow row) {
        return isTransferRow(row) && !row.isExternalTransferLike() && trimToNull(row.getResolvedTransferAccountId()) != null;
    }

    private List<FinancePayee> loadPayees(User user) {
        return this.payeeRepository.findByUserGuidOrderByNameAsc(user.getGuid().toString());
    }

    private List<FinanceCategory> loadCategories(User user) {
        return this.categoryRepository.findAllByClassificationIdAndUserGuid(0, user.getGuid().toString());
    }

    private List<FinanceAccount> loadAccounts(User user) {
        return this.accountRepository.findAllByUserGuid(user.getGuid().toString());
    }

    private void applyTransferResolution(User user, FinanceTransactionImportRow row, List<FinanceAccount> accounts) {
        if (!isTransferRow(row)) {
            row.setResolvedTransferAccountId(null);
            row.setResolvedTransferAccountName(null);
            row.setTransferNeedsReview(false);
            row.setExternalTransferLike(false);
            return;
        }

        if (trimToNull(row.getResolvedTransferAccountId()) != null) {
            applyResolvedTransferAccount(user, row, row.getResolvedTransferAccountId());
            return;
        }

        FinanceAccount matchedAccount = resolveTransferAccountMatch(
            user.getGuid().toString(),
            row.getTransactionImport().getAccountId(),
            getTransferMatchText(row),
            accounts
        );
        if (matchedAccount != null) {
            row.setResolvedTransferAccountId(matchedAccount.getId().toString());
            row.setResolvedTransferAccountName(matchedAccount.getName());
            row.setTransferNeedsReview(false);
            row.setExternalTransferLike(false);
            if (trimToNull(row.getTransferAccountText()) == null) {
                row.setTransferAccountText(extractTransferAccountText(row.getPayeeText()));
            }
            row.setResolvedCategoryId(null);
            row.setResolvedCategoryName(null);
            return;
        }

        row.setResolvedTransferAccountId(null);
        row.setResolvedTransferAccountName(null);
        row.setTransferNeedsReview(!row.isExternalTransferLike());
        if (trimToNull(getTransferMatchText(row)) != null) {
            row.setExternalTransferLike(true);
        }
    }

    private String getTransferMatchText(FinanceTransactionImportRow row) {
        String transferText = trimToNull(row.getTransferAccountText());
        if (transferText != null) {
            return transferText;
        }
        if (!isTransferRow(row)) {
            return null;
        }
        return extractTransferAccountText(row.getPayeeText());
    }

    private String extractTransferAccountText(String rawTransferLikeText) {
        String value = trimToNull(rawTransferLikeText);
        if (value == null) {
            return null;
        }

        String normalized = value
            .replaceFirst("(?i)^payment\\s+to\\s+", "")
            .replaceFirst("(?i)^payment\\s+from\\s+", "")
            .replaceFirst("(?i)^transfer\\s+to\\s+", "")
            .replaceFirst("(?i)^transfer\\s+from\\s+", "")
            .replaceFirst("(?i)^to\\s+", "")
            .replaceFirst("(?i)^from\\s+", "");

        int separatorIndex = normalized.indexOf(" - ");
        if (separatorIndex > 0) {
            normalized = normalized.substring(0, separatorIndex);
        }

        normalized = normalized.replaceFirst("(?i)\\s+internal\\s+transfer$", "").trim();
        return trimToNull(normalized);
    }

    private void applyResolvedTransferAccount(User user, FinanceTransactionImportRow row, String transferAccountId) {
        if (transferAccountId == null) {
            row.setResolvedTransferAccountId(null);
            row.setResolvedTransferAccountName(null);
            row.setTransferNeedsReview(!row.isExternalTransferLike());
            if (!row.isExternalTransferLike()) {
                row.setTransactionKind(isDeposit(row.getAmount()) ? TRANSACTION_KIND_DEPOSIT : TRANSACTION_KIND_WITHDRAWAL);
            }
            return;
        }

        FinanceAccount transferAccount = this.accountRepository
            .findById(UUID.fromString(transferAccountId))
            .filter(candidate -> user.getGuid().toString().equals(candidate.getUserGuid()))
            .filter(candidate -> !candidate.getId().toString().equals(row.getTransactionImport().getAccountId()))
            .orElseThrow(() -> new IllegalArgumentException("Transfer account not found"));
        row.setTransactionKind(TRANSACTION_KIND_TRANSFER);
        row.setResolvedTransferAccountId(transferAccount.getId().toString());
        row.setResolvedTransferAccountName(transferAccount.getName());
        row.setTransferNeedsReview(false);
        row.setExternalTransferLike(false);
        row.setResolvedCategoryId(null);
        row.setResolvedCategoryName(null);
    }

    private FinancePayee resolvePayeeMatch(String normalizedPayee, List<FinancePayee> payees) {
        Map<String, FinancePayee> exactMatches = new LinkedHashMap<>();
        for (FinancePayee payee : payees) {
            String normalizedName = normalizePayeeText(payee.getName());
            if (normalizedPayee.equals(normalizedName)) {
                exactMatches.putIfAbsent(payee.getId().toString(), payee);
            }
        }
        if (exactMatches.size() == 1) {
            return exactMatches.values().iterator().next();
        }

        FinancePayee fuzzyMatch = null;
        for (FinancePayee payee : payees) {
            String normalizedName = normalizePayeeText(payee.getName());
            if (
                normalizedName != null &&
                (normalizedPayee.contains(normalizedName) || normalizedName.contains(normalizedPayee)) &&
                normalizedName.length() >= 4
            ) {
                if (fuzzyMatch != null) {
                    return null;
                }
                fuzzyMatch = payee;
            }
        }
        return fuzzyMatch;
    }

    private FinanceAccount resolveTransferAccountMatch(
        String userGuid,
        String sourceAccountId,
        String transferAccountText,
        List<FinanceAccount> accounts
    ) {
        String normalizedTransferText = normalizePayeeText(transferAccountText);
        if (normalizedTransferText == null) {
            return null;
        }

        Optional<FinanceTransactionImportTransferMemory> memory =
            this.transferMemoryRepository.findFirstByUserGuidAndSourceAccountIdAndNormalizedTransferText(
                userGuid,
                sourceAccountId,
                normalizedTransferText
            );
        if (memory.isEmpty()) {
            memory = this.transferMemoryRepository.findFirstByUserGuidAndSourceAccountIdIsNullAndNormalizedTransferText(
                userGuid,
                normalizedTransferText
            );
        }
        if (memory.isPresent()) {
            String matchedTransferAccountId = memory.get().getTransferAccountId();
            return accounts
                .stream()
                .filter(account -> account.getId() != null && matchedTransferAccountId.equals(account.getId().toString()))
                .filter(account -> !account.getId().toString().equals(sourceAccountId))
                .findFirst()
                .orElse(null);
        }

        FinanceAccount bestMatch = null;
        int bestScore = 0;
        boolean ambiguous = false;
        for (FinanceAccount account : accounts) {
            if (account.getId() == null || account.getId().toString().equals(sourceAccountId)) {
                continue;
            }

            int score = scoreTransferAccountMatch(normalizedTransferText, account, accounts);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = account;
                ambiguous = false;
            } else if (score > 0 && score == bestScore) {
                ambiguous = true;
            }
        }

        if (ambiguous || bestScore <= 0) {
            return null;
        }
        return bestMatch;
    }

    private int scoreTransferAccountMatch(String normalizedTransferText, FinanceAccount account, List<FinanceAccount> accounts) {
        int bestScore = 0;
        for (String alias : collectTransferAccountAliases(account, accounts)) {
            String normalizedAlias = normalizePayeeText(alias);
            if (normalizedAlias == null) {
                continue;
            }
            if (normalizedTransferText.equals(normalizedAlias)) {
                bestScore = Math.max(bestScore, 6);
                continue;
            }
            if (normalizedTransferText.contains(normalizedAlias) || normalizedAlias.contains(normalizedTransferText)) {
                bestScore = Math.max(bestScore, 5);
                continue;
            }

            List<String> leftTokens = significantPayeeTokens(normalizedTransferText);
            List<String> rightTokens = significantPayeeTokens(normalizedAlias);
            if (leftTokens.equals(rightTokens) && !leftTokens.isEmpty()) {
                bestScore = Math.max(bestScore, 4);
            } else if (
                (isTokenSubset(leftTokens, rightTokens) || isTokenSubset(rightTokens, leftTokens)) &&
                !leftTokens.isEmpty() &&
                !rightTokens.isEmpty()
            ) {
                bestScore = Math.max(bestScore, 3);
            }
        }
        return bestScore;
    }

    private List<String> collectTransferAccountAliases(FinanceAccount account, List<FinanceAccount> accounts) {
        java.util.LinkedHashSet<String> aliases = new java.util.LinkedHashSet<>();
        if (trimToNull(account.getName()) != null) {
            aliases.add(account.getName());
        }
        if (trimToNull(account.getRelatedToAccountId()) != null) {
            accounts
                .stream()
                .filter(candidate -> candidate.getId() != null)
                .filter(candidate -> candidate.getId().toString().equals(account.getRelatedToAccountId()))
                .map(FinanceAccount::getName)
                .filter(Objects::nonNull)
                .forEach(aliases::add);
        }
        return new ArrayList<>(aliases);
    }

    private FinanceCategory resolveCategoryGuess(String categoryGuess, List<FinanceCategory> categories) {
        String normalizedGuess = trimToNull(categoryGuess);
        if (normalizedGuess == null) {
            return null;
        }

        for (FinanceCategory category : categories) {
            if (
                normalizedGuess.equalsIgnoreCase(category.getName()) || normalizedGuess.equalsIgnoreCase(buildCategoryDisplayName(category))
            ) {
                return category;
            }
        }
        return null;
    }

    private FinanceResourceDTO getLastCategoryForPayeeBranch(User user, FinancePayee payee, boolean deposit) {
        List<String> payeeIds = collectPayeeBranchIds(user.getGuid().toString(), payee);
        return this.transactionRepository
            .findRecentCategorisedByUserGuidAndPayeeIdsAndDirection(
                user.getGuid().toString(),
                payeeIds,
                deposit,
                org.springframework.data.domain.Pageable.ofSize(1)
            )
            .stream()
            .findFirst()
            .map(FinanceTransaction::getCategory)
            .filter(Objects::nonNull)
            .map(category -> new FinanceResourceDTO(category.getId().toString(), buildCategoryDisplayName(category)))
            .orElse(null);
    }

    private FinanceTransaction resolveDuplicateTransaction(User user, FinanceTransactionImportRow row) {
        String duplicateTransactionId = trimToNull(row.getDuplicateTransactionId());
        if (duplicateTransactionId == null) {
            throw new IllegalStateException("No duplicate transaction is available to confirm for this row.");
        }

        return this.transactionRepository
            .findByIdAndUserGuid(UUID.fromString(duplicateTransactionId), user.getGuid().toString())
            .orElseThrow(() -> new IllegalArgumentException("Matched duplicate transaction not found"));
    }

    private FinancePayee resolveCanonicalPayee(String userGuid, FinancePayee payee) {
        FinancePayee current = payee;
        while (trimToNull(current.getParentId()) != null) {
            FinancePayee parent = this.payeeRepository
                .findById(UUID.fromString(current.getParentId()))
                .filter(candidate -> userGuid.equals(candidate.getUserGuid()))
                .orElse(null);
            if (parent == null) {
                break;
            }
            current = parent;
        }
        return current;
    }

    private void ensureDuplicatePayeeVariant(User user, FinanceTransactionImportRow row, FinancePayee canonicalPayee) {
        String variantName = trimToNull(row.getPayeeText());
        if (variantName == null) {
            return;
        }

        String normalizedVariantName = normalizePayeeText(variantName);
        if (normalizedVariantName == null || normalizedVariantName.equals(normalizePayeeText(canonicalPayee.getName()))) {
            return;
        }

        Optional<FinancePayee> existingPayee = this.payeeRepository.findByUserGuidAndNameIgnoreCase(user.getGuid().toString(), variantName);
        if (existingPayee.isPresent()) {
            return;
        }

        FinancePayee variant = new FinancePayee();
        variant.setUserGuid(user.getGuid().toString());
        variant.setName(variantName);
        variant.setParentId(canonicalPayee.getId().toString());
        variant.setHidden(Boolean.FALSE);
        variant.setSerialDateTime(ZonedDateTime.now());
        this.payeeRepository.save(variant);
    }

    private List<String> collectPayeeBranchIds(String userGuid, FinancePayee root) {
        List<FinancePayee> payees = this.payeeRepository.findByUserGuidOrderByNameAsc(userGuid);
        List<String> payeeIds = new ArrayList<>();
        appendPayeeBranchId(root, payees, payeeIds);
        return payeeIds;
    }

    private void appendPayeeBranchId(FinancePayee payee, List<FinancePayee> payees, List<String> payeeIds) {
        payeeIds.add(payee.getId().toString());
        payees
            .stream()
            .filter(candidate -> payee.getId().toString().equals(candidate.getParentId()))
            .forEach(child -> appendPayeeBranchId(child, payees, payeeIds));
    }

    private String buildCategoryDisplayName(FinanceCategory category) {
        if (category.getParent() == null || category.getParent().getName() == null) {
            return category.getName();
        }
        return category.getParent().getName() + ": " + category.getName();
    }

    private String determineTransactionKind(ParsedImportRow parsedRow) {
        String normalizedKind = trimToNull(parsedRow.transactionKind);
        if (normalizedKind != null) {
            normalizedKind = normalizedKind.toUpperCase(Locale.ROOT);
            if (Set.of(TRANSACTION_KIND_TRANSFER, TRANSACTION_KIND_DEPOSIT, TRANSACTION_KIND_WITHDRAWAL).contains(normalizedKind)) {
                return normalizedKind;
            }
        }

        if (trimToNull(parsedRow.transferAccountText) != null || looksLikeTransferText(parsedRow.payeeText)) {
            return TRANSACTION_KIND_TRANSFER;
        }

        return isDeposit(parsedRow.amount) ? TRANSACTION_KIND_DEPOSIT : TRANSACTION_KIND_WITHDRAWAL;
    }

    private boolean looksLikeTransferText(String value) {
        String normalized = normalizePayeeText(value);
        if (normalized == null) {
            return false;
        }
        return (
            normalized.contains("transfer") ||
            normalized.startsWith("payment to ") ||
            normalized.startsWith("payment from ") ||
            normalized.startsWith("to ") ||
            normalized.startsWith("from ")
        );
    }

    private String buildPrompt(FinanceAccount account, String rawContent, String rawHtml) {
        String htmlSection =
            rawHtml == null
                ? "HTML clipboard content:\n[none supplied]\n"
                : """
                  HTML clipboard content:
                  %s
                  """.formatted(rawHtml);
        return """
        Extract standard bank or credit-card transactions from the pasted content.
        Return JSON only. Do not include markdown fences or commentary.
        Expected schema:
        {
          "transactions": [
            {
              "date": "YYYY-MM-DD",
              "dateText": "string as shown in source",
              "yearExplicit": true,
              "transactionKind": "WITHDRAWAL | DEPOSIT | TRANSFER",
              "transferAccountText": "string or null",
              "amount": 0.00,
              "payeeName": "string",
              "memo": "string or null",
              "runningBalance": 0.00 or null,
              "categoryGuess": "string or null",
              "confidence": 0.0
            }
          ]
        }

        Rules:
        - Include ordinary debit/credit rows and transfer-like rows.
        - Use negative amounts for withdrawals/spend and positive amounts for deposits/refunds.
        - Skip headers, totals, pending-only markers, and unrelated text.
        - If a field is unknown, use null.
        - Preserve payee text as closely as practical.
        - Use transactionKind TRANSFER when the source looks like money moving to or from another account, card, or transfer destination.
        - For transfer-like rows, set transferAccountText to the account/card/destination text if it can be identified from the pasted content.
        - For ordinary rows, set transactionKind to WITHDRAWAL or DEPOSIT based on the sign of the amount and leave transferAccountText null.
        - Set dateText to the date exactly as shown in the source where possible.
        - Set yearExplicit to true only when the source itself clearly shows a year for that transaction date.
        - If the source omits the year, infer the most likely year, defaulting to the current year (%s) instead of an older year.
        - If HTML clipboard content is supplied, prefer it because it may preserve table structure.
        - Account currency is %s.

        Plain text clipboard content:
        %s

        %s
        """.formatted(LocalDate.now().getYear(), account.getCurrencyCode(), rawContent, htmlSection);
    }

    private String buildStoredRawContent(String rawContent, String rawHtml) {
        if (rawHtml == null) {
            return rawContent;
        }

        return """
        Plain text clipboard content:
        %s

        HTML clipboard content:
        %s
        """.formatted(rawContent, rawHtml);
    }

    private String stripCodeFence(String text) {
        String trimmed = trimToNull(text);
        if (trimmed == null) {
            return "";
        }
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private LocalDate parseDate(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private void inferMissingImportYears(FinanceAccount account, List<ParsedImportRow> rows) {
        List<ParsedImportRow> rowsNeedingYearInference = rows
            .stream()
            .filter(row -> row.date != null)
            .filter(row -> Boolean.FALSE.equals(row.yearExplicit) || sourceDateTextLacksYear(row.sourceDateText))
            .toList();
        if (rowsNeedingYearInference.isEmpty()) {
            return;
        }

        int fallbackYear = LocalDate.now().getYear();
        Integer inferredYear = inferBestImportYearFromHistory(account, rowsNeedingYearInference, fallbackYear);
        int chosenYear = inferredYear == null ? fallbackYear : inferredYear;
        for (ParsedImportRow row : rowsNeedingYearInference) {
            row.date = applyYear(row.date, chosenYear);
        }
        log.info(
            "parseRowsWithAi:inferredMissingYear accountId={} chosenYear={} fallbackYear={} rowsAffected={} sourceDates={}",
            account.getId(),
            chosenYear,
            fallbackYear,
            rowsNeedingYearInference.size(),
            rowsNeedingYearInference
                .stream()
                .map(row -> row.sourceDateText != null ? row.sourceDateText : row.date.toString())
                .toList()
        );
    }

    private Integer inferBestImportYearFromHistory(FinanceAccount account, List<ParsedImportRow> rows, int fallbackYear) {
        List<FinanceTransaction> existingTransactions = this.transactionRepository
            .findAllByAccountIdOrderByDate(account.getId().toString())
            .stream()
            .filter(transaction -> !transaction.isVoided() && !transaction.isSplitChild() && !transaction.isRecurring())
            .filter(transaction -> transaction.getDate() != null)
            .toList();
        if (existingTransactions.isEmpty()) {
            return null;
        }

        int startYear = fallbackYear - 1;
        int endYear = fallbackYear + 1;
        Integer bestYear = null;
        int bestScore = Integer.MIN_VALUE;

        for (int candidateYear = startYear; candidateYear <= endYear; candidateYear++) {
            int score = scoreCandidateImportYear(rows, existingTransactions, candidateYear);
            if (score > bestScore) {
                bestScore = score;
                bestYear = candidateYear;
            }
        }

        if (bestScore <= 0) {
            return null;
        }
        return bestYear;
    }

    private int scoreCandidateImportYear(List<ParsedImportRow> rows, List<FinanceTransaction> existingTransactions, int candidateYear) {
        int score = 0;
        for (ParsedImportRow row : rows) {
            LocalDate candidateDate = applyYear(row.date, candidateYear);
            if (candidateDate == null) {
                continue;
            }
            List<FinanceTransaction> nearbyTransactions = existingTransactions
                .stream()
                .filter(transaction -> isWithinDuplicateDateWindow(transaction.getDate(), candidateDate))
                .toList();
            long sameAmountMatches = nearbyTransactions
                .stream()
                .filter(transaction -> sameAmount(transaction.getAmount(), row.amount))
                .count();
            score += (int) sameAmountMatches * 10;
            if (!nearbyTransactions.isEmpty()) {
                score += 1;
            }
        }
        return score;
    }

    private LocalDate applyYear(LocalDate date, int year) {
        if (date == null) {
            return null;
        }
        int dayOfMonth = Math.min(date.getDayOfMonth(), java.time.Month.of(date.getMonthValue()).length(java.time.Year.isLeap(year)));
        return LocalDate.of(year, date.getMonthValue(), dayOfMonth);
    }

    private boolean sourceDateTextLacksYear(String sourceDateText) {
        String normalized = trimToNull(sourceDateText);
        if (normalized == null) {
            return true;
        }
        return !normalized.matches(".*\\b\\d{4}\\b.*");
    }

    private BigDecimal decimalValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = trimToNull(node.asText());
        if (value == null) {
            return null;
        }
        value = value.replace(",", "").replace("$", "");
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isDeposit(BigDecimal amount) {
        return amount != null && amount.signum() >= 0;
    }

    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return false;
        }
        return left.subtract(right).abs().compareTo(new BigDecimal("0.01")) <= 0;
    }

    private boolean duplicatePayeesLikelyMatch(FinanceAccount sourceAccount, FinanceTransactionImportRow row, String left, String right) {
        return duplicatePayeeMatchScore(sourceAccount, row, left, right) > 0;
    }

    private boolean duplicateCandidateMatchesRow(FinanceTransactionImportRow row, FinanceTransaction transaction) {
        if (isInternalTransferRow(row)) {
            return (
                transaction.isTransfer() &&
                Objects.equals(trimToNull(transaction.getTransferredAccountId()), trimToNull(row.getResolvedTransferAccountId()))
            );
        }

        if (isTransferRow(row) && !row.isExternalTransferLike()) {
            return transaction.isTransfer();
        }

        if (isPotentialTransferDuplicateRow(row)) {
            // When the pasted row looks transfer-like but has not been fully resolved yet,
            // keep transfer transactions in the candidate pool so the user can review them.
            return true;
        }

        return !transaction.isTransfer();
    }

    private boolean isPotentialTransferDuplicateRow(FinanceTransactionImportRow row) {
        return (
            isTransferRow(row) ||
            trimToNull(row.getTransferAccountText()) != null ||
            trimToNull(row.getResolvedTransferAccountId()) != null ||
            trimToNull(row.getResolvedTransferAccountName()) != null ||
            looksLikeTransferText(row.getPayeeText()) ||
            looksLikeTransferText(row.getResolvedPayeeName())
        );
    }

    private boolean isWithinDuplicateDateWindow(LocalDate left, LocalDate right) {
        if (left == null || right == null) {
            return false;
        }
        return Math.abs(java.time.temporal.ChronoUnit.DAYS.between(left, right)) <= 2;
    }

    private int duplicateDateClosenessScore(LocalDate left, LocalDate right) {
        if (left == null || right == null) {
            return Integer.MIN_VALUE;
        }
        return 2 - (int) Math.abs(java.time.temporal.ChronoUnit.DAYS.between(left, right));
    }

    private int duplicatePayeeMatchScore(FinanceAccount sourceAccount, FinanceTransactionImportRow row, String left, String right) {
        if (left == null || right == null) {
            return 0;
        }
        if (left.equals(right)) {
            return 4;
        }
        if (left.contains(right) || right.contains(left)) {
            return 3;
        }

        List<String> leftTokens = significantPayeeTokens(left);
        List<String> rightTokens = significantPayeeTokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }

        if (leftTokens.equals(rightTokens)) {
            return 3;
        }

        if (isTokenSubset(leftTokens, rightTokens) || isTokenSubset(rightTokens, leftTokens)) {
            return 2;
        }

        if (isGenericInstitutionPaymentRow(sourceAccount, row, right) && matchesInstitutionName(sourceAccount, left)) {
            return 2;
        }

        return 0;
    }

    private boolean isGenericInstitutionPaymentRow(
        FinanceAccount sourceAccount,
        FinanceTransactionImportRow row,
        String normalizedRowPayee
    ) {
        if (
            sourceAccount == null || sourceAccount.getInstitution() == null || trimToNull(sourceAccount.getInstitution().getName()) == null
        ) {
            return false;
        }
        if (normalizedRowPayee == null) {
            return false;
        }
        if (row == null || row.getAmount() == null || row.getAmount().signum() <= 0) {
            return false;
        }
        if (row.getResolvedCategoryName() == null && row.getResolvedCategoryId() == null) {
            return false;
        }
        return Set.of("payment", "interest", "interest payment", "credit interest", "deposit").contains(normalizedRowPayee);
    }

    private boolean matchesInstitutionName(FinanceAccount sourceAccount, String normalizedCandidatePayee) {
        if (sourceAccount == null || sourceAccount.getInstitution() == null) {
            return false;
        }
        String normalizedInstitution = normalizePayeeText(sourceAccount.getInstitution().getName());
        if (normalizedInstitution == null || normalizedCandidatePayee == null) {
            return false;
        }
        return (
            normalizedCandidatePayee.equals(normalizedInstitution) ||
            normalizedCandidatePayee.contains(normalizedInstitution) ||
            normalizedInstitution.contains(normalizedCandidatePayee)
        );
    }

    private boolean isTokenSubset(List<String> leftTokens, List<String> rightTokens) {
        return !leftTokens.isEmpty() && rightTokens.containsAll(leftTokens);
    }

    private List<String> significantPayeeTokens(String normalizedPayee) {
        if (normalizedPayee == null) {
            return List.of();
        }

        return java.util.Arrays.stream(normalizedPayee.split(" "))
            .map(String::trim)
            .filter(token -> !token.isBlank())
            .filter(token -> token.length() > 2 || "ca".equals(token) || "bp".equals(token))
            .filter(token -> !Set.of("the", "for", "and", "pty", "ltd", "au").contains(token))
            .distinct()
            .toList();
    }

    private void logDuplicateMissDiagnostics(
        FinanceAccount sourceAccount,
        FinanceTransactionImportRow row,
        String normalizedRowPayee,
        List<FinanceTransaction> exactDateAmountMatches,
        List<FinanceTransaction> nearbyDateMatches
    ) {
        if (row.getDate() == null || row.getAmount() == null) {
            return;
        }

        if (!exactDateAmountMatches.isEmpty()) {
            log.warn(
                "duplicateDiagnostics:noSelection rowId={} rowIndex={} date={} amount={} payee='{}' normalizedPayee='{}' exactAmountCandidates={}",
                row.getId(),
                row.getRowIndex(),
                row.getDate(),
                row.getAmount(),
                trimToNull(row.getPayeeText()),
                normalizedRowPayee,
                exactDateAmountMatches
                    .stream()
                    .map(transaction ->
                        formatDuplicateCandidateDiagnostic(
                            sourceAccount,
                            row,
                            transaction,
                            normalizedRowPayee,
                            row.getResolvedCategoryId(),
                            row.getDate()
                        )
                    )
                    .toList()
            );
            return;
        }

        List<FinanceTransaction> sameCategoryNearbyCandidates = nearbyDateMatches
            .stream()
            .filter(transaction -> {
                String candidateCategoryId = transaction.getCategory() == null ? null : transaction.getCategory().getId().toString();
                return row.getResolvedCategoryId() != null && Objects.equals(candidateCategoryId, row.getResolvedCategoryId());
            })
            .toList();
        if (!sameCategoryNearbyCandidates.isEmpty()) {
            log.info(
                "duplicateDiagnostics:categoryAlignedNearMiss rowId={} rowIndex={} date={} amount={} payee='{}' normalizedPayee='{}' resolvedCategoryId={} nearbyCategoryCandidates={}",
                row.getId(),
                row.getRowIndex(),
                row.getDate(),
                row.getAmount(),
                trimToNull(row.getPayeeText()),
                normalizedRowPayee,
                row.getResolvedCategoryId(),
                sameCategoryNearbyCandidates
                    .stream()
                    .map(transaction ->
                        formatDuplicateCandidateDiagnostic(
                            sourceAccount,
                            row,
                            transaction,
                            normalizedRowPayee,
                            row.getResolvedCategoryId(),
                            row.getDate()
                        )
                    )
                    .toList()
            );
        }

        List<FinanceTransaction> oppositeSignCandidates = nearbyDateMatches
            .stream()
            .filter(
                transaction ->
                    transaction.getAmount() != null &&
                    row.getAmount() != null &&
                    sameAmount(transaction.getAmount().abs(), row.getAmount().abs()) &&
                    !sameAmount(transaction.getAmount(), row.getAmount())
            )
            .toList();
        if (!oppositeSignCandidates.isEmpty()) {
            log.info(
                "duplicateDiagnostics:signMismatch rowId={} rowIndex={} date={} amount={} payee='{}' normalizedPayee='{}' oppositeSignCandidates={}",
                row.getId(),
                row.getRowIndex(),
                row.getDate(),
                row.getAmount(),
                trimToNull(row.getPayeeText()),
                normalizedRowPayee,
                oppositeSignCandidates
                    .stream()
                    .map(transaction ->
                        formatDuplicateCandidateDiagnostic(
                            sourceAccount,
                            row,
                            transaction,
                            normalizedRowPayee,
                            row.getResolvedCategoryId(),
                            row.getDate()
                        )
                    )
                    .toList()
            );
        }

        if (!nearbyDateMatches.isEmpty()) {
            log.info(
                "duplicateDiagnostics:nearbyDateCandidates rowId={} rowIndex={} date={} amount={} payee='{}' normalizedPayee='{}' nearbyCandidates={}",
                row.getId(),
                row.getRowIndex(),
                row.getDate(),
                row.getAmount(),
                trimToNull(row.getPayeeText()),
                normalizedRowPayee,
                nearbyDateMatches
                    .stream()
                    .limit(5)
                    .map(transaction ->
                        formatDuplicateCandidateDiagnostic(
                            sourceAccount,
                            row,
                            transaction,
                            normalizedRowPayee,
                            row.getResolvedCategoryId(),
                            row.getDate()
                        )
                    )
                    .toList()
            );
        }
    }

    private String formatDuplicateCandidateDiagnostic(
        FinanceAccount sourceAccount,
        FinanceTransactionImportRow row,
        FinanceTransaction transaction,
        String normalizedRowPayee,
        String rowCategoryId,
        LocalDate rowDate
    ) {
        String normalizedCandidatePayee = normalizePayeeText(transaction.getPayeeName());
        int payeeScore = duplicatePayeeMatchScore(sourceAccount, row, normalizedCandidatePayee, normalizedRowPayee);
        long dayDelta =
            transaction.getDate() == null || rowDate == null
                ? Long.MIN_VALUE
                : java.time.temporal.ChronoUnit.DAYS.between(rowDate, transaction.getDate());
        String candidateCategoryId = transaction.getCategory() == null ? null : transaction.getCategory().getId().toString();
        boolean categoryMatches = Objects.equals(candidateCategoryId, rowCategoryId);
        return "id=%s,date=%s,amount=%s,payee='%s',normalizedPayee='%s',payeeScore=%s,dayDelta=%s,categoryMatches=%s,categoryId=%s".formatted(
            transaction.getId(),
            transaction.getDate(),
            transaction.getAmount(),
            trimToNull(transaction.getPayeeName()),
            normalizedCandidatePayee,
            payeeScore,
            dayDelta,
            categoryMatches,
            candidateCategoryId
        );
    }

    private String normalizePayeeText(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class ParsedImportRow {

        private LocalDate date;
        private String sourceDateText;
        private Boolean yearExplicit;
        private String transactionKind;
        private String transferAccountText;
        private BigDecimal amount;
        private String payeeText;
        private String memo;
        private BigDecimal runningBalance;
        private String categoryGuess;
        private Double confidence;
    }
}
