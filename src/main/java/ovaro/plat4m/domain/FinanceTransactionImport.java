package ovaro.plat4m.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "fin_transaction_import")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceTransactionImport implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String userGuid;

    private String accountId;

    private String accountName;

    private String sourceLabel;

    @Column(length = 100000)
    private String rawContent;

    @Enumerated(EnumType.STRING)
    private FinanceTransactionImportStatus status;

    private Integer totalRows;

    private Integer flaggedRows;

    private String correlationStatus;

    @Column(length = 2000)
    private String correlationMessage;

    private BigDecimal expectedEndingBalance;

    private ZonedDateTime createdDateTime;

    private ZonedDateTime importedDateTime;

    private ZonedDateTime backedOutDateTime;

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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public FinanceTransactionImportStatus getStatus() {
        return status;
    }

    public void setStatus(FinanceTransactionImportStatus status) {
        this.status = status;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getFlaggedRows() {
        return flaggedRows;
    }

    public void setFlaggedRows(Integer flaggedRows) {
        this.flaggedRows = flaggedRows;
    }

    public String getCorrelationStatus() {
        return correlationStatus;
    }

    public void setCorrelationStatus(String correlationStatus) {
        this.correlationStatus = correlationStatus;
    }

    public String getCorrelationMessage() {
        return correlationMessage;
    }

    public void setCorrelationMessage(String correlationMessage) {
        this.correlationMessage = correlationMessage;
    }

    public BigDecimal getExpectedEndingBalance() {
        return expectedEndingBalance;
    }

    public void setExpectedEndingBalance(BigDecimal expectedEndingBalance) {
        this.expectedEndingBalance = expectedEndingBalance;
    }

    public ZonedDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(ZonedDateTime createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public ZonedDateTime getImportedDateTime() {
        return importedDateTime;
    }

    public void setImportedDateTime(ZonedDateTime importedDateTime) {
        this.importedDateTime = importedDateTime;
    }

    public ZonedDateTime getBackedOutDateTime() {
        return backedOutDateTime;
    }

    public void setBackedOutDateTime(ZonedDateTime backedOutDateTime) {
        this.backedOutDateTime = backedOutDateTime;
    }
}
