package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceCategory;

/**
 * Spring Data JPA repository for the {@link FinanceCategory} entity.
 */
@Repository
public interface FinanceCategoryRepository extends JpaRepository<FinanceCategory, String> {
    public Optional<FinanceCategory> findById(UUID id);

    public List<FinanceCategory> findAllByClassificationId(Integer classificationId);
    public List<FinanceCategory> findAllByClassificationIdAndUserGuid(Integer classificationId, String userGuid);
    public List<FinanceCategory> findAllByUserGuid(String userGuid);
    public boolean existsByParent_IdAndUserGuid(UUID parentId, String userGuid);

    @Query(
        "select c from FinanceCategory c where c.classificationId = :classificationId and (c.userGuid = :userGuid or c.systemCategory = true)"
    )
    public List<FinanceCategory> findAllByClassificationIdAndUserGuidOrSystem(
        @Param("classificationId") Integer classificationId,
        @Param("userGuid") String userGuid
    );

    @Query(
        "select c from FinanceCategory c where c.classificationId = :classificationId and (c.userGuid = :userGuid or c.systemCategory = true) and (c.hidden = false or c.hidden is null)"
    )
    public List<FinanceCategory> findVisibleByClassificationIdAndUserGuidOrSystem(
        @Param("classificationId") Integer classificationId,
        @Param("userGuid") String userGuid
    );

    @Query("select c from FinanceCategory c where c.userGuid = :userGuid or c.systemCategory = true")
    public List<FinanceCategory> findAllByUserGuidOrSystem(@Param("userGuid") String userGuid);

    @Query(
        "select c from FinanceCategory c where (c.userGuid = :userGuid or c.systemCategory = true) and (:includeHidden = true or c.hidden = false or c.hidden is null)"
    )
    public List<FinanceCategory> findManagedByUserGuidOrSystem(
        @Param("userGuid") String userGuid,
        @Param("includeHidden") boolean includeHidden
    );

    @Query("select c from FinanceCategory c where c.id = :id and (c.userGuid = :userGuid or c.systemCategory = true)")
    public Optional<FinanceCategory> findByIdAndUserGuidOrSystem(@Param("id") UUID id, @Param("userGuid") String userGuid);

    @Query(
        "select count(c) > 0 from FinanceCategory c where c.parent.id = :parentId and (c.userGuid = :userGuid or c.systemCategory = true)"
    )
    public boolean existsAccessibleChild(@Param("parentId") UUID parentId, @Param("userGuid") String userGuid);

    public List<FinanceCategory> findAllByTemplateCategoryTrueOrderByLevelAscNameAsc();

    public List<FinanceCategory> findAllByUserGuidAndPlatformDefaultCategoryTrue(String userGuid);
}
