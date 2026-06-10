package ovaro.plat4m.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceTransactionTag;

@Repository
public interface FinanceTransactionTagRepository extends JpaRepository<FinanceTransactionTag, UUID> {
    void deleteAllByTransaction_Id(UUID transactionId);

    @Query(
        value = "select ftt.transaction_id as transactionId, ft.name as tagName, ftt.sort_order as sortOrder " +
            "from fin_transaction_tag ftt " +
            "inner join fin_tag ft on uuid(ft.id) = uuid(ftt.tag_id) " +
            "where ftt.transaction_id in :transactionIds " +
            "order by ftt.transaction_id, coalesce(ftt.sort_order, 0), lower(ft.name)",
        nativeQuery = true
    )
    List<FinanceTransactionTagNameProjection> findTagNamesByTransactionIds(@Param("transactionIds") List<UUID> transactionIds);

    interface FinanceTransactionTagNameProjection {
        UUID getTransactionId();
        String getTagName();
        Integer getSortOrder();
    }
}
