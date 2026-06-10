package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinancePayee;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface FinancePayeeRepository extends JpaRepository<FinancePayee, String> {
    //List<Account> findAccounts();
    Optional<FinancePayee> findById(UUID uuid);
    List<FinancePayee> findByUserGuidOrderByNameAsc(String userGuid);

    @Query(
        "select p from FinancePayee p where p.userGuid = :userGuid and (p.hidden = false or p.hidden is null) order by lower(p.name), p.name"
    )
    List<FinancePayee> findVisibleByUserGuid(@Param("userGuid") String userGuid);

    @Query(
        "select p from FinancePayee p where p.userGuid = :userGuid and (p.hidden = false or p.hidden is null) order by lower(p.name), p.name"
    )
    Page<FinancePayee> findVisibleByUserGuid(@Param("userGuid") String userGuid, Pageable pageable);

    @Query(
        "select p from FinancePayee p where p.userGuid = :userGuid and (p.hidden = false or p.hidden is null) and lower(p.name) like lower(concat('%', :name, '%')) order by lower(p.name), p.name"
    )
    Page<FinancePayee> findVisibleByUserGuidAndNameContaining(
        @Param("userGuid") String userGuid,
        @Param("name") String name,
        Pageable pageable
    );
}
