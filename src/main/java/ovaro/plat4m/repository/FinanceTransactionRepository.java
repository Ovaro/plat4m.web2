package ovaro.plat4m.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceSecurityInvestmentSummary;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.IFinanceMonthlySummary;
import ovaro.plat4m.domain.IFinanceTransactionWithRunningBalance;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID> {
    //Page<Transaction> findTransactions(Pageable pageable);
    // @Query("SELECT e FROM SourceLink e WHERE e.sourceId IN (:ids) AND e.sourceEntity = :sourceEntity AND e.sourceTypeId = :sourceTypeId")
    // List<Transaction> findBySourceEntityAndSourceTypeIdInSourceIds(String accountId);
    List<FinanceTransaction> findAllByAccountIdOrderByDate(String accountId);

    //sum(amount) over (order by date asc rows between unbounded preceding and current row)

    Optional<FinanceTransaction> findById(UUID id);

    @Query(
        "SELECT sum(amount) as balance FROM FinanceTransaction f WHERE f.accountId = :accountId AND f.voided = FALSE AND  f.splitChild = FALSE AND f.recurring = FALSE"
    )
    BigDecimal findSumTransactionsForAccount(String accountId);

    // This means everything up to the date (i.e 09-01-2022 will show the balance at the 00:00 09-01-2022)
    @Query(
        "SELECT sum(amount) as balance FROM FinanceTransaction f WHERE f.accountId = :accountId AND f.voided = FALSE AND  f.splitChild = FALSE AND f.recurring = FALSE AND f.date < :date"
    )
    BigDecimal findSumTransactionsForAccountUpToDate(String accountId, LocalDate date);

    @Query(
        "SELECT accountId,sum(amount) as balance FROM FinanceTransaction f WHERE f.voided = FALSE AND  f.splitChild = FALSE AND f.recurring = FALSE group by f.accountId"
    )
    List<Object[]> findSumTransactions();

    @Query(
        "SELECT accountId,sum(amount) as balance FROM FinanceTransaction f WHERE f.voided = FALSE AND  f.splitChild = FALSE AND f.recurring = FALSE  AND f.date < :date group by f.accountId"
    )
    List<Object[]> findSumTransactionsUpToDate(LocalDate date);

    Page<FinanceTransaction> findAllByAccountId(String accountId, Pageable pageable);

    //@Query(value="SELECT *, sum(amount) over (order by date asc rows between unbounded preceding and current row) as running_balance from fin_transaction f WHERE f.account_id = :accountId and not voided and not split_child and not recurring ORDER BY date",nativeQuery = true)
    @Query(
        value = "SELECT *, sum(amount) over (order by date asc rows between unbounded preceding and current row) as running_balance from fin_transaction f WHERE f.account_id = :account_id and f.user_guid = :user_guid and not voided and not split_child and not recurring ORDER BY date",
        nativeQuery = true
    )
    List<FinanceTransaction> findAllByAccountIdWithBalanceNonPage(
        @Param("user_guid") String userGuid,
        @Param("account_id") String accountId
    );

    @Query(
        value = "SELECT f.*, f.transfer_to as transferTo, f.split_parent as splitParent, f.split_child as splitChild, f.account_id as accountId, f.master_guid as masterGuid, f.category_id as categoryId, c.name as categoryName, c.parent_category_id as parentCategoryId, pc.name as parentCategoryName, f.payee_name as payeeName, f.transferred_account_id as transferredAccountId, f.statement_id as statementId, f.status_flag as statusFlag, f.security_id as securityId, f.payee_id as payeeId, f.investment_activity_type as investmentActivityTypeId, sum(amount) over (order by date asc rows between unbounded preceding and current row) as runningBalance from fin_transaction f LEFT JOIN fin_category c ON uuid(c.id) = uuid(f.category_id) LEFT JOIN fin_category pc ON uuid(pc.id) = uuid(c.parent_category_id) WHERE f.account_id = :account_id and f.user_guid = :user_guid and not voided and not split_child and not recurring ORDER BY f.date DESC, f.number DESC",
        nativeQuery = true
    )
    Page<IFinanceTransactionWithRunningBalance> findAllByAccountIdWithBalance(
        @Param("user_guid") String userGuid,
        @Param("account_id") String accountId,
        Pageable pageable
    );

    @Query(
        value = "select a.id as accountId, a.name as accountName, f.security_id as securityId, u.name, u.type as securityType, u.known_symbol as symbol, u.symbol as userSymbol, s.currency_code as currencyCode, " +
        "sum(quantity) FILTER (WHERE f.investment_activity_type in (1,9,12,16,32)) as addSec, " +
        "sum(quantity*-1) FILTER (WHERE f.investment_activity_type in (2,13,33)) as removeSec " +
        "FROM fin_transaction f INNER JOIN fin_account a ON a.id = uuid(f.account_id) INNER JOIN fin_security u ON u.id = uuid(f.security_id) LEFT JOIN fin_security s ON s.symbol = u.known_symbol  " +
        "WHERE a.name = :account_name and f.user_guid = :user_guid " +
        "GROUP BY f.security_id, u.name, u.type, a.id, a.name, u.symbol, u.known_symbol, s.currency_code",
        nativeQuery = true
    )
    List<FinanceSecurityInvestmentSummary> findSecurityInvestmentTransactions(
        @Param("user_guid") String userGuid,
        @Param("account_name") String accountName
    );

    // @Query(value="select a.id as accountId, a.name as accountName, f.security_id as securityId, u.name as name, u.known_symbol as symbol, u.symbol as userSymbol, s.currency_code as currencyCode, " +
    //         "sum(quantity) FILTER (WHERE f.investment_activity_type in (1,9,12,16,32)) as addSec, " +
    //         "sum(quantity*-1) FILTER (WHERE f.investment_activity_type in (2,13,33)) as removeSec, " +
    //         "s.sector as sector, s.industry as industry, s.exchange_name as exchangeName " +
    //     "FROM fin_transaction f INNER JOIN fin_account a ON a.id = uuid(f.account_id) INNER JOIN fin_security u ON u.id = uuid(f.security_id) LEFT JOIN fin_security s ON s.symbol = u.known_symbol  " +
    //     "where f.account_id = :account_id and u.type in (1,2,7) and f.user_guid = :user_guid " +
    //     "GROUP BY f.security_id, u.name, a.id, a.name, u.symbol, u.known_symbol, s.currency_code, s.sector, s.industry, s.exchange_name;",nativeQuery = true)

    @Query(
        value = "select a.id as accountId, a.name as accountName, f.security_id as securityId, u.type as securityType, u.name as name, u.known_symbol as symbol, u.symbol as userSymbol, s.currency_code as currencyCode, " +
        "sum(quantity) FILTER (WHERE f.investment_activity_type in (1,9,12,16,32)) as addSec, " +
        "sum(quantity*-1) FILTER (WHERE f.investment_activity_type in (2,13,33)) as removeSec, " +
        "s.sector as sector, s.industry as industry, s.exchange_name as exchangeName " +
        "FROM fin_transaction f INNER JOIN fin_account a ON a.id = uuid(f.account_id) INNER JOIN fin_user_security u ON u.id = uuid(f.security_id) LEFT JOIN fin_security s ON s.symbol = u.known_symbol  " +
        "where f.account_id = :account_id and u.type in (1,2,7,10) and f.user_guid = :user_guid " +
        "GROUP BY f.security_id, u.type, u.name, a.id, a.name, u.symbol, u.known_symbol, s.currency_code, s.sector, s.industry, s.exchange_name;",
        nativeQuery = true
    )
    List<FinanceSecurityInvestmentSummary> findSecurityInvestmentTransactionsForAccountId(
        @Param("user_guid") String userGuid,
        @Param("account_id") String accountId
    );

    @Query(
        value = "select a.id as accountId, a.name as accountName, f.security_id as securityId, u.type as securityType, u.name as name, u.known_symbol as symbol, u.symbol as userSymbol, s.currency_code as currencyCode, " +
        "sum(quantity) FILTER (WHERE f.investment_activity_type in (1,9,12,16,32)) as addSec, " +
        "sum(quantity*-1) FILTER (WHERE f.investment_activity_type in (2,13,33)) as removeSec, " +
        "s.sector as sector, s.industry as industry, s.exchange_name as exchangeName " +
        "FROM fin_transaction f INNER JOIN fin_account a ON a.id = uuid(f.account_id) INNER JOIN fin_user_security u ON u.id = uuid(f.security_id) LEFT JOIN fin_security s ON s.symbol = u.known_symbol  " +
        "where u.type in (1,2,7,10) and f.user_guid = :user_guid and a.closed = false " +
        "GROUP BY f.security_id, u.type, u.name, a.id, a.name, u.symbol, u.known_symbol, s.currency_code, s.sector, s.industry, s.exchange_name;",
        nativeQuery = true
    )
    List<FinanceSecurityInvestmentSummary> findSecurityInvestmentTransactionsForOpenAccounts(@Param("user_guid") String userGuid);

    @Query(
        value = "select a.id as accountId, a.name as accountName, f.security_id as securityId, u.type as securityType, u.name as name, u.known_symbol as symbol, u.symbol as userSymbol, s.currency_code as currencyCode, " +
        "sum(quantity) FILTER (WHERE f.investment_activity_type in (1,9,12,16,32)) as addSec, " +
        "sum(quantity*-1) FILTER (WHERE f.investment_activity_type in (2,13,33)) as removeSec, " +
        "s.sector as sector, s.industry as industry, s.exchange_name as exchangeName " +
        "FROM fin_transaction f INNER JOIN fin_account a ON a.id = uuid(f.account_id) INNER JOIN fin_user_security u ON u.id = uuid(f.security_id) LEFT JOIN fin_security s ON s.symbol = u.known_symbol  " +
        "where u.type in (1,2,7,10) and f.user_guid = :user_guid and a.closed = false AND f.date < :date " +
        "GROUP BY f.security_id, u.type, u.name, a.id, a.name, u.symbol, u.known_symbol, s.currency_code, s.sector, s.industry, s.exchange_name;",
        nativeQuery = true
    )
    List<FinanceSecurityInvestmentSummary> findSecurityInvestmentTransactionsForOpenAccountsUptoDate(
        @Param("user_guid") String userGuid,
        LocalDate date
    );

    List<FinanceTransaction> findByUserGuidAndSecurityIdOrderByDateDesc(String userGuid, String securityId);

    List<FinanceTransaction> findByUserGuidAndInvestmentOrderByDateDesc(String userGuid, boolean investment);

    @Query(
        value = "with data as (" +
        "SELECT f.account_id, EXTRACT(YEAR from f.date) as y, EXTRACT(MONTH from f.date) as m, sum(amount) as amt " +
        "FROM fin_transaction f " +
        "WHERE f.user_guid = :user_guid and (not voided and not split_child and not recurring) " +
        "GROUP BY f.account_id, y, m " +
        "ORDER BY y DESC, m DESC " +
        ") " +
        "select account_id as accountId, a.name as accountName, y, m, sum(amt) over (PARTITION BY account_id ORDER BY y ASC, m ASC rows between unbounded preceding and current row) as runningBalance,  " +
        "(select rate " +
        "    from fin_fx f " +
        "    where f.date <= (make_date(y\\:\\:int,m\\:\\:int,1) + interval '1 month') and f.from_iso_code = a.currency_code and f.to_iso_code = :to_iso_code " +
        "    order by f.date desc " +
        "    LIMIT 1) as rate " +
        "from data LEFT JOIN fin_account a ON uuid(account_id) = uuid(a.id) where a.type <> 5 " +
        "GROUP BY accountId, a.id, y, m, amt " +
        "ORDER BY y DESC, m DESC;",
        nativeQuery = true
    )
    List<IFinanceMonthlySummary> findAllAccountsMonthlyRunningBalance(
        @Param("user_guid") String userGuid,
        @Param("to_iso_code") String toCurrencyCode
    );
}
