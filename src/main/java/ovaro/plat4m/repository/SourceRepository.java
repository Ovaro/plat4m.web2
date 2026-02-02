package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.Source;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface SourceRepository extends JpaRepository<Source, String> {
    List<Source> findByUserGuid(String userGuid);
    List<Source> findByUserGuidAndTypeId(String userGuid, Integer typeId);
    Optional<Source> findByUserGuidAndTypeIdAndName(String userGuid, Integer typeId, String name);
    //Page<Transaction> findTransactions(Pageable pageable);
}
