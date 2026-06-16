package ovaro.plat4m.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinanceInvestmentActivityType;
import ovaro.plat4m.domain.FinancePayee;
import ovaro.plat4m.domain.FinanceSecurityInvestmentSummary;
import ovaro.plat4m.domain.FinanceTag;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceTransactionTag;
import ovaro.plat4m.domain.FinanceTransferLink;
import ovaro.plat4m.domain.IFinanceMonthlySummary;
import ovaro.plat4m.domain.SourceLink;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceCategoryRepository;
import ovaro.plat4m.repository.FinancePayeeRepository;
import ovaro.plat4m.repository.FinanceTagRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.FinanceTransactionTagRepository;
import ovaro.plat4m.repository.FinanceTransferLinkRepository;
import ovaro.plat4m.repository.SourceLinkRepository;
import ovaro.plat4m.service.dto.FinanceInvestmentTransactionDTO;
import ovaro.plat4m.service.dto.FinanceManageCategoryDTO;
import ovaro.plat4m.service.dto.FinanceManageCategoryUpdateDTO;
import ovaro.plat4m.service.dto.FinanceManagePayeeDTO;
import ovaro.plat4m.service.dto.FinanceManagePayeeUpdateDTO;
import ovaro.plat4m.service.dto.FinanceResourceDTO;
import ovaro.plat4m.service.dto.FinanceTransactionEditorOptionsDTO;
import ovaro.plat4m.service.dto.FinanceTransactionRowDTO;
import ovaro.plat4m.service.dto.FinanceTransactionUpdateDTO;
import ovaro.plat4m.service.dto.FinanceTreeNodeDTO;

@Service
public class FinanceTransactionService {

    private static final Logger log = LoggerFactory.getLogger(FinanceTransactionService.class);
    private static final Integer EDITOR_CATEGORY_CLASSIFICATION_ID = 0;
    private FinanceTransactionRepository transactionRepository;
    private FinanceTransferLinkRepository transferLinkRepository;
    private FinanceCategoryRepository categoryRepository;
    private FinanceAccountRepository accountRepository;
    private FinancePayeeRepository payeeRepository;
    private FinanceTagRepository tagRepository;
    private FinanceTransactionTagRepository transactionTagRepository;
    private SourceLinkRepository sourceLinkRepository;
    private FinanceTransactionEditorLookupCacheService editorLookupCacheService;

    public FinanceTransactionService(
        FinanceTransactionRepository transactionRepository,
        FinanceTransferLinkRepository transferLinkRepository,
        FinanceCategoryRepository categoryRepository,
        FinanceAccountRepository accountRepository,
        FinancePayeeRepository payeeRepository,
        FinanceTagRepository tagRepository,
        FinanceTransactionTagRepository transactionTagRepository,
        SourceLinkRepository sourceLinkRepository,
        FinanceTransactionEditorLookupCacheService editorLookupCacheService
    ) {
        this.transactionRepository = transactionRepository;
        this.transferLinkRepository = transferLinkRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.payeeRepository = payeeRepository;
        this.tagRepository = tagRepository;
        this.transactionTagRepository = transactionTagRepository;
        this.sourceLinkRepository = sourceLinkRepository;
        this.editorLookupCacheService = editorLookupCacheService;
    }

    public Page<FinanceTransaction> getTransactions(String accountId, Pageable pageable) {
        Page<FinanceTransaction> transactions = this.transactionRepository.findAllByAccountId(accountId, pageable);

        return transactions;
    }

    public List<FinanceTransaction> getTransactionsNonPage(User user, String accountId) {
        List<FinanceTransaction> transactions = this.transactionRepository.findAllByAccountIdWithBalanceNonPage(
            user.getGuid().toString(),
            accountId
        );

        return transactions;
    }

    public Page<FinanceTransactionRowDTO> getTransactionsPaging(User user, String accountId, Pageable pageable, String filterModel) {
        return this.transactionRepository.findTransactionRows(user.getGuid().toString(), accountId, pageable, filterModel);
    }

    public FinanceTransactionRowDTO getTransactionRow(User user, String accountId, UUID transactionId) {
        return this.transactionRepository.findTransactionRowById(user.getGuid().toString(), accountId, transactionId);
    }

    public FinanceTransactionEditorOptionsDTO getEditorOptions(User user) {
        List<FinanceResourceDTO> categoryOptions = this.getCategoryOptions(user, null, Pageable.unpaged()).getContent();
        List<FinanceResourceDTO> payeeOptions = this.getPayeeOptions(user, null, Pageable.unpaged()).getContent();
        return new FinanceTransactionEditorOptionsDTO(categoryOptions, payeeOptions);
    }

    public Page<FinanceResourceDTO> getCategoryOptions(User user, String query, Pageable pageable) {
        List<FinanceResourceDTO> filteredCategories = this.editorLookupCacheService.getCategoryOptions(user.getGuid().toString());
        return filterResourceOptions(filteredCategories, query, pageable);
    }

    public List<FinanceTreeNodeDTO> getCategoryTreeOptions(User user) {
        return this.editorLookupCacheService.getCategoryTreeOptions(user.getGuid().toString());
    }

    @Transactional
    public FinanceResourceDTO createCategory(User user, String rawName, String type, String parentCategoryId) {
        String normalizedRawName = trimToNull(rawName);
        if (normalizedRawName == null) {
            throw new IllegalArgumentException("Category name is required");
        }

        String normalizedType = trimToNull(type);
        if (!"income".equalsIgnoreCase(normalizedType) && !"expense".equalsIgnoreCase(normalizedType)) {
            throw new IllegalArgumentException("Category type must be income or expense");
        }

        String rootName = "income".equalsIgnoreCase(normalizedType) ? "Income" : "Expense";
        FinanceCategory rootCategory = findCategory(user, rootName, null).orElseThrow(() ->
            new IllegalStateException(rootName + " root category was not found")
        );

        String normalizedParentCategoryId = trimToNull(parentCategoryId);
        if (normalizedParentCategoryId != null) {
            FinanceCategory parentCategory = this.categoryRepository
                .findById(UUID.fromString(normalizedParentCategoryId))
                .filter(category -> user.getGuid().toString().equals(category.getUserGuid()))
                .filter(category -> Objects.equals(EDITOR_CATEGORY_CLASSIFICATION_ID, category.getClassificationId()))
                .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
            while (parentCategory.getParent() != null && parentCategory.getParent().getParent() != null) {
                parentCategory = parentCategory.getParent();
            }

            FinanceCategory normalizedParentCategory = parentCategory;
            FinanceCategory childCategory = findCategory(user, normalizedRawName, normalizedParentCategory).orElseGet(() ->
                saveCategory(user, normalizedRawName, normalizedParentCategory)
            );
            this.editorLookupCacheService.invalidateCategoryOptions(user.getGuid().toString());
            this.editorLookupCacheService.invalidateCategoryTreeOptions(user.getGuid().toString());
            return new FinanceResourceDTO(childCategory.getId().toString(), buildCategoryDisplayName(childCategory));
        }

        String[] parts = normalizedRawName.split(":", 2);
        String parentName = trimToNull(parts[0]);
        String childName = parts.length > 1 ? trimToNull(parts[1]) : null;
        if (parentName == null) {
            throw new IllegalArgumentException("Category name is required");
        }

        FinanceCategory parentCategory = findCategory(user, parentName, rootCategory).orElseGet(() ->
            saveCategory(user, parentName, rootCategory)
        );

        if (childName == null) {
            this.editorLookupCacheService.invalidateCategoryOptions(user.getGuid().toString());
            this.editorLookupCacheService.invalidateCategoryTreeOptions(user.getGuid().toString());
            return new FinanceResourceDTO(parentCategory.getId().toString(), buildCategoryDisplayName(parentCategory));
        }

        FinanceCategory childCategory = findCategory(user, childName, parentCategory).orElseGet(() ->
            saveCategory(user, childName, parentCategory)
        );
        this.editorLookupCacheService.invalidateCategoryOptions(user.getGuid().toString());
        this.editorLookupCacheService.invalidateCategoryTreeOptions(user.getGuid().toString());
        return new FinanceResourceDTO(childCategory.getId().toString(), buildCategoryDisplayName(childCategory));
    }

