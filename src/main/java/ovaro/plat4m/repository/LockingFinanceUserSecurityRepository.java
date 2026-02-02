package ovaro.plat4m.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceUserSecurity;

/**
 * Spring Data JPA repository for the {@link FinanceUserSecurity} entity.
 */
@Repository
public interface LockingFinanceUserSecurityRepository extends JpaRepository<FinanceUserSecurity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FinanceUserSecurity> findById(UUID id);
}
