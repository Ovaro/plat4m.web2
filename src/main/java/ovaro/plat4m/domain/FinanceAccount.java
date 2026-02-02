package ovaro.plat4m.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "fin_account")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    private String userGuid;

    private String name;
    private Integer type;

    // @OneToOne(fetch = FetchType.EAGER)
    // @JoinColumn(name = "currency_code", referencedColumnName = "isoCode")
    // @Fetch(FetchMode.JOIN)
    // private FinanceCurrency currency;
    private String currencyCode;

    // private String currencyId;
    // private String currencyCode;
    private BigDecimal startingBalance;
    //private BigDecimal currentBalance;
    private Integer relatedToAccountSrcId;
    private String relatedToAccountId;
    private boolean closed;
    private boolean retirement;
    private Integer investmentSubType;
    private BigDecimal amountLimit;
    private boolean credit;
    private boolean is401k403b;
    private LocalDate dateOpened;
    private LocalDate dateClosed;
    private Double interestRate;
    private LocalDate dateInterestRateChanged;
    private ZonedDateTime serialDateTime;
    private Integer maxPayments;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id", referencedColumnName = "id")
    @Fetch(FetchMode.JOIN)
    private FinanceInstitution institution;

    public FinanceAccount() {}

    public FinanceAccount(FinanceAccount toCopy) {
        this.id = toCopy.getId();
        this.name = toCopy.getName();
        this.type = toCopy.getType();
        // this.currencyId = toCopy.getCurrencyId();
        // this.currencyCode = toCopy.getCurrencyCode();
        this.startingBalance = toCopy.getStartingBalance();
        //this.currentBalance = toCopy.getCurrentBalance();
        this.relatedToAccountId = toCopy.getRelatedToAccountId();
        this.closed = toCopy.isClosed();
        this.retirement = toCopy.isRetirement();
        this.investmentSubType = toCopy.getInvestmentSubType();
        this.amountLimit = toCopy.getAmountLimit();
        this.credit = toCopy.isCredit();
        this.is401k403b = toCopy.is401k403b;
        this.dateOpened = toCopy.getDateOpened();
        this.dateClosed = toCopy.getDateClosed();
        this.interestRate = toCopy.getInterestRate();
        this.dateInterestRateChanged = toCopy.getDateInterestRateChanged();
        this.serialDateTime = toCopy.getSerialDateTime();
        this.maxPayments = toCopy.getMaxPayments();
        this.currencyCode = toCopy.getCurrencyCode();
        this.userGuid = toCopy.getUserGuid();
        this.institution = toCopy.getInstitution();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    // public String getCurrencyId() {
    //     return currencyId;
    // }

    // public void setCurrencyId(String currencyId) {
    //     this.currencyId = currencyId;
    // }

    // public String getCurrencyCode() {
    //     return currencyCode;
    // }

    // public void setCurrencyCode(String currencyCode) {
    //     this.currencyCode = currencyCode;
    // }

    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public void setStartingBalance(BigDecimal startingBalance) {
        this.startingBalance = startingBalance;
    }

    // public BigDecimal getCurrentBalance() {
    //     return currentBalance;
    // }

    // public void setCurrentBalance(BigDecimal currentBalance) {
    //     this.currentBalance = currentBalance;
    // }

    public boolean isClosed() {
        return closed;
    }

    public Integer getRelatedToAccountSrcId() {
        return relatedToAccountSrcId;
    }

    public void setRelatedToAccountSrcId(Integer relatedToAccountSrcId) {
        this.relatedToAccountSrcId = relatedToAccountSrcId;
    }

    public String getRelatedToAccountId() {
        return relatedToAccountId;
    }

    public void setRelatedToAccountId(String relatedToAccountId) {
        this.relatedToAccountId = relatedToAccountId;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isRetirement() {
        return retirement;
    }

    public void setRetirement(boolean retirement) {
        this.retirement = retirement;
    }

    public Integer getInvestmentSubType() {
        return investmentSubType;
    }

    public void setInvestmentSubType(Integer investmentSubType) {
        this.investmentSubType = investmentSubType;
    }

    public BigDecimal getAmountLimit() {
        return amountLimit;
    }

    public void setAmountLimit(BigDecimal amountLimit) {
        this.amountLimit = amountLimit;
    }

    public boolean isCredit() {
        return credit;
    }

    public void setCredit(boolean credit) {
        this.credit = credit;
    }

    public boolean isIs401k403b() {
        return is401k403b;
    }

    public void setIs401k403b(boolean is401k403b) {
        this.is401k403b = is401k403b;
    }

    public LocalDate getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(LocalDate dateOpened) {
        this.dateOpened = dateOpened;
    }

    public LocalDate getDateClosed() {
        return dateClosed;
    }

    public void setDateClosed(LocalDate dateClosed) {
        this.dateClosed = dateClosed;
    }

    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(Double interestRate) {
        this.interestRate = interestRate;
    }

    public LocalDate getDateInterestRateChanged() {
        return dateInterestRateChanged;
    }

    public void setDateInterestRateChanged(LocalDate dateInterestRateChanged) {
        this.dateInterestRateChanged = dateInterestRateChanged;
    }

    public ZonedDateTime getSerialDateTime() {
        return serialDateTime;
    }

    public void setSerialDateTime(ZonedDateTime serialDateTime) {
        this.serialDateTime = serialDateTime;
    }

    public Integer getMaxPayments() {
        return maxPayments;
    }

    public void setMaxPayments(Integer maxPayments) {
        this.maxPayments = maxPayments;
    }

    public FinanceInstitution getInstitution() {
        return institution;
    }

    public void setInstitution(FinanceInstitution institution) {
        this.institution = institution;
    }
}
