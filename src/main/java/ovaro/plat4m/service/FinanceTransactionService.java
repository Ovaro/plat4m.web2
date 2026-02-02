package ovaro.plat4m.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceInvestmentActivityType;
import ovaro.plat4m.domain.FinanceSecurityInvestmentSummary;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.IFinanceMonthlySummary;
import ovaro.plat4m.domain.IFinanceTransactionWithRunningBalance;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.service.dto.FinanceInvestmentTransactionDTO;

@Service
public class FinanceTransactionService {

    private static final Logger log = LoggerFactory.getLogger(FinanceTransactionService.class);
    private FinanceTransactionRepository transactionRepository;

    public FinanceTransactionService(FinanceTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Page<FinanceTransaction> getTransactions(String accountId, Pageable pageable) {
        Page<FinanceTransaction> transactions = this.transactionRepository.findAllByAccountId(accountId, pageable);

        return transactions;
    }

    public List<FinanceTransaction> getTransactionsNonPage(User user, String accountId) {
        List<FinanceTransaction> transactions =
            this.transactionRepository.findAllByAccountIdWithBalanceNonPage(user.getGuid().toString(), accountId);

        return transactions;
    }

    public Page<IFinanceTransactionWithRunningBalance> getTransactionsPaging(User user, String accountId, Pageable pageable) {
        Page<IFinanceTransactionWithRunningBalance> transactions =
            this.transactionRepository.findAllByAccountIdWithBalance(user.getGuid().toString(), accountId, pageable);
        // TODO: needs to be updated as modifing a collection durting loop.
        // for(FinanceTransactionWithRunningBalance txn : transactions.getContent()) {
        //     if(txn.getInvestmentActivityTypeId() != null){
        //         log.info("Converting FinanceInvestmentActivityType: " + txn.getInvestmentActivityTypeId());
        //         String name = FinanceInvestmentActivityType.valueOf(txn.getInvestmentActivityTypeId()).name;
        //         txn.setInvestmentActivityType(name);
        //     }
        // }
        return transactions;
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
        List<FinanceSecurityInvestmentSummary> transactions =
            this.transactionRepository.findSecurityInvestmentTransactionsForAccountId(user.getGuid().toString(), accountId);
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
        List<FinanceTransaction> transactions =
            this.transactionRepository.findByUserGuidAndSecurityIdOrderByDateDesc(user.getGuid().toString(), securityId);
        List<FinanceInvestmentTransactionDTO> dtos = new ArrayList<FinanceInvestmentTransactionDTO>();
        for (FinanceTransaction txn : transactions) {
            dtos.add(new FinanceInvestmentTransactionDTO(txn));
        }
        return dtos;
    }

    public List<FinanceInvestmentTransactionDTO> findByUser(User user) {
        List<FinanceTransaction> transactions =
            this.transactionRepository.findByUserGuidAndInvestmentOrderByDateDesc(user.getGuid().toString(), true);
        List<FinanceInvestmentTransactionDTO> dtos = new ArrayList<FinanceInvestmentTransactionDTO>();
        for (FinanceTransaction txn : transactions) {
            dtos.add(new FinanceInvestmentTransactionDTO(txn));
        }
        return dtos;
    }

    public List<IFinanceMonthlySummary> getAllAccountsMonthlyRunningBalance(User user) {
        List<IFinanceMonthlySummary> s =
            this.transactionRepository.findAllAccountsMonthlyRunningBalance(user.getGuid().toString(), user.getLocalCurrency());
        return s;
    }
}
