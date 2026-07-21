package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceCustomPortfolio;

@Repository
public interface FinanceCustomPortfolioRepository extends JpaRepository<FinanceCustomPortfolio, UUID> {
    List<FinanceCustomPortfolio> findAllByUserGuidOrderByNameAsc(String userGuid);
    Optional<FinanceCustomPortfolio> findByIdAndUserGuid(UUID id, String userGuid);
    boolean existsByUserGuidAndNameIgnoreCase(String userGuid, String name);
    boolean existsByUserGuidAndNameIgnoreCaseAndIdNot(String userGuid, String name, UUID id);
}
