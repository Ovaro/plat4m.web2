package ovaro.plat4m.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceTransactionImportRow;

@Repository
public interface FinanceTransactionImportRowRepository extends JpaRepository<FinanceTransactionImportRow, UUID> {
    List<FinanceTransactionImportRow> findAllByTransactionImport_IdOrderByRowIndexAsc(UUID importId);
    Optional<FinanceTransactionImportRow> findByIdAndUserGuid(UUID id, String userGuid);
    void deleteAllByTransactionImport_Id(UUID importId);
}