    public Page<FinanceResourceDTO> getWhoOptions(User user, String query, Pageable pageable) {
        List<FinanceResourceDTO> whoOptions = this.editorLookupCacheService.getWhoOptions(user.getGuid().toString());
        return filterResourceOptions(whoOptions, query, pageable);
    }

    public List<FinanceTreeNodeDTO> getWhoTreeOptions(User user) {
        return this.editorLookupCacheService.getWhoTreeOptions(user.getGuid().toString());
    }

    @Transactional(readOnly = true)
    public List<FinanceManageCategoryDTO> getManagedCategories(User user) {
        String userGuid = user.getGuid().toString();
        return this.categoryRepository
            .findAllByUserGuid(userGuid)
            .stream()
            .map(category -> mapManagedCategory(category, userGuid))
            .sorted((left, right) -> compareNullableStrings(left.getDisplayName(), right.getDisplayName()))
            .toList();
    }

    @Transactional
    public FinanceManageCategoryDTO createManagedCategory(User user, FinanceManageCategoryUpdateDTO update) {
        FinanceCategory category = new FinanceCategory();
        category.setUserGuid(user.getGuid().toString());
        applyManagedCategoryUpdate(user, category, update);
        FinanceCategory saved = this.categoryRepository.save(category);
        invalidateCategoryCaches(user);
        return mapManagedCategory(saved, user.getGuid().toString());
    }

    @Transactional
    public FinanceManageCategoryDTO updateManagedCategory(User user, UUID categoryId, FinanceManageCategoryUpdateDTO update) {
        FinanceCategory category = findUserCategory(user, categoryId);
        applyManagedCategoryUpdate(user, category, update);
        FinanceCategory saved = this.categoryRepository.save(category);
        invalidateCategoryCaches(user);
        return mapManagedCategory(saved, user.getGuid().toString());
    }

    @Transactional
    public void deleteManagedCategory(User user, UUID categoryId) {
        FinanceCategory category = findUserCategory(user, categoryId);
        if (this.categoryRepository.existsByParent_IdAndUserGuid(categoryId, user.getGuid().toString())) {
            throw new IllegalStateException("Categories with sub-categories cannot be deleted");
        }
        this.categoryRepository.delete(category);
        invalidateCategoryCaches(user);
    }

    @Transactional(readOnly = true)
    public List<FinanceTransactionRowDTO> getManagedCategoryTransactions(User user, UUID categoryId) {
        FinanceCategory category = findUserCategory(user, categoryId);
        String userGuid = user.getGuid().toString();
        List<UUID> categoryIds = collectCategoryBranchIds(userGuid, category);
        List<FinanceTransaction> transactions = Integer.valueOf(1).equals(category.getClassificationId())
            ? this.transactionRepository.findAllByUserGuidAndWhoIds(userGuid, categoryIds)
            : this.transactionRepository.findAllByUserGuidAndCategoryIds(userGuid, categoryIds);

        return withTransactionTags(transactions.stream().map(this::mapTransactionRow).toList());
    }

