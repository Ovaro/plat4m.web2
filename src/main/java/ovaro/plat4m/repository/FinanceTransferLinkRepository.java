package ovaro.plat4m.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceTransferLink;

@Repository
public interface FinanceTransferLinkRepository extends JpaRepository<FinanceTransferLink, UUID> {
    @Query(
        "select x from FinanceTransferLink x " + "where x.userGuid = :userGuid and (x.fromId = :transactionId or x.linkId = :transactionId)"
    )
    Optional<FinanceTransferLink> findByUserGuidAndTransactionId(
        @Param("userGuid") String userGuid,
        @Param("transactionId") UUID transactionId
    );
}
