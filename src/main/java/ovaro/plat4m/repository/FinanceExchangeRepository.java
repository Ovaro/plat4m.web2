package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceExchange;

/**
 * Spring Data JPA repository for the {@link FinanceExchange} entity.
 */
@Repository
public interface FinanceExchangeRepository extends JpaRepository<FinanceExchange, String> {
    Optional<FinanceExchange> findByMic(String mic);
    List<FinanceExchange> findByCurrencyCode(String currencyCode);
    List<FinanceExchange> findByCountryCode(String countryCode);
}
