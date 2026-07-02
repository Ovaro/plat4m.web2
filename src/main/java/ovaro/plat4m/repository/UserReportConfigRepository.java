package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.UserReportConfig;

@Repository
public interface UserReportConfigRepository extends JpaRepository<UserReportConfig, UUID> {
    List<UserReportConfig> findByUserLoginAndReportKeyOrderByNameAsc(String login, String reportKey);

    Optional<UserReportConfig> findOneByIdAndUserLogin(UUID id, String login);

    @Query(
        "select case when count(c) > 0 then true else false end from UserReportConfig c " +
            "where c.user.login = :login and c.reportKey = :reportKey and lower(c.name) = lower(:name) and (:excludeId is null or c.id <> :excludeId)"
    )
    boolean existsNameForUserReport(
        @Param("login") String login,
        @Param("reportKey") String reportKey,
        @Param("name") String name,
        @Param("excludeId") UUID excludeId
    );
}
