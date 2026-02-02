package ovaro.plat4m.domain;

import jakarta.persistence.CascadeType;
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
@Table(name = "fin_security")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceSecurity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true)
    private String symbol;

    private String name;
    private String shortName;
    private String exchangeName;

    @Column(name = "exchange_mic")
    private String exchangeMic;

    @Column(name = "exchange_suffix")
    private String exchangeSuffix;

    private String country;
    private String sector;
    private String industry;
    private String capitalizationClass;
    private boolean active = true;

    private ZonedDateTime serialDateTime;

    // @OneToOne(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
    // @JoinColumn(name = "currency_code", referencedColumnName = "isoCode")
    // @Fetch(FetchMode.JOIN)
    // private FinanceCurrency currency;
    private String currencyCode;

    private Integer type;

    public FinanceSecurity() {}

    public FinanceSecurity(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getExchangeMic() {
        return exchangeMic;
    }

    public void setExchangeMic(String exchangeMic) {
        this.exchangeMic = exchangeMic;
    }

    public String getExchangeSuffix() {
        return exchangeSuffix;
    }

    public void setExchangeSuffix(String exchangeSuffix) {
        this.exchangeSuffix = exchangeSuffix;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCapitalizationClass() {
        return capitalizationClass;
    }

    public void setCapitalizationClass(String capitalizationClass) {
        this.capitalizationClass = capitalizationClass;
    }

    public ZonedDateTime getSerialDateTime() {
        return serialDateTime;
    }

    public void setSerialDateTime(ZonedDateTime serialDateTime) {
        this.serialDateTime = serialDateTime;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return (
            "FinanceSecurity [capitalizationClass=" +
            capitalizationClass +
            ", country=" +
            country +
            ", currency=" +
            currencyCode +
            ", exchangeMic=" +
            exchangeMic +
            ", exchangeName=" +
            exchangeName +
            ", exchangeSuffix=" +
            exchangeSuffix +
            ", id=" +
            id +
            ", industry=" +
            industry +
            ", name=" +
            name +
            ", sector=" +
            sector +
            ", serialDateTime=" +
            serialDateTime +
            ", shortName=" +
            shortName +
            ", symbol=" +
            symbol +
            ", type=" +
            type +
            "]"
        );
    }
}
