package ovaro.plat4m.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceLot;

@Repository
public interface FinanceLotRepository extends JpaRepository<FinanceLot, UUID> {
    Optional<FinanceLot> findById(UUID id);
    Optional<FinanceLot> findByIdAndUserGuid(UUID id, String userGuid);
    List<FinanceLot> findAllByUserGuidAndIdIn(String userGuid, Collection<UUID> ids);
    List<FinanceLot> findByUserGuidAndSecurityIdOrderByOpenDateAscIdAsc(String userGuid, String securityId);

    @Query(
        """
        select l from FinanceLot l
        where l.userGuid = :userGuid
          and (l.sellTransactionId in :transactionIds or l.closeTransactionId in :transactionIds)
        order by l.openDate asc, l.id asc
        """
    )
    List<FinanceLot> findSellLotsByUserGuidAndTransactionIds(
        @Param("userGuid") String userGuid,
        @Param("transactionIds") Collection<UUID> transactionIds
    );

    @Query(
        """
        select l from FinanceLot l
        where l.userGuid = :userGuid
          and (l.sellTransactionId = :transactionId or l.closeTransactionId = :transactionId)
        order by l.openDate asc, l.id asc
        """
    )
    List<FinanceLot> findSellLotsByUserGuidAndTransactionId(@Param("userGuid") String userGuid, @Param("transactionId") UUID transactionId);

    @Query(
        """
        select l from FinanceLot l
        where l.userGuid = :userGuid
          and (l.buyTransactionId in :transactionIds or l.openTransactionId in :transactionIds)
        order by l.buyDate asc, l.openDate asc, l.id asc
        """
    )
    List<FinanceLot> findBuyLotsByUserGuidAndTransactionIds(
        @Param("userGuid") String userGuid,
        @Param("transactionIds") Collection<UUID> transactionIds
    );
}
