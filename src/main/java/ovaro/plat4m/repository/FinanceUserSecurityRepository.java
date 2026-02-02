package ovaro.plat4m.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
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
public interface FinanceUserSecurityRepository extends JpaRepository<FinanceUserSecurity, String> {
    Optional<FinanceUserSecurity> findById(UUID id);
    Optional<FinanceUserSecurity> findBySymbol(String symbol);
    Optional<FinanceUserSecurity> findByMasterGuid(String masterGuid);
    List<FinanceUserSecurity> findByUserGuid(String userGuid);
    Optional<FinanceUserSecurity> findByIdAndUserGuid(UUID id, String userGuid);
}
