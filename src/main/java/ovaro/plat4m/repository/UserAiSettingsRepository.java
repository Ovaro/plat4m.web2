package ovaro.plat4m.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.UserAiSettings;

@Repository
public interface UserAiSettingsRepository extends JpaRepository<UserAiSettings, UUID> {
    Optional<UserAiSettings> findOneByUserLogin(String login);
}
