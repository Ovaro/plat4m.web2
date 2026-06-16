package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.SourceLink;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface SourceLinkRepository extends JpaRepository<SourceLink, Long> {
    //Page<Transaction> findTransactions(Pageable pageable);
    List<SourceLink> findByUserGuidAndSourceEntityAndSourceTypeId(String userGuid, String sourceEntity, String sourceTypeId);
    SourceLink findByUserGuidAndSourceEntityAndSourceId(String userGuid, String sourceEntity, String sourceId);
    List<SourceLink> findByUserGuidAndLocalIdIn(String userGuid, List<String> localIds);

    Optional<SourceLink> findByUserGuidAndSourceIdAndSourceEntityAndSourceTypeId(
        String userGuid,
        String sourceId,
        String sourceEntity,
        String sourceTypeId
    );

    @Query(
        "SELECT e FROM SourceLink e WHERE e.userGuid = :userGuid AND e.sourceId IN (:ids) AND e.sourceEntity = :sourceEntity AND e.sourceTypeId = :sourceTypeId"
    )
    List<SourceLink> findBySourceEntityAndSourceTypeIdInSourceIds(
        @Param("userGuid") String userGuid,
        @Param("sourceEntity") String sourceEntity,
        @Param("sourceTypeId") String sourceTypeId,
        @Param("ids") List<String> sourceIds
    );
}
