package ovaro.plat4m.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

//"symbol":"<em>ANZ</em>","description":"AUSTRALIA AND NEW ZEALAND BANKING GROUP LIMITED","type":"stock","exchange":"ASX","currency_code":"AUD","logoid":"australia-and-new-zealand-banking","provider_id":"ice","country":"AU","typespecs":["common"]}
//{"symbol":"<em>MSFT</em>","description":"Microsoft Corporation","type":"stock","exchange":"NASDAQ","currency_code":"USD","logoid":"microsoft","provider_id":"ice","country":"US","typespecs":["common"]}
//{"symbol":"<em>IBM</em>","description":"International Business Machines Corporation","type":"stock","exchange":"NYSE","currency_code":"USD","logoid":"international-bus-mach","provider_id":"ice","country":"US","typespecs":["common"]}

@Entity
@Table(name = "fin_security_details")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceSecurityDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(unique = true)
    private String symbol;

    private String name;
    private String shortName;
    private String exchangeName;
    private String exchangeMIC;
    private String country;
    private String sector;
    private String industry;
    private String capitalizationClass;
    private String website;
    private String employees;

    private Double sharesOutstanding;
    private Double pcHeldInsiders;
    private Double pcHeldInstitutions;

    private Double fiveYearDividendRate;
    private Double forwardAnnualDividendRate;
    private Double forwardAnnualDividendYield;
    private Double trailingAnnualDividendRate;
    private Double trailingAnnualDividendYield;

    private Double profitMargin;
    private Double grossProfit;
    private Double ebita;
    private Double dilutedEPS;
    private Double totalCash;
    private Double totalDebt;
    private Double operatingCashFlow;
    private Double leveredFreeCashFlow;

    private ZonedDateTime exDividendDate;

    private ZonedDateTime lastUpdatedDateTime;
}
