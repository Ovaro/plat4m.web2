package ovaro.plat4m.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceTag;

@Repository
public interface FinanceTagRepository extends JpaRepository<FinanceTag, UUID> {
    List<FinanceTag> findAllByUserGuidAndNormalizedNameIn(String userGuid, Collection<String> normalizedNames);

    Page<FinanceTag> findAllByUserGuidOrderByNameAsc(String userGuid, Pageable pageable);

    Page<FinanceTag> findAllByUserGuidAndNameContainingIgnoreCaseOrderByNameAsc(String userGuid, String query, Pageable pageable);
}