    @Transactional(readOnly = true)
    public List<FinanceManagePayeeDTO> getManagedPayees(User user, boolean includeHidden) {
        List<FinancePayee> payees = includeHidden
            ? this.payeeRepository.findByUserGuidOrderByNameAsc(user.getGuid().toString())
            : this.payeeRepository.findVisibleByUserGuid(user.getGuid().toString());
        return payees.stream().map(this::mapManagedPayee).toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceTransactionRowDTO> getManagedPayeeTransactions(User user, UUID payeeId) {
        FinancePayee payee = findUserPayee(user, payeeId);
        String userGuid = user.getGuid().toString();
        List<String> payeeIds = collectPayeeBranchIds(userGuid, payee);
        List<FinanceTransaction> transactions = this.transactionRepository.findAllByUserGuidAndPayeeIds(userGuid, payeeIds);
        return withTransactionTags(transactions.stream().map(this::mapTransactionRow).toList());
    }

    @Transactional
    public FinanceManagePayeeDTO createManagedPayee(User user, FinanceManagePayeeUpdateDTO update) {
        FinancePayee payee = new FinancePayee();
        payee.setUserGuid(user.getGuid().toString());
        payee.setSerialDateTime(ZonedDateTime.now());
        applyManagedPayeeUpdate(user, payee, update);
        FinancePayee saved = this.payeeRepository.save(payee);
        this.editorLookupCacheService.invalidatePayeeOptions(user.getGuid().toString());
        return mapManagedPayee(saved);
    }

    @Transactional
    public FinanceManagePayeeDTO updateManagedPayee(User user, UUID payeeId, FinanceManagePayeeUpdateDTO update) {
        FinancePayee payee = findUserPayee(user, payeeId);
        applyManagedPayeeUpdate(user, payee, update);
        FinancePayee saved = this.payeeRepository.save(payee);
        this.editorLookupCacheService.invalidatePayeeOptions(user.getGuid().toString());
        return mapManagedPayee(saved);
    }

    @Transactional
    public void deleteManagedPayee(User user, UUID payeeId) {
        FinancePayee payee = findUserPayee(user, payeeId);
        payee.setHidden(true);
        this.payeeRepository.save(payee);
        this.editorLookupCacheService.invalidatePayeeOptions(user.getGuid().toString());
    }

    public Page<FinanceResourceDTO> getPayeeOptions(User user, String query, Pageable pageable) {
        List<FinanceResourceDTO> payeeOptions = this.editorLookupCacheService.getPayeeOptions(user.getGuid().toString());
        return filterResourceOptions(payeeOptions, query, pageable);
    }

    public Page<FinanceResourceDTO> getTagOptions(User user, String query, Pageable pageable) {
        Page<FinanceTag> tagPage;
        String normalizedQuery = trimToNull(query);
        if (normalizedQuery == null) {
            tagPage = this.tagRepository.findAllByUserGuidOrderByNameAsc(user.getGuid().toString(), pageable);
        } else {
            tagPage = this.tagRepository.findAllByUserGuidAndNameContainingIgnoreCaseOrderByNameAsc(
                user.getGuid().toString(),
                normalizedQuery,
                pageable
            );
        }

        return tagPage.map(tag -> new FinanceResourceDTO(tag.getId().toString(), tag.getName()));
    }

    private Page<FinanceResourceDTO> filterResourceOptions(List<FinanceResourceDTO> options, String query, Pageable pageable) {
        String normalizedQuery = trimToNull(query);
        if (normalizedQuery != null) {
            String loweredQuery = normalizedQuery.toLowerCase();
            options = options
                .stream()
                .filter(option -> option.getName() != null && option.getName().toLowerCase().contains(loweredQuery))
                .collect(Collectors.toList());
        }

        return pageResources(options, pageable);
    }

    @Transactional
    public FinanceTransactionRowDTO updateTransaction(User user, String accountId, UUID transactionId, FinanceTransactionUpdateDTO update) {
        FinanceTransaction transaction = this.transactionRepository
            .findByIdAndUserGuid(transactionId, user.getGuid().toString())
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!accountId.equals(transaction.getAccountId())) {
            throw new IllegalArgumentException("Transaction does not belong to the selected account");
        }
        if (transaction.isVoided()) {
            throw new IllegalStateException("Voided transactions cannot be edited");
        }
        if (transaction.isSplitChild()) {
            throw new IllegalStateException("Split child transactions cannot be edited directly");
        }
        if (transaction.isInvestment()) {
            throw new IllegalStateException("Investment transactions cannot be edited");
        }
        if (update.getDate() == null || update.getAmount() == null) {
            throw new IllegalArgumentException("Date and amount are required");
        }
        if (Boolean.TRUE.equals(update.getReplaceWithTransfer())) {
            return replaceTransactionWithTransfer(user, accountId, transactionId, transaction, update);
        }

        applyTransactionUpdate(user, accountId, transaction, update);
        FinanceTransaction saved = this.transactionRepository.save(transaction);
        syncSplitTransactions(user, accountId, saved, update);
        syncTransferTransaction(user, saved);
        syncTransactionTags(user, saved, update.getTags());
        return getTransactionRow(user, accountId, saved.getId());
    }

    private FinanceTransactionRowDTO replaceTransactionWithTransfer(
        User user,
        String accountId,
        UUID transactionId,
        FinanceTransaction existingTransaction,
        FinanceTransactionUpdateDTO update
    ) {
        if (trimToNull(update.getTransferredAccountId()) == null) {
            throw new IllegalArgumentException("Transfer account is required when converting a transaction to a transfer");
        }
        if (existingTransaction.isTransfer()) {
            throw new IllegalStateException("Transfer transactions cannot be replaced with another transfer");
        }

        deleteTransaction(user, accountId, transactionId);
        return createTransaction(user, accountId, update);
    }

    @Transactional
    public FinanceTransactionRowDTO createTransaction(User user, String accountId, FinanceTransactionUpdateDTO update) {
        if (update.getDate() == null || update.getAmount() == null) {
            throw new IllegalArgumentException("Date and amount are required");
        }

        FinanceAccount account = this.accountRepository
            .findById(UUID.fromString(accountId))
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!user.getGuid().toString().equals(account.getUserGuid())) {
            throw new IllegalArgumentException("Account not found");
        }

        FinanceTransaction transaction = new FinanceTransaction();
        transaction.setUserGuid(user.getGuid().toString());
        transaction.setAccountId(accountId);
        transaction.setDate(update.getDate());
        transaction.setAmount(update.getAmount());
        transaction.setRecurring(false);
        transaction.setSplitParent(false);
        transaction.setSplitChild(false);
        transaction.setVoided(false);
        transaction.setInvestment(false);
        transaction.setCleared(Boolean.TRUE.equals(update.getCleared()));
        transaction.setReconciled(false);
        transaction.setCurrencyCode(account.getCurrencyCode());
        transaction.setSerialDateTime(ZonedDateTime.now());

