package ovaro.plat4m.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.domain.FinanceFX;

/**
 * Spring Data JPA repository for the {@link FinanceCurrency} entity.
 */
@Repository
public interface FinanceFXRepository extends JpaRepository<FinanceFX, String> {
    //@Query(value="SELECT DISTINCT ON (a.to_currency_id, a.from_currency_id) * FROM fin_fx a ORDER BY a.to_currency_id, a.from_currency_id, a.date DESC;", nativeQuery=true)
    List<FinanceFX> findAll();

    //@Query(value="SELECT DISTINCT ON (a.to_currency_id, a.from_currency_id) * FROM fin_fx a INNER JOIN  WHERE a.from_currency_id = :fromCurrencyId AND a.to_currency_id = :toCurrencyId ORDER BY a.to_currency_id, a.from_currency_id, a.date DESC;", nativeQuery=true)

    @Query(
        value = "SELECT DISTINCT ON (a.to_iso_code, a.from_iso_code) * " +
        "FROM fin_fx a " +
        "WHERE (a.from_iso_code = :from_iso_code AND a.to_iso_code = :to_iso_code) OR (a.from_iso_code = :to_iso_code AND a.to_iso_code = :from_iso_code) and date <= :date " +
        "ORDER BY a.to_iso_code, a.from_iso_code, a.date DESC",
        nativeQuery = true
    )
    List<FinanceFX> findByFromIsoCodeAndToIsoCode(
        @Param("from_iso_code") String fromIsoCode,
        @Param("to_iso_code") String toIsoCode,
        @Param("date") LocalDate date
    );

    @Query(
        value = "WITH subq AS (   " +
        "SELECT *, COUNT(*) OVER(ORDER BY date desc " +
        "RANGE BETWEEN (INTERVAL '1' DAY) * :days PRECEDING AND CURRENT ROW) as cnt " +
        "FROM fin_fx f  " +
        "WHERE ((f.from_iso_code = :from_iso_code and f.to_iso_code = :to_iso_code) OR (f.from_iso_code = :to_iso_code and f.to_iso_code = :from_iso_code))  AND date BETWEEN :from_date AND :to_date ) SELECT * FROM subq where cnt = 1 ORDER BY date;",
        nativeQuery = true
    )
    List<FinanceFX> findFXSummaries(
        @Param("from_iso_code") String fromIsoCode,
        @Param("to_iso_code") String toIsoCode,
        @Param("from_date") LocalDate fromDate,
        @Param("to_date") LocalDate toDate,
        @Param("days") int days
    ); //

    @Query(
        value = "WITH subq AS (   " +
        "SELECT *, COUNT(*) OVER(ORDER BY date desc " +
        "RANGE BETWEEN (INTERVAL '1' DAY) * :days PRECEDING AND CURRENT ROW) as cnt " +
        "FROM fin_fx f  " +
        "WHERE (f.from_iso_code in (:from_iso_code) OR f.to_iso_code in (:to_iso_code)) AND date BETWEEN :from_date AND :to_date ) SELECT * FROM subq where cnt = 1 ORDER BY date;",
        nativeQuery = true
    )
    List<FinanceFX> findFXSummariesMultiple(
        @Param("from_iso_code") List<String> fromIsoCode,
        @Param("to_iso_code") List<String> toIsoCode,
        @Param("from_date") LocalDate fromDate,
        @Param("to_date") LocalDate toDate,
        @Param("days") int days
    );

    @Query(
        value = "SELECT id, DATE_PART('year', date) as y, DATE_PART('month', date) as m, date, from_iso_code, to_iso_code, rate FROM ( " +
        "  SELECT *, " +
        "    ROW_NUMBER() OVER (PARTITION BY from_iso_code, to_iso_code, DATE_PART('month', x.date) + DATE_PART('year', x.date) ORDER BY x.date DESC) rn " +
        " FROM fin_fx AS x) xx " +
        " WHERE rn = 1 and from_iso_code=:from_iso_code and to_iso_code=:to_iso_code and date BETWEEN :from_date AND :to_date" +
        " ORDER BY date DESC;",
        nativeQuery = true
    )
    List<FinanceFX> monthlyFX(
        @Param("from_iso_code") String fromIsoCode,
        @Param("to_iso_code") String toIsoCode,
        @Param("from_date") LocalDate fromDate,
        @Param("to_date") LocalDate toDate
    ); //

    // Same as above but asc
    @Query(
        value = "SELECT id, DATE_PART('year', date) as y, DATE_PART('month', date) as m, date, from_iso_code, to_iso_code, rate FROM ( " +
        "  SELECT *, " +
        "    ROW_NUMBER() OVER (PARTITION BY from_iso_code, to_iso_code, DATE_PART('month', x.date) + DATE_PART('year', x.date) ORDER BY x.date DESC) rn " +
        " FROM fin_fx AS x) xx " +
        " WHERE rn = 1 and from_iso_code=:from_iso_code and to_iso_code=:to_iso_code and date BETWEEN :from_date AND :to_date" +
        " ORDER BY date ASC;",
        nativeQuery = true
    )
    List<FinanceFX> monthlyFXAsending(
        @Param("from_iso_code") String fromIsoCode,
        @Param("to_iso_code") String toIsoCode,
        @Param("from_date") LocalDate fromDate,
        @Param("to_date") LocalDate toDate
    ); //
}
