package ovaro.plat4m.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceSecurity;

/**
 * Spring Data JPA repository for the {@link FinanceCurrency} entity.
 */
@Repository
public interface FinanceSecurityRepository extends JpaRepository<FinanceSecurity, String> {
    Optional<FinanceSecurity> findBySymbol(String symbol);
}
