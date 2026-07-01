package ovaro.plat4m.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceTransactionImportMemory;

@Repository
public interface FinanceTransactionImportMemoryRepository extends JpaRepository<FinanceTransactionImportMemory, UUID> {
    Optional<FinanceTransactionImportMemory> findFirstByUserGuidAndAccountIdAndNormalizedPayeeText(
        String userGuid,
        String accountId,
        String normalizedPayeeText
    );
    Optional<FinanceTransactionImportMemory> findFirstByUserGuidAndAccountIdIsNullAndNormalizedPayeeText(
        String userGuid,
        String normalizedPayeeText
    );
}
