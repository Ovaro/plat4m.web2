package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceCategory;

/**
 * Spring Data JPA repository for the {@link FinanceCategory} entity.
 */
@Repository
public interface FinanceCategoryRepository extends JpaRepository<FinanceCategory, String> {
    public Optional<FinanceCategory> findById(UUID id);

    public List<FinanceCategory> findAllByClassificationId(Integer classificationId);
}
