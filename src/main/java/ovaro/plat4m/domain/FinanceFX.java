package ovaro.plat4m.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "fin_fx", uniqueConstraints = @UniqueConstraint(columnNames = { "date", "from_iso_code", "to_iso_code" }))
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceFX implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    ZonedDateTime date;

    @Column(name = "from_iso_code")
    String fromIsoCode;

    @Column(name = "to_iso_code")
    String toIsoCode;

    Double rate;

    @Transient
    @JsonProperty("favourite")
    private boolean favourite;

    public FinanceFX() {}

    public UUID getId() {
        return id;
    }

    public FinanceFX(ZonedDateTime date, String fromISOCode, String toISOCode, Double rate) {
        this.date = date;
        this.fromIsoCode = fromISOCode;
        this.toIsoCode = toISOCode;
        this.rate = rate;
    }

    public String getFromIsoCode() {
        return fromIsoCode;
    }

    public void setFromIsoCode(String fromIsoCode) {
        this.fromIsoCode = fromIsoCode;
    }

    public String getToIsoCode() {
        return toIsoCode;
    }

    public void setToIsoCode(String toIsoCode) {
        this.toIsoCode = toIsoCode;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    @JsonProperty("favourite")
    public boolean isFavourite() {
        return favourite;
    }

    @JsonProperty("favourite")
    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }
}
