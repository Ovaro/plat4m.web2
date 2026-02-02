package ovaro.plat4m.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "fin_inv_event")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceInvestmentEvent implements Serializable {

    public static final MathContext MC = new MathContext(8, RoundingMode.HALF_UP);
    public static final int SCALE = 5;

    public static BigDecimal setScale(BigDecimal in) {
        in.setScale(SCALE, RoundingMode.HALF_UP);
        return in;
    }

    private static final long serialVersionUID = 1L;

    public FinanceInvestmentEvent() {}

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private LocalDate date;

    private String userSecurityId;

    private String userGuid;

    private BigDecimal realisedGain;

    @Column(precision = 19, scale = 5)
    private BigDecimal realisedCurrencyGain;

    @Column(precision = 19, scale = 5)
    private BigDecimal baseCurrencyRealisedGain;

    private String currencyCode;

    private BigDecimal feeDelta;

    private BigDecimal capitalDelta;

    @Column(precision = 19, scale = 5)
    private BigDecimal baseCurrencyCapitalDelta;

    private Double quantity;

    @Column(precision = 19, scale = 3)
    private BigDecimal price;

    private Double holding;

    private BigDecimal income;

    private BigDecimal fee;

    private Double rateToBase;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getUserSecurityId() {
        return userSecurityId;
    }

    public void setUserSecurityId(String userSecurityId) {
        this.userSecurityId = userSecurityId;
    }

    public BigDecimal getRealisedGain() {
        return realisedGain;
    }

    public void setRealisedGain(BigDecimal realisedGain) {
        this.realisedGain = realisedGain;
    }

    public void setRealisedGain(double realisedGain) {
        this.realisedGain = new BigDecimal(realisedGain, MC).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal getCapitalDelta() {
        return capitalDelta;
    }

    public void setCapitalDelta(BigDecimal capitalDelta) {
        this.capitalDelta = capitalDelta;
    }

    public void setCapitalDelta(double capitalDelta) {
        this.capitalDelta = new BigDecimal(capitalDelta, MC).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getHolding() {
        return holding;
    }

    public void setHolding(Double holding) {
        this.holding = holding;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public void setFee(double fee) {
        this.fee = new BigDecimal(fee, MC);
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public void setIncome(double income) {
        this.income = new BigDecimal(income, MC);
    }

    public BigDecimal getRealisedCurrencyGain() {
        return realisedCurrencyGain;
    }

    public void setRealisedCurrencyGain(BigDecimal realisedCurrencyGain) {
        this.realisedCurrencyGain = realisedCurrencyGain;
    }

    public void setRealisedCurrencyGain(double realisedCurrencyGain) {
        this.realisedCurrencyGain = new BigDecimal(realisedCurrencyGain, MC).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal getBaseCurrencyCapitalDelta() {
        return baseCurrencyCapitalDelta;
    }

    public void setBaseCurrencyCapitalDelta(BigDecimal baseCurrencyCapitalDelta) {
        this.baseCurrencyCapitalDelta = baseCurrencyCapitalDelta;
    }

    public BigDecimal getBaseCurrencyRealisedGain() {
        return baseCurrencyRealisedGain;
    }

    public void setBaseCurrencyRealisedGain(BigDecimal baseCurrencyRealisedGain) {
        this.baseCurrencyRealisedGain = baseCurrencyRealisedGain;
    }

    @Override
    public String toString() {
        return (
            "FinanceInvestmentEvent [baseCurrencyCapitalDelta=" +
            baseCurrencyCapitalDelta +
            ", capitalDelta=" +
            capitalDelta +
            ", date=" +
            date +
            ", fee=" +
            fee +
            ", holding=" +
            holding +
            ", id=" +
            id +
            ", income=" +
            income +
            ", quantity=" +
            quantity +
            ", realisedCurrencyGain=" +
            realisedCurrencyGain +
            ", realisedGain=" +
            realisedGain +
            ", userSecurityId=" +
            userSecurityId +
            "]"
        );
    }

    public BigDecimal getFeeDelta() {
        return feeDelta;
    }

    public void setFeeDelta(BigDecimal feeDelta) {
        this.feeDelta = feeDelta;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getUserGuid() {
        return userGuid;
    }

    public void setUserGuid(String userGuid) {
        this.userGuid = userGuid;
    }

    public Double getRateToBase() {
        return rateToBase;
    }

    public void setRateToBase(Double rateToBase) {
        this.rateToBase = rateToBase;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
