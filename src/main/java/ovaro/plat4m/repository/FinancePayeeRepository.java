package ovaro.plat4m.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinancePayee;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface FinancePayeeRepository extends JpaRepository<FinancePayee, String> {
    //List<Account> findAccounts();
    Optional<FinancePayee> findById(UUID uuid);
}
