package ovaro.plat4m.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.UserDashboardConfig;

@Repository
public interface UserDashboardConfigRepository extends JpaRepository<UserDashboardConfig, UUID> {
    Optional<UserDashboardConfig> findFirstByUserLoginAndActiveTrueOrderByLastModifiedDateDesc(String login);

    Optional<UserDashboardConfig> findOneByIdAndUserLogin(UUID id, String login);
}
