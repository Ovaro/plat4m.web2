package ovaro.plat4m.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.IFinanceCategorisedIncomeExpenses;

/**
 * Spring Data JPA repository for the {@link FinanceAccount} entity.
 */
@Repository
public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, String> {
    List<FinanceAccount> findAllByUserGuid(String userGuid);
    List<FinanceAccount> findAllByUserGuidAndDateOpenedBefore(String userGuid, LocalDate dateOpened);
    List<FinanceAccount> findAllByUserGuidAndTypeAndClosed(String userGuid, Integer type, boolean closed);
    List<FinanceAccount> findAllByUserGuidAndType(String userGuid, Integer type);
    List<FinanceAccount> findAllByUserGuidAndClosed(String userGuid, boolean closed);
    Optional<FinanceAccount> findById(UUID uuid);

    /*
     * Custom Queries
     */

    @Query(
        value = "SELECT sum(amount) as amount, category_id as categoryId,  pc.id as parentCategoryID, pc2.id as parentParentCategoryId, c.name as categoryName, pc.name as parentCategoryName, pc2.name as parentParentCategoryName, transferred_account_id " +
        "from fin_transaction f LEFT JOIN fin_category c ON uuid(c.id) = uuid(f.category_id) LEFT JOIN fin_category pc ON uuid(pc.id) = uuid(c.parent_category_id) LEFT JOIN fin_category pc2 ON uuid(pc2.id) = uuid(pc.parent_category_id) " +
        "WHERE date <= :end_date AND date >= :start_date AND not voided and not split_parent and not recurring " +
        "GROUP BY transferred_account_id, categoryId, parentCategoryID, parentParentCategoryID, categoryName, parentCategoryName, parentParentCategoryName " +
        "order by amount;",
        nativeQuery = true
    )
    List<IFinanceCategorisedIncomeExpenses> findIncomeExpensesForPeriod(
        @Param("start_date") LocalDate startDate,
        @Param("end_date") LocalDate endDate
    );
}
