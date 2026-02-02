package ovaro.plat4m.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceInstitution;

/**
 * Spring Data JPA repository for the {@link FinanceInstitution} entity.
 */
@Repository
public interface FinanceInstitutionRepository extends JpaRepository<FinanceInstitution, String> {
    Optional<FinanceInstitution> findById(UUID uuid);
    //List<Account> findAccounts();
}
