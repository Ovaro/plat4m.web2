package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceTransactionImport;

@Repository
public interface FinanceTransactionImportRepository extends JpaRepository<FinanceTransactionImport, UUID> {
    Optional<FinanceTransactionImport> findByIdAndUserGuid(UUID id, String userGuid);
    List<FinanceTransactionImport> findAllByUserGuidOrderByCreatedDateTimeDesc(String userGuid);
    List<FinanceTransactionImport> findAllByUserGuidAndAccountIdOrderByCreatedDateTimeDesc(String userGuid, String accountId);
}
