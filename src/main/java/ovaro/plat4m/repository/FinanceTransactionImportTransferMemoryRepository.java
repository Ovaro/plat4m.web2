package ovaro.plat4m.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceTransactionImportTransferMemory;

@Repository
public interface FinanceTransactionImportTransferMemoryRepository extends JpaRepository<FinanceTransactionImportTransferMemory, UUID> {
    Optional<FinanceTransactionImportTransferMemory> findFirstByUserGuidAndSourceAccountIdAndNormalizedTransferText(
        String userGuid,
        String sourceAccountId,
        String normalizedTransferText
    );

    Optional<FinanceTransactionImportTransferMemory> findFirstByUserGuidAndSourceAccountIdIsNullAndNormalizedTransferText(
        String userGuid,
        String normalizedTransferText
    );
}
