package ovaro.plat4m.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ovaro.plat4m.service.dto.FinanceTransactionRowDTO;

public interface FinanceTransactionRepositoryCustom {
    Page<FinanceTransactionRowDTO> findTransactionRows(String userGuid, String accountId, Pageable pageable, String filterModel);
    FinanceTransactionRowDTO findTransactionRowById(String userGuid, String accountId, UUID transactionId);
}
