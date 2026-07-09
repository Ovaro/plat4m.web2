package ovaro.plat4m.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "fin_lot")
public class FinanceLot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_guid", nullable = false)
    private String userGuid;

    @Column(name = "source_id")
    private Integer sourceId;

    @Column(name = "buy_transaction_id")
    private UUID buyTransactionId;

    @Column(name = "sell_transaction_id")
    private UUID sellTransactionId;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "security_id")
    private String securityId;

    @Column(name = "buy_date")
    private LocalDate buyDate;

    @Column(name = "sell_date")
    private LocalDate sellDate;

    @Column(name = "lot_open_id")
    private UUID lotOpenId;

    @Column(name = "lot_type")
    private Integer lotType;

    @Column(name = "open_transaction_id")
    private UUID openTransactionId;

    @Column(name = "close_transaction_id")
    private UUID closeTransactionId;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "master_guid", unique = true)
    private String masterGuid;

    @Column(name = "serial_date_time")
    private ZonedDateTime serialDateTime;

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

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public UUID getBuyTransactionId() {
        return buyTransactionId;
    }

    public void setBuyTransactionId(UUID buyTransactionId) {
        this.buyTransactionId = buyTransactionId;
    }

    public UUID getSellTransactionId() {
        return sellTransactionId;
    }

    public void setSellTransactionId(UUID sellTransactionId) {
        this.sellTransactionId = sellTransactionId;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    public LocalDate getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(LocalDate buyDate) {
        this.buyDate = buyDate;
    }

    public LocalDate getSellDate() {
        return sellDate;
    }

    public void setSellDate(LocalDate sellDate) {
        this.sellDate = sellDate;
    }

    public UUID getLotOpenId() {
        return lotOpenId;
    }

    public void setLotOpenId(UUID lotOpenId) {
        this.lotOpenId = lotOpenId;
    }

    public Integer getLotType() {
        return lotType;
    }

    public void setLotType(Integer lotType) {
        this.lotType = lotType;
    }

    public UUID getOpenTransactionId() {
        return openTransactionId;
    }

    public void setOpenTransactionId(UUID openTransactionId) {
        this.openTransactionId = openTransactionId;
    }

    public UUID getCloseTransactionId() {
        return closeTransactionId;
    }

    public void setCloseTransactionId(UUID closeTransactionId) {
        this.closeTransactionId = closeTransactionId;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
    }

    public String getMasterGuid() {
        return masterGuid;
    }

    public void setMasterGuid(String masterGuid) {
        this.masterGuid = masterGuid;
    }

    public ZonedDateTime getSerialDateTime() {
        return serialDateTime;
    }

    public void setSerialDateTime(ZonedDateTime serialDateTime) {
        this.serialDateTime = serialDateTime;
    }
}