        applyTransactionUpdate(user, accountId, transaction, update);
        FinanceTransaction saved = this.transactionRepository.save(transaction);
        syncSplitTransactions(user, accountId, saved, update);
        syncTransferTransaction(user, saved);
        syncTransactionTags(user, saved, update.getTags());
        return getTransactionRow(user, accountId, saved.getId());
    }

    @Transactional
    public void deleteTransaction(User user, String accountId, UUID transactionId) {
        FinanceTransaction transaction = this.transactionRepository
            .findByIdAndUserGuid(transactionId, user.getGuid().toString())
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!accountId.equals(transaction.getAccountId())) {
            throw new IllegalArgumentException("Transaction does not belong to the selected account");
        }
        if (transaction.isSplitChild()) {
            throw new IllegalStateException("Split child transactions cannot be edited directly");
        }

        List<UUID> transactionIdsToDelete = new ArrayList<>();
        transactionIdsToDelete.add(transaction.getId());

        if (transaction.isSplitParent()) {
            transactionIdsToDelete.addAll(
                this.transactionRepository
                    .findAllByUserGuidAndParentIdOrderByNumberAscDateAscIdAsc(user.getGuid().toString(), transaction.getId())
                    .stream()
                    .map(FinanceTransaction::getId)
                    .toList()
            );
        }

        findTransferLink(user, transaction.getId()).ifPresent(transferLink -> {
            UUID counterpartId = transferLink.getFromId().equals(transaction.getId()) ? transferLink.getLinkId() : transferLink.getFromId();
            if (!transactionIdsToDelete.contains(counterpartId)) {
                transactionIdsToDelete.add(counterpartId);
            }
            deleteSourceLinksForLocalIds(user, List.of(transferLink.getId()));
            this.transferLinkRepository.delete(transferLink);
        });

        deleteSourceLinksForLocalIds(user, transactionIdsToDelete);
        transactionIdsToDelete.forEach(this.transactionTagRepository::deleteAllByTransaction_Id);
        this.transactionRepository.deleteAllById(transactionIdsToDelete);
    }

    @Transactional(readOnly = true)
    public List<FinanceTransactionUpdateDTO.SplitLineDTO> getSplitTransactions(User user, String accountId, UUID transactionId) {
        FinanceTransaction transaction = this.transactionRepository
            .findByIdAndUserGuid(transactionId, user.getGuid().toString())
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!accountId.equals(transaction.getAccountId())) {
            throw new IllegalArgumentException("Transaction does not belong to the selected account");
        }

        return this.transactionRepository
            .findAllByUserGuidAndParentIdOrderByNumberAscDateAscIdAsc(user.getGuid().toString(), transactionId)
            .stream()
            .map(this::mapSplitTransaction)
            .toList();
    }

    private void applyTransactionUpdate(User user, String accountId, FinanceTransaction transaction, FinanceTransactionUpdateDTO update) {
        transaction.setDate(update.getDate());
        transaction.setAmount(update.getAmount());
        transaction.setCleared(Boolean.TRUE.equals(update.getCleared()));
        transaction.setMemo(trimToNull(update.getMemo()));

        String transferredAccountId = trimToNull(update.getTransferredAccountId());
        if (transferredAccountId != null) {
            if (accountId.equals(transferredAccountId)) {
                throw new IllegalArgumentException("Transfer account must be different from the current account");
            }

            FinanceAccount transferAccount = this.accountRepository
                .findById(UUID.fromString(transferredAccountId))
                .orElseThrow(() -> new IllegalArgumentException("Transfer account not found"));

            if (!user.getGuid().toString().equals(transferAccount.getUserGuid())) {
                throw new IllegalArgumentException("Transfer account not found");
            }

            transaction.setTransferredAccountId(transferredAccountId);
            transaction.setTransfer(true);
            transaction.setTransferTo(update.getAmount().signum() > 0);
            transaction.setCategory(null);
            transaction.setWho(null);
            transaction.setPayeeId(null);
            transaction.setPayeeName(null);
            transaction.setSplitParent(false);
            transaction.setSplitChild(false);
            transaction.setParentId(null);
            return;
        }

        transaction.setTransferredAccountId(null);
        transaction.setTransfer(false);
        transaction.setTransferTo(false);
        transaction.setSplitParent(update.getSplits() != null && !update.getSplits().isEmpty());
        transaction.setSplitChild(false);

        String categoryId = trimToNull(update.getCategoryId());
        if (categoryId == null) {
            transaction.setCategory(null);
        } else {
            FinanceCategory category = this.categoryRepository
                .findById(UUID.fromString(categoryId))
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            transaction.setCategory(category);
        }

        String whoId = trimToNull(update.getWhoId());
        if (whoId == null) {
            transaction.setWho(null);
        } else {
            FinanceCategory who = this.categoryRepository
                .findById(UUID.fromString(whoId))
                .orElseThrow(() -> new IllegalArgumentException("Who not found"));
            if (!user.getGuid().toString().equals(who.getUserGuid()) || !Integer.valueOf(1).equals(who.getClassificationId())) {
                throw new IllegalArgumentException("Who not found");
            }
            transaction.setWho(who);
        }

        String payeeId = trimToNull(update.getPayeeId());
        String payeeName = trimToNull(update.getPayeeName());
        if (payeeId != null) {
            FinancePayee payee = this.payeeRepository
                .findById(UUID.fromString(payeeId))
                .orElseThrow(() -> new IllegalArgumentException("Payee not found"));
            if (!user.getGuid().toString().equals(payee.getUserGuid())) {
                throw new IllegalArgumentException("Payee not found");
            }
            transaction.setPayeeId(payee.getId().toString());
            transaction.setPayeeName(payeeName != null ? payeeName : payee.getName());
        } else {
            FinancePayee payee = resolveOrCreatePayee(user, payeeName);
            transaction.setPayeeId(payee != null ? payee.getId().toString() : null);
            transaction.setPayeeName(payee != null ? payee.getName() : null);
        }
    }

    private void syncSplitTransactions(User user, String accountId, FinanceTransaction parent, FinanceTransactionUpdateDTO update) {
        this.transactionRepository.deleteAllByUserGuidAndParentId(user.getGuid().toString(), parent.getId());
        this.transactionRepository.flush();

        List<FinanceTransactionUpdateDTO.SplitLineDTO> requestedSplits = update.getSplits();
        if (requestedSplits == null || requestedSplits.isEmpty()) {
            parent.setSplitParent(false);
            this.transactionRepository.save(parent);
            return;
        }

        BigDecimal splitTotal = requestedSplits
            .stream()
            .map(FinanceTransactionUpdateDTO.SplitLineDTO::getAmount)
            .filter(Objects::nonNull)
            .map(BigDecimal::abs)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal signedTotal = parent.getAmount() != null && parent.getAmount().signum() >= 0 ? splitTotal : splitTotal.negate();
        parent.setAmount(signedTotal);
        parent.setSplitParent(true);
        parent.setCategory(null);
        parent.setWho(null);
        this.transactionRepository.save(parent);

        List<FinanceTransaction> splitTransactions = new ArrayList<>();
        int number = 0;
        for (FinanceTransactionUpdateDTO.SplitLineDTO split : requestedSplits) {
            BigDecimal splitAmount = split.getAmount();
            if (
                splitAmount == null &&
                trimToNull(split.getCategoryId()) == null &&
                trimToNull(split.getWhoId()) == null &&
                trimToNull(split.getMemo()) == null
            ) {
                continue;
            }

            FinanceTransaction child = new FinanceTransaction();
            child.setUserGuid(user.getGuid().toString());
            child.setAccountId(accountId);
            child.setParentId(parent.getId());
            child.setDate(parent.getDate());
            BigDecimal normalizedSplitAmount = splitAmount == null ? BigDecimal.ZERO : splitAmount.abs();
            child.setAmount(signedTotal.signum() >= 0 ? normalizedSplitAmount : normalizedSplitAmount.negate());
            child.setNumber(number++);
            child.setRecurring(false);
            child.setSplitParent(false);
            child.setSplitChild(true);
            child.setVoided(false);
            child.setInvestment(false);
            child.setCleared(parent.isCleared());
            child.setReconciled(parent.isReconciled());
            child.setCurrencyCode(parent.getCurrencyCode());
            child.setPayeeId(parent.getPayeeId());
            child.setPayeeName(parent.getPayeeName());
            child.setMemo(trimToNull(split.getMemo()));
            child.setCategory(resolveCategory(split.getCategoryId()));
            child.setWho(resolveWho(user, split.getWhoId()));
            child.setSerialDateTime(ZonedDateTime.now());
            splitTransactions.add(child);
        }

        this.transactionRepository.saveAll(splitTransactions);
    }

    private void syncTransferTransaction(User user, FinanceTransaction transaction) {
        Optional<FinanceTransferLink> existingLink = findTransferLink(user, transaction.getId());
        if (!transaction.isTransfer()) {
            deleteTransferCounterpart(transaction.getId(), existingLink);
            return;
        }

        BigDecimal normalizedAmount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount().abs();
        boolean incomingSide = transaction.isTransferTo();

        FinanceAccount sourceAccount = incomingSide
            ? findUserAccount(user, transaction.getTransferredAccountId())
            : findUserAccount(user, transaction.getAccountId());
        FinanceAccount destinationAccount = incomingSide
            ? findUserAccount(user, transaction.getAccountId())
            : findUserAccount(user, transaction.getTransferredAccountId());

        FinanceTransaction counterpart = resolveTransferCounterpart(user, transaction, existingLink.orElse(null));
        boolean newCounterpart = counterpart.getId() == null;
        if (newCounterpart) {
            initializeTransferCounterpart(counterpart, user);
        }

        FinanceTransaction fromTransaction = incomingSide ? counterpart : transaction;
        FinanceTransaction toTransaction = incomingSide ? transaction : counterpart;

        configureTransferTransaction(fromTransaction, sourceAccount, destinationAccount, normalizedAmount.negate(), false, transaction);
        configureTransferTransaction(toTransaction, destinationAccount, sourceAccount, normalizedAmount, true, transaction);

        FinanceTransaction savedFromTransaction = this.transactionRepository.save(fromTransaction);
        FinanceTransaction savedToTransaction = this.transactionRepository.save(toTransaction);
        upsertTransferLink(user, existingLink.orElse(null), savedFromTransaction.getId(), savedToTransaction.getId());
    }

    private Optional<FinanceTransferLink> findTransferLink(User user, UUID transactionId) {
        return this.transferLinkRepository.findByUserGuidAndTransactionId(user.getGuid().toString(), transactionId);
    }

    private FinanceTransaction resolveTransferCounterpart(User user, FinanceTransaction transaction, FinanceTransferLink existingLink) {
        if (existingLink == null) {
            return new FinanceTransaction();
        }

        UUID counterpartId = existingLink.getFromId().equals(transaction.getId()) ? existingLink.getLinkId() : existingLink.getFromId();
        return this.transactionRepository.findByIdAndUserGuid(counterpartId, user.getGuid().toString()).orElseGet(FinanceTransaction::new);
    }

    private void initializeTransferCounterpart(FinanceTransaction counterpart, User user) {
        counterpart.setUserGuid(user.getGuid().toString());
        counterpart.setRecurring(false);
        counterpart.setSplitParent(false);
        counterpart.setSplitChild(false);
        counterpart.setVoided(false);
        counterpart.setInvestment(false);
        counterpart.setReconciled(false);
        counterpart.setSerialDateTime(ZonedDateTime.now());
    }

    private void configureTransferTransaction(
        FinanceTransaction transaction,
        FinanceAccount account,
        FinanceAccount transferredAccount,
        BigDecimal amount,
        boolean transferTo,
        FinanceTransaction editedTransaction
    ) {
        transaction.setAccountId(account.getId().toString());
        transaction.setTransferredAccountId(transferredAccount.getId().toString());
        transaction.setDate(editedTransaction.getDate());
        transaction.setAmount(amount);
        transaction.setRecurring(false);
        transaction.setTransfer(true);
        transaction.setTransferTo(transferTo);
        transaction.setSplitParent(false);
        transaction.setSplitChild(false);
        transaction.setParentId(null);
        transaction.setVoided(false);
        transaction.setInvestment(false);
        transaction.setCleared(editedTransaction.isCleared());
        transaction.setMemo(editedTransaction.getMemo());
        transaction.setCategory(null);
        transaction.setWho(null);
        transaction.setPayeeId(null);
        transaction.setPayeeName(null);
        transaction.setStatementId(null);
        transaction.setCurrencyCode(account.getCurrencyCode());
    }

    private void upsertTransferLink(User user, FinanceTransferLink existingLink, UUID fromId, UUID linkId) {
        FinanceTransferLink transferLink = existingLink == null ? new FinanceTransferLink() : existingLink;
        transferLink.setUserGuid(user.getGuid().toString());
        transferLink.setFromId(fromId);
        transferLink.setLinkId(linkId);
        this.transferLinkRepository.save(transferLink);
    }

    private void deleteTransferCounterpart(UUID transactionId, Optional<FinanceTransferLink> existingLink) {
        if (existingLink.isEmpty()) {
            return;
        }

        FinanceTransferLink transferLink = existingLink.get();
        UUID counterpartId = transferLink.getFromId().equals(transactionId) ? transferLink.getLinkId() : transferLink.getFromId();
        deleteSourceLinksForLocalIds(transferLink.getUserGuid(), List.of(counterpartId, transferLink.getId()));
        this.transferLinkRepository.delete(transferLink);
        this.transactionTagRepository.deleteAllByTransaction_Id(counterpartId);
        this.transactionRepository.deleteById(counterpartId);
    }

    private void deleteSourceLinksForLocalIds(User user, List<UUID> localIds) {
        deleteSourceLinksForLocalIds(user.getGuid().toString(), localIds);
    }

    private void deleteSourceLinksForLocalIds(String userGuid, List<UUID> localIds) {
        List<String> normalizedIds = localIds.stream().filter(Objects::nonNull).map(UUID::toString).distinct().toList();
        if (normalizedIds.isEmpty()) {
            return;
        }

        List<SourceLink> sourceLinks = this.sourceLinkRepository.findByUserGuidAndLocalIdIn(userGuid, normalizedIds);
        if (!sourceLinks.isEmpty()) {
            this.sourceLinkRepository.deleteAll(sourceLinks);
        }
    }

    private FinanceAccount findUserAccount(User user, String accountId) {
        return this.accountRepository
            .findById(UUID.fromString(accountId))
            .filter(account -> user.getGuid().toString().equals(account.getUserGuid()))
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private FinanceTransactionUpdateDTO.SplitLineDTO mapSplitTransaction(FinanceTransaction transaction) {
        FinanceTransactionUpdateDTO.SplitLineDTO dto = new FinanceTransactionUpdateDTO.SplitLineDTO();
        if (transaction.getCategory() != null) {
            dto.setCategoryId(transaction.getCategory().getId().toString());
            dto.setCategoryName(buildCategoryDisplayName(transaction.getCategory()));
        }
        if (transaction.getWho() != null) {
            dto.setWhoId(transaction.getWho().getId().toString());
            dto.setWhoName(transaction.getWho().getName());
        }
        dto.setMemo(transaction.getMemo());
        dto.setAmount(transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount().abs());
        return dto;
    }

    private String buildCategoryDisplayName(FinanceCategory category) {
        if (category.getParent() == null || category.getParent().getName() == null) {
            return category.getName();
        }

        return category.getParent().getName() + ": " + category.getName();
    }

    private Optional<FinanceCategory> findCategory(User user, String name, FinanceCategory parent) {
        String normalizedName = trimToNull(name);
        if (normalizedName == null) {
            return Optional.empty();
        }

        return this.categoryRepository
            .findAllByClassificationIdAndUserGuid(EDITOR_CATEGORY_CLASSIFICATION_ID, user.getGuid().toString())
            .stream()
            .filter(category -> normalizedName.equalsIgnoreCase(category.getName()))
            .filter(category -> {
                if (parent == null) {
                    return category.getParent() == null;
                }
                return category.getParent() != null && parent.getId().equals(category.getParent().getId());
            })
            .findFirst();
    }

    private FinanceCategory saveCategory(User user, String name, FinanceCategory parent) {
        FinanceCategory category = new FinanceCategory();
        category.setUserGuid(user.getGuid().toString());
        category.setName(trimToNull(name));
        category.setParent(parent);
        category.setClassificationId(EDITOR_CATEGORY_CLASSIFICATION_ID);
        category.setLevel(parent == null || parent.getLevel() == null ? 0 : parent.getLevel() + 1);
        return this.categoryRepository.save(category);
    }

    private FinanceCategory resolveCategory(String categoryId) {
        String normalizedCategoryId = trimToNull(categoryId);
        if (normalizedCategoryId == null) {
            return null;
        }

        return this.categoryRepository
            .findById(UUID.fromString(normalizedCategoryId))
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    private FinanceCategory resolveWho(User user, String whoId) {
        String normalizedWhoId = trimToNull(whoId);
        if (normalizedWhoId == null) {
            return null;
        }

        FinanceCategory who = this.categoryRepository
            .findById(UUID.fromString(normalizedWhoId))
            .orElseThrow(() -> new IllegalArgumentException("Who not found"));
        if (!user.getGuid().toString().equals(who.getUserGuid()) || !Integer.valueOf(1).equals(who.getClassificationId())) {
            throw new IllegalArgumentException("Who not found");
        }
        return who;
    }

    private FinancePayee resolveOrCreatePayee(User user, String payeeName) {
        String normalizedPayeeName = trimToNull(payeeName);
        if (normalizedPayeeName == null) {
            return null;
        }

        return this.payeeRepository.findByUserGuidAndNameIgnoreCase(user.getGuid().toString(), normalizedPayeeName).orElseGet(() -> {
            FinancePayee payee = new FinancePayee();
            payee.setUserGuid(user.getGuid().toString());
            payee.setName(normalizedPayeeName);
            payee.setHidden(false);
            payee.setSerialDateTime(ZonedDateTime.now());
            return this.payeeRepository.save(payee);
        });
    }

    private FinanceManageCategoryDTO mapManagedCategory(FinanceCategory category, String userGuid) {
        FinanceCategory parent = category.getParent();
        return new FinanceManageCategoryDTO(
            category.getId().toString(),
            category.getName(),
            buildCategoryDisplayName(category),
            parent == null ? null : parent.getId().toString(),
            parent == null ? null : parent.getName(),
            category.getClassificationId(),
            category.getLevel(),
            category.getComment(),
            this.categoryRepository.existsByParent_IdAndUserGuid(category.getId(), userGuid)
        );
    }

    private FinanceManagePayeeDTO mapManagedPayee(FinancePayee payee) {
        return new FinanceManagePayeeDTO(payee.getId().toString(), payee.getName(), payee.getParentId(), payee.getHidden());
    }

    private FinanceTransactionRowDTO mapTransactionRow(FinanceTransaction transaction) {
        FinanceTransactionRowDTO dto = new FinanceTransactionRowDTO();
        dto.setAccountId(transaction.getAccountId());
        dto.setAmount(transaction.getAmount());
        dto.setPrincipalAmount(transaction.getPrincipalAmount());
        if (transaction.getCategory() != null) {
            FinanceCategory category = transaction.getCategory();
            dto.setCategoryId(category.getId().toString());
            dto.setCategoryName(category.getName());
            if (category.getParent() != null) {
                dto.setParentCategoryId(category.getParent().getId().toString());
                dto.setParentCategoryName(category.getParent().getName());
            }
        }
        dto.setCleared(transaction.isCleared());
        dto.setDate(transaction.getDate());
        dto.setId(transaction.getId() == null ? null : transaction.getId().toString());
        dto.setInvestment(transaction.isInvestment());
        dto.setInvestmentActivityType(
            transaction.getInvestmentActivityType() == null ? null : transaction.getInvestmentActivityType().name()
        );
        dto.setInvestmentActivityTypeId(
            transaction.getInvestmentActivityType() == null ? null : transaction.getInvestmentActivityType().value
        );
        dto.setMasterGuid(transaction.getMasterGuid());
        dto.setMemo(transaction.getMemo());
        dto.setNumber(transaction.getNumber());
        dto.setPayeeId(transaction.getPayeeId());
        dto.setPayeeName(transaction.getPayeeName());
        dto.setPrice(transaction.getPrice());
        dto.setQuantity(transaction.getQuantity());
        dto.setReconciled(transaction.isReconciled());
        dto.setRecurring(transaction.isRecurring());
        dto.setSecurityId(transaction.getSecurityId());
        dto.setSplitChild(transaction.isSplitChild());
        dto.setSplitParent(transaction.isSplitParent());
        dto.setStatementId(transaction.getStatementId());
        dto.setStatusFlag(transaction.getStatusFlag());
        dto.setTransfer(transaction.isTransfer());
        dto.setTransferTo(transaction.isTransferTo());
        dto.setTransferredAccountId(transaction.getTransferredAccountId());
        dto.setVoided(transaction.isVoided());
        if (transaction.getWho() != null) {
            dto.setWhoId(transaction.getWho().getId().toString());
            dto.setWhoName(transaction.getWho().getName());
        }
        return dto;
    }

    private List<FinanceTransactionRowDTO> withTransactionTags(List<FinanceTransactionRowDTO> rows) {
        List<UUID> transactionIds = rows
            .stream()
            .map(FinanceTransactionRowDTO::getId)
            .filter(Objects::nonNull)
            .map(UUID::fromString)
            .toList();
        if (transactionIds.isEmpty()) {
            return rows;
        }

        Map<String, List<String>> tagsByTransactionId = new HashMap<>();
        this.transactionTagRepository
            .findTagNamesByTransactionIds(transactionIds)
            .forEach(tag ->
                tagsByTransactionId.computeIfAbsent(tag.getTransactionId().toString(), ignored -> new ArrayList<>()).add(tag.getTagName())
            );

        rows.forEach(row -> {
            List<String> tags = tagsByTransactionId.getOrDefault(row.getId(), List.of());
            row.setTags(tags);
            row.setTagsDisplay(String.join(", ", tags));
        });
        return rows;
    }

    private List<UUID> collectCategoryBranchIds(String userGuid, FinanceCategory root) {
        List<FinanceCategory> categories = this.categoryRepository.findAllByUserGuid(userGuid);
        List<UUID> categoryIds = new ArrayList<>();
        appendCategoryBranchId(root, categories, categoryIds);
        return categoryIds;
    }

    private void appendCategoryBranchId(FinanceCategory category, List<FinanceCategory> categories, List<UUID> categoryIds) {
        categoryIds.add(category.getId());
        categories
            .stream()
            .filter(candidate -> candidate.getParent() != null && category.getId().equals(candidate.getParent().getId()))
            .forEach(child -> appendCategoryBranchId(child, categories, categoryIds));
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

    private void applyManagedCategoryUpdate(User user, FinanceCategory category, FinanceManageCategoryUpdateDTO update) {
        String name = trimToNull(update.getName());
        if (name == null) {
            throw new IllegalArgumentException("Category name is required");
        }

        Integer classificationId = update.getClassificationId() == null ? EDITOR_CATEGORY_CLASSIFICATION_ID : update.getClassificationId();
        FinanceCategory parent = null;
        String parentId = trimToNull(update.getParentId());
        if (parentId != null) {
            parent = findUserCategory(user, UUID.fromString(parentId));
            if (category.getId() != null && category.getId().equals(parent.getId())) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            classificationId = parent.getClassificationId();
        }

        category.setName(name);
        category.setParent(parent);
        category.setClassificationId(classificationId);
        category.setLevel(parent == null || parent.getLevel() == null ? 0 : parent.getLevel() + 1);
        category.setComment(trimToNull(update.getComment()));
    }

    private void applyManagedPayeeUpdate(User user, FinancePayee payee, FinanceManagePayeeUpdateDTO update) {
        String name = trimToNull(update.getName());
        if (name == null) {
            throw new IllegalArgumentException("Payee name is required");
        }

        String parentId = trimToNull(update.getParentId());
        if (parentId != null) {
            FinancePayee parent = findUserPayee(user, UUID.fromString(parentId));
            if (payee.getId() != null && payee.getId().equals(parent.getId())) {
                throw new IllegalArgumentException("A payee cannot be its own parent");
            }
        }

        payee.setName(name);
        payee.setParentId(parentId);
        payee.setHidden(Boolean.TRUE.equals(update.getHidden()));
    }

    private FinanceCategory findUserCategory(User user, UUID categoryId) {
        return this.categoryRepository
            .findById(categoryId)
            .filter(category -> user.getGuid().toString().equals(category.getUserGuid()))
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    private FinancePayee findUserPayee(User user, UUID payeeId) {
        return this.payeeRepository
            .findById(payeeId)
            .filter(payee -> user.getGuid().toString().equals(payee.getUserGuid()))
            .orElseThrow(() -> new IllegalArgumentException("Payee not found"));
    }

    private void invalidateCategoryCaches(User user) {
        this.editorLookupCacheService.invalidateCategoryOptions(user.getGuid().toString());
        this.editorLookupCacheService.invalidateCategoryTreeOptions(user.getGuid().toString());
    }

    private int compareNullableStrings(String left, String right) {
        String normalizedLeft = left == null ? "" : left.toLowerCase();
        String normalizedRight = right == null ? "" : right.toLowerCase();
        return normalizedLeft.compareTo(normalizedRight);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Page<FinanceResourceDTO> pageResources(List<FinanceResourceDTO> resources, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(resources);
        }

        int start = (int) pageable.getOffset();
        if (start >= resources.size()) {
            return new PageImpl<>(List.of(), pageable, resources.size());
        }

        int end = Math.min(start + pageable.getPageSize(), resources.size());
        return new PageImpl<>(resources.subList(start, end), pageable, resources.size());
    }

    private void syncTransactionTags(User user, FinanceTransaction transaction, List<String> requestedTags) {
        this.transactionTagRepository.deleteAllByTransaction_Id(transaction.getId());
        this.transactionTagRepository.flush();

        List<String> normalizedTags =
            requestedTags == null
                ? List.of()
                : requestedTags
                      .stream()
                      .map(this::trimToNull)
                      .filter(Objects::nonNull)
                      .map(String::trim)
                      .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));

        if (normalizedTags.isEmpty()) {
            return;
        }

        Map<String, String> normalizedToDisplay = new LinkedHashMap<>();
        for (String tagName : normalizedTags) {
            normalizedToDisplay.put(normalizeTagName(tagName), tagName);
        }

        List<FinanceTag> existingTags = this.tagRepository.findAllByUserGuidAndNormalizedNameIn(
            user.getGuid().toString(),
            normalizedToDisplay.keySet()
        );
        Map<String, FinanceTag> tagsByNormalizedName = existingTags
            .stream()
            .collect(Collectors.toMap(FinanceTag::getNormalizedName, tag -> tag));

        for (Map.Entry<String, String> entry : normalizedToDisplay.entrySet()) {
            if (!tagsByNormalizedName.containsKey(entry.getKey())) {
                FinanceTag tag = new FinanceTag();
                tag.setUserGuid(user.getGuid().toString());
                tag.setName(entry.getValue());
                tag.setNormalizedName(entry.getKey());
                tag.setSerialDateTime(ZonedDateTime.now());
                tagsByNormalizedName.put(entry.getKey(), this.tagRepository.save(tag));
            }
        }

        List<FinanceTransactionTag> assignments = new ArrayList<>();
        int sortOrder = 0;
        for (String tagName : normalizedTags) {
            FinanceTag tag = tagsByNormalizedName.get(normalizeTagName(tagName));
            FinanceTransactionTag assignment = new FinanceTransactionTag();
            assignment.setTransaction(transaction);
            assignment.setTag(tag);
            assignment.setSortOrder(sortOrder++);
            assignments.add(assignment);
        }

        this.transactionTagRepository.saveAll(assignments);
    }

    private String normalizeTagName(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    public BigDecimal getAccountBalance(FinanceAccount account) {
        List<FinanceTransaction> transactions = null;

        transactions = this.transactionRepository.findAllByAccountIdOrderByDate(account.getId().toString());
        log.debug(
            "Calculating Balance for account: " +
                account.getName() +
                ", ID: " +
                account.getId().toString() +
                ". transactions# " +
                transactions.size()
        );
        return FinanceTransactionService.calculateNonInvestmentBalance(account, transactions, LocalDate.now());
    }

    public Map<String, BigDecimal> sumTransactionsForAllAccounts(LocalDate toDate) {
        //List<FinanceTransaction> transactions =  this.transactionRepository.findAllByAccountIdOrderByDate(account.getId().toString());
        // log.debug("");

        Map<String, BigDecimal> results = new HashMap<>();

        List<Object[]> resultsList = null;

        if (toDate != null) {
            resultsList = this.transactionRepository.findSumTransactionsUpToDate(toDate);
        } else {
            resultsList = this.transactionRepository.findSumTransactions();
        }
        //log.info("Results: " + resultsList);

        for (Object[] o : resultsList) {
            //log.info("Result Row: " + o + " - " + o[0] + ", " + o[1]);
            results.put((String) o[0], (BigDecimal) o[1]);
        }
        return results;
    }

    public BigDecimal getAccountBalanceForAccountUptoDate(FinanceAccount account, LocalDate date) {
        //List<FinanceTransaction> transactions =  this.transactionRepository.findAllByAccountIdOrderByDate(account.getId().toString());
        log.debug("");
        // if(date == null) {
        //     date = LocalDate.parse("2022-01-09");
        // }
        //this.transactionRepository.findAllByAccountIdOrderByDate(account.getId().toString(), date);
        BigDecimal sum = this.transactionRepository.findSumTransactionsForAccountUpToDate(account.getId().toString(), date);

        if (sum == null) {
            sum = new BigDecimal(0);
        }
        BigDecimal total = account.getStartingBalance();
        if (total != null) {
            total = total.add(sum);
        } else {
            total = sum;
        }
        log.info(
            "Found sum of transactions for account: " +
                account.getId().toString() +
                ", Sum: " +
                sum +
                ", starting: " +
                account.getStartingBalance() +
                ", Total: " +
                total
        );
        //account.setCurrentBalance(total);
        return total;
    }

    public static BigDecimal calculateNonInvestmentBalance(FinanceAccount account, List<FinanceTransaction> transactions, LocalDate date) {
        BigDecimal currentBalance = account.getStartingBalance();
        if (currentBalance == null) {
            log.warn("Starting balance is null. Set to 0. Account's id=" + account.getId());
            currentBalance = new BigDecimal(0.00);
        }
        //boolean l = false;
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/YYYY");
        // if(account.getName().equals("Suncorp Variable")){
        //     l = true;
        // }
        for (FinanceTransaction transaction : transactions) {
            if (transaction.isVoided()) {
                continue;
            }
            if (transaction.isSplitChild()) {
                continue;
            }
            if (transaction.isRecurring()) {
                continue;
            }
            if (date != null) {
                LocalDate transactionDate = transaction.getDate();
                if (transactionDate.compareTo(date) > 0) {
                    continue;
                }
            }

            BigDecimal amount = transaction.getAmount();
            if (amount != null) {
                currentBalance = currentBalance.add(amount);
                // if(l) {
                //     System.out.println(transaction);
                //     System.out.println("" + transaction.getDate().format(formatter) + ",\"" + transaction.getPayeeName() + "\","+ transaction.getAmount() + "," + currentBalance);
                // }
            } else {
                log.warn("Transaction with no amount, id=" + transaction.getId());
            }
        }
        //account.setCurrentBalance(currentBalance);
        log.info("Account: " + account.getName() + ", ID: " + account.getId().toString() + ", Balance:  " + currentBalance);
        return currentBalance;
    }

    public List<FinanceSecurityInvestmentSummary> getFinanceSecurityInvestmentTransactions(
        User user,
        boolean includeClosed,
        LocalDate upToDate
    ) {
        List<FinanceSecurityInvestmentSummary> transactions = null;
        if (upToDate == null) {
            transactions = this.transactionRepository.findSecurityInvestmentTransactionsForOpenAccounts(user.getGuid().toString());
        } else {
            transactions = this.transactionRepository.findSecurityInvestmentTransactionsForOpenAccountsUptoDate(
                user.getGuid().toString(),
                upToDate
            );
        }
        if (!includeClosed) {
            removeZeroBalances(transactions);
        }
        return transactions;
    }

    public List<FinanceSecurityInvestmentSummary> getFinanceSecurityInvestmentTransactionsForAccount(
        User user,
        String accountId,
        boolean includeClosed
    ) {
        List<FinanceSecurityInvestmentSummary> transactions = this.transactionRepository.findSecurityInvestmentTransactionsForAccountId(
            user.getGuid().toString(),
            accountId
        );
        if (!includeClosed) {
            removeZeroBalances(transactions);
        }
        return transactions;
    }

    private void removeZeroBalances(List<FinanceSecurityInvestmentSummary> transactions) {
        List<FinanceSecurityInvestmentSummary> toRemove = new ArrayList<FinanceSecurityInvestmentSummary>();
        for (FinanceSecurityInvestmentSummary txn : transactions) {
            if (txn.getAddSec() != null && txn.getRemoveSec() != null && txn.getAddSec() + txn.getRemoveSec() <= 0) {
                toRemove.add(txn);
            }
        }

        transactions.removeAll(toRemove);
    }

    // findBySecurityIdOrderByDate
    public List<FinanceInvestmentTransactionDTO> findBySecurityId(User user, String securityId) {
        List<FinanceTransaction> transactions = this.transactionRepository.findByUserGuidAndSecurityIdOrderByDateDesc(
            user.getGuid().toString(),
            securityId
        );
        List<FinanceInvestmentTransactionDTO> dtos = new ArrayList<FinanceInvestmentTransactionDTO>();
        for (FinanceTransaction txn : transactions) {
            dtos.add(new FinanceInvestmentTransactionDTO(txn));
        }
        return dtos;
    }

    public List<FinanceInvestmentTransactionDTO> findByUser(User user) {
        List<FinanceTransaction> transactions = this.transactionRepository.findByUserGuidAndInvestmentOrderByDateDesc(
            user.getGuid().toString(),
            true
        );
        List<FinanceInvestmentTransactionDTO> dtos = new ArrayList<FinanceInvestmentTransactionDTO>();
        for (FinanceTransaction txn : transactions) {
            dtos.add(new FinanceInvestmentTransactionDTO(txn));
        }
        return dtos;
    }

    public List<IFinanceMonthlySummary> getAllAccountsMonthlyRunningBalance(User user) {
        List<IFinanceMonthlySummary> s = this.transactionRepository.findAllAccountsMonthlyRunningBalance(
            user.getGuid().toString(),
            user.getLocalCurrency()
        );
        return s;
    }
}
