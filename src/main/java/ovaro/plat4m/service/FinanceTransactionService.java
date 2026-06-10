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
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinanceInvestmentActivityType;
import ovaro.plat4m.domain.FinancePayee;
import ovaro.plat4m.domain.FinanceSecurityInvestmentSummary;
import ovaro.plat4m.domain.FinanceTag;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceTransactionTag;
import ovaro.plat4m.domain.IFinanceMonthlySummary;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceCategoryRepository;
import ovaro.plat4m.repository.FinancePayeeRepository;
import ovaro.plat4m.repository.FinanceTagRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.FinanceTransactionTagRepository;
import ovaro.plat4m.service.dto.FinanceInvestmentTransactionDTO;
import ovaro.plat4m.service.dto.FinanceResourceDTO;
import ovaro.plat4m.service.dto.FinanceTransactionEditorOptionsDTO;
import ovaro.plat4m.service.dto.FinanceTransactionRowDTO;
import ovaro.plat4m.service.dto.FinanceTransactionUpdateDTO;

@Service
public class FinanceTransactionService {

    private static final Logger log = LoggerFactory.getLogger(FinanceTransactionService.class);
    private FinanceTransactionRepository transactionRepository;
    private FinanceCategoryRepository categoryRepository;
    private FinanceAccountRepository accountRepository;
    private FinancePayeeRepository payeeRepository;
    private FinanceTagRepository tagRepository;
    private FinanceTransactionTagRepository transactionTagRepository;
    private FinanceTransactionEditorLookupCacheService editorLookupCacheService;

    public FinanceTransactionService(
        FinanceTransactionRepository transactionRepository,
        FinanceCategoryRepository categoryRepository,
        FinanceAccountRepository accountRepository,
        FinancePayeeRepository payeeRepository,
        FinanceTagRepository tagRepository,
        FinanceTransactionTagRepository transactionTagRepository,
        FinanceTransactionEditorLookupCacheService editorLookupCacheService
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.payeeRepository = payeeRepository;
        this.tagRepository = tagRepository;
        this.transactionTagRepository = transactionTagRepository;
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

    public Page<FinanceResourceDTO> getWhoOptions(User user, String query, Pageable pageable) {
        List<FinanceResourceDTO> whoOptions = this.editorLookupCacheService.getWhoOptions(user.getGuid().toString());
        return filterResourceOptions(whoOptions, query, pageable);
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
        if (transaction.isSplitParent() || transaction.isSplitChild()) {
            throw new IllegalStateException("Split transactions cannot be edited");
        }
        if (transaction.isInvestment()) {
            throw new IllegalStateException("Investment transactions cannot be edited");
        }
        if (update.getDate() == null || update.getAmount() == null) {
            throw new IllegalArgumentException("Date and amount are required");
        }

        applyTransactionUpdate(user, accountId, transaction, update);
        FinanceTransaction saved = this.transactionRepository.save(transaction);
        syncTransactionTags(user, saved, update.getTags());
        return getTransactionRow(user, accountId, saved.getId());
    }

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
        syncTransactionTags(user, saved, update.getTags());
        return getTransactionRow(user, accountId, saved.getId());
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
            transaction.setTransferTo(update.getAmount().signum() < 0);
            transaction.setCategory(null);
            transaction.setWho(null);
            transaction.setPayeeId(null);
            transaction.setPayeeName(null);
            return;
        }

        transaction.setTransferredAccountId(null);
        transaction.setTransfer(false);
        transaction.setTransferTo(false);

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
            transaction.setPayeeId(null);
            transaction.setPayeeName(payeeName);
        }
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
