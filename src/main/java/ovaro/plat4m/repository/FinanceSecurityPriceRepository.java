package ovaro.plat4m.repository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.IFinanceSecurityPriceInPeriod;

/**
 * Spring Data JPA repository for the {@link FinanceSecurityPrice} entity.
 */
@Repository
public interface FinanceSecurityPriceRepository extends JpaRepository<FinanceSecurityPrice, String> {
    // @Query(value="SELECT DISTINCT ON (a.to_currency_id, a.from_currency_id) * " +
    // "FROM fin_fx a INNER JOIN fin_currency c ON uuid(a.from_currency_id) = c.id INNER JOIN fin_currency d ON uuid(a.to_currency_id) = d.id " +
    // "WHERE c.iso_code = :fromCurrencyISO AND d.iso_code = :toCurrencyISO " +
    // "ORDER BY a.to_currency_id, a.from_currency_id, a.date DESC", nativeQuery = true)
    @Query(
        value = "SELECT DISTINCT ON (sp.symbol) * " +
        "FROM fin_security_price sp " +
        "WHERE sp.symbol = :symbol " +
        "ORDER BY sp.symbol, sp.date DESC",
        nativeQuery = true
    )
    FinanceSecurityPrice findLatestBySymbol(@Param("symbol") String symbol);

    @Query(
        value = "SELECT DISTINCT ON (sp.symbol) * " +
        "FROM fin_security_price sp " +
        "WHERE sp.symbol in :symbols and sp.date <= :date " +
        "ORDER BY sp.symbol, sp.date DESC",
        nativeQuery = true
    )
    List<FinanceSecurityPrice> findLatestBySymbolsAndBeforeDate(@Param("symbols") List<String> symbols, @Param("date") LocalDate date);

    @Query(
        value = "SELECT DISTINCT ON (sp.symbol) * " +
        "FROM fin_security_price sp " +
        "WHERE u.id in :ids and sp.date <= :date " +
        "ORDER BY sp.symbol, sp.date DESC",
        nativeQuery = true
    )
    FinanceSecurityPrice findLatestByIdsAndBeforeDate(@Param("ids") List<UUID> ids, @Param("date") ZonedDateTime date);

    // @Query(value="SELECT DISTINCT ON (sp.symbol) * " +
    // "FROM fin_security_price sp INNER JOIN fin_security s ON sp.symbol = s.symbol " +
    // "WHERE s.id = uuid(:id) " +
    // "ORDER BY sp.symbol, sp.date DESC", nativeQuery = true)
    // FinanceSecurityPrice findLatestBySecurityId(@Param("id") String id);

    @Query(
        value = "SELECT DISTINCT ON (sp.symbol) * " +
        "FROM fin_security_price sp INNER JOIN fin_user_security u ON sp.symbol = u.known_symbol " +
        "WHERE u.id in :ids " +
        "ORDER BY sp.symbol, sp.date DESC",
        nativeQuery = true
    )
    List<FinanceSecurityPrice> findLatestBySecurityIdIn(@Param("ids") List<UUID> ids);

    @Query(
        value = "SELECT DISTINCT ON (sp.symbol) * " + "FROM fin_security_price sp " + "ORDER BY sp.symbol, sp.date DESC",
        nativeQuery = true
    )
    List<FinanceSecurityPrice> findLatestAll();

    @Query(
        value = "SELECT sp1.symbol, price, date FROM fin_security_price sp1 INNER JOIN (" +
        "SELECT MAX(date) AS Max_Date, symbol FROM fin_security_price sp2 " +
        "WHERE sp2.symbol in :ids AND (date BETWEEN :start_date AND :end_date) " +
        "GROUP BY symbol) sp2 " +
        "ON sp1.Date=sp2.Max_Date and sp1.symbol = sp2.symbol;",
        nativeQuery = true
    )
    List<IFinanceSecurityPriceInPeriod> findLatestInPeriod(
        @Param("ids") List<String> ids,
        @Param("start_date") LocalDate startDate,
        @Param("end_date") LocalDate endDate
    );
}
