package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.DashboardPanelDefinition;

@Repository
public interface DashboardPanelDefinitionRepository extends JpaRepository<DashboardPanelDefinition, UUID> {
    List<DashboardPanelDefinition> findByScopeOrderByNameAsc(String scope);

    List<DashboardPanelDefinition> findByUserLoginOrderByNameAsc(String login);

    List<DashboardPanelDefinition> findByScopeOrUserLoginOrderByNameAsc(String scope, String login);

    Optional<DashboardPanelDefinition> findOneByIdAndUserLogin(UUID id, String login);
}
