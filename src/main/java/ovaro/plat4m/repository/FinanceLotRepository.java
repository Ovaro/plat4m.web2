package ovaro.plat4m.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceLot;

@Repository
public interface FinanceLotRepository extends JpaRepository<FinanceLot, UUID> {
    Optional<FinanceLot> findById(UUID id);
    Optional<FinanceLot> findByIdAndUserGuid(UUID id, String userGuid);
    List<FinanceLot> findAllByUserGuidAndIdIn(String userGuid, Collection<UUID> ids);
    List<FinanceLot> findByUserGuidAndSecurityIdOrderByOpenDateAscIdAsc(String userGuid, String securityId);
}
