package ovaro.plat4m.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceInvestmentEvent;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface FinanceInvestmentEventRepository extends JpaRepository<FinanceInvestmentEvent, String> {
    //List<Account> findAccounts();
    List<FinanceInvestmentEvent> findAllByUserSecurityId(String userSecurityId);

    @Query("select f from FinanceInvestmentEvent f where f.userSecurityId = :userSecurityId AND f.date <= :date order by date")
    List<FinanceInvestmentEvent> findAllByUserSecurityIdOrderByDate(
        @Param(value = "userSecurityId") String userSecurityId,
        @Param(value = "date") LocalDate date
    );

    @Query("select f from FinanceInvestmentEvent f where f.userSecurityId in :userSecurityIds AND f.date <= :date order by date")
    List<FinanceInvestmentEvent> findAllByUserSecurityIdsOrderByDate(
        @Param(value = "userSecurityIds") List<String> userSecurityIds,
        @Param(value = "date") LocalDate date
    );

    @Query("select f from FinanceInvestmentEvent f where f.userGuid = :userGuid AND f.date <= :date order by date")
    List<FinanceInvestmentEvent> findAllByUserGuidOrderByDate(
        @Param(value = "userGuid") String userGuid,
        @Param(value = "date") LocalDate date
    );
}
