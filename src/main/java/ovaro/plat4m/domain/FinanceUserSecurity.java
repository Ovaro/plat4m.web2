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
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "fin_user_security")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceUserSecurity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String userGuid;

    private String name;

    private String symbol;

    private String currencyCode;

    // Whether the FinanceInvestmentEvents have been calculated or indicate whether they need to be recalculated.
    private boolean eventsValid = false;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "known_symbol", referencedColumnName = "symbol")
    @Fetch(FetchMode.JOIN)
    private FinanceSecurity security;

    @Column(unique = true)
    private String masterGuid;

    private ZonedDateTime serialDateTime;

    private String comment;

    private boolean ignoredForRollup = false;

    @Column(length = 2000)
    private String ignoredForRollupReason;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    @Fetch(FetchMode.JOIN)
    private FinanceUserSecurity linked;

    private Integer type;

    public FinanceUserSecurity() {}

    public FinanceUserSecurity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserGuid() {
        return userGuid;
    }

    public void setUserGuid(String userGuid) {
        this.userGuid = userGuid;
    }

    public String getMasterGuid() {
        return masterGuid;
    }

    public void setMasterGuid(String masterGuid) {
        this.masterGuid = masterGuid;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public ZonedDateTime getSerialDateTime() {
        return serialDateTime;
    }

    public void setSerialDateTime(ZonedDateTime serialDateTime) {
        this.serialDateTime = serialDateTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isIgnoredForRollup() {
        return ignoredForRollup;
    }

    public void setIgnoredForRollup(boolean ignoredForRollup) {
        this.ignoredForRollup = ignoredForRollup;
    }

    public String getIgnoredForRollupReason() {
        return ignoredForRollupReason;
    }

    public void setIgnoredForRollupReason(String ignoredForRollupReason) {
        this.ignoredForRollupReason = ignoredForRollupReason;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public FinanceUserSecurity getLinked() {
        return linked;
    }

    public void setLinked(FinanceUserSecurity linked) {
        this.linked = linked;
    }

    public FinanceSecurity getSecurity() {
        return security;
    }

    public void setSecurity(FinanceSecurity security) {
        this.security = security;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public boolean isEventsValid() {
        return eventsValid;
    }

    public void setEventsValid(boolean eventsValid) {
        this.eventsValid = eventsValid;
    }
}
