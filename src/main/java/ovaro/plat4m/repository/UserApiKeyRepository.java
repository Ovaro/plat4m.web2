package ovaro.plat4m.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.UserApiKey;

@Repository
public interface UserApiKeyRepository extends JpaRepository<UserApiKey, UUID> {
    Optional<UserApiKey> findOneById(UUID id);

    List<UserApiKey> findAllByUserLoginAndRevokedAtIsNullOrderByCreatedDateDesc(String login);

    Optional<UserApiKey> findOneByIdAndUserLoginAndRevokedAtIsNull(UUID id, String login);

    List<UserApiKey> findAllByRevokedAtIsNullAndExpiresAtBefore(Instant cutoff);
}
