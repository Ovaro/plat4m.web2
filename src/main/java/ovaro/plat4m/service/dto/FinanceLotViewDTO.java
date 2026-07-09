package ovaro.plat4m.service.dto;

import java.time.LocalDate;

public class FinanceLotViewDTO {

    private String id;
    private Double quantity;
    private Integer lotType;
    private String accountId;
    private String accountName;
    private String securityId;
    private String securityName;
    private LocalDate buyDate;
    private LocalDate sellDate;
    private LocalDate openDate;
    private LocalDate closeDate;
    private String buyTransactionId;
    private String sellTransactionId;
    private String openTransactionId;
    private String closeTransactionId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Integer getLotType() {
        return lotType;
    }

    public void setLotType(Integer lotType) {
        this.lotType = lotType;
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

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    public String getSecurityName() {
        return securityName;
    }

    public void setSecurityName(String securityName) {
        this.securityName = securityName;
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

    public String getBuyTransactionId() {
        return buyTransactionId;
    }

    public void setBuyTransactionId(String buyTransactionId) {
        this.buyTransactionId = buyTransactionId;
    }

    public String getSellTransactionId() {
        return sellTransactionId;
    }

    public void setSellTransactionId(String sellTransactionId) {
        this.sellTransactionId = sellTransactionId;
    }

    public String getOpenTransactionId() {
        return openTransactionId;
    }

    public void setOpenTransactionId(String openTransactionId) {
        this.openTransactionId = openTransactionId;
    }

    public String getCloseTransactionId() {
        return closeTransactionId;
    }

    public void setCloseTransactionId(String closeTransactionId) {
        this.closeTransactionId = closeTransactionId;
    }
}
