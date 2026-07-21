package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinanceLotViewDTO {

    private String id;
    private Integer sourceId;
    private String lotKey;
    private String originalLotId;
    private Integer originalSourceId;
    private LocalDate originalBuyDate;
    private Double originalQuantity;
    private Double originalPrice;
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
    private Double buyPrice;
    private Double sellPrice;
    private Double buyFxRate;
    private Double sellFxRate;
    private String buyCurrencyCode;
    private String sellCurrencyCode;
    private BigDecimal buyCharges;
    private BigDecimal sellCharges;
    private String buyTransactionId;
    private String sellTransactionId;
    private String openTransactionId;
    private String closeTransactionId;
    private BigDecimal costBasis;
    private BigDecimal saleProceeds;
    private BigDecimal realisedGainLoss;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public String getLotKey() {
        return lotKey;
    }

    public void setLotKey(String lotKey) {
        this.lotKey = lotKey;
    }

    public String getOriginalLotId() {
        return originalLotId;
    }

    public void setOriginalLotId(String originalLotId) {
        this.originalLotId = originalLotId;
    }

    public Integer getOriginalSourceId() {
        return originalSourceId;
    }

    public void setOriginalSourceId(Integer originalSourceId) {
        this.originalSourceId = originalSourceId;
    }

    public LocalDate getOriginalBuyDate() {
        return originalBuyDate;
    }

    public void setOriginalBuyDate(LocalDate originalBuyDate) {
        this.originalBuyDate = originalBuyDate;
    }

    public Double getOriginalQuantity() {
        return originalQuantity;
    }

    public void setOriginalQuantity(Double originalQuantity) {
        this.originalQuantity = originalQuantity;
    }

    public Double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Double originalPrice) {
        this.originalPrice = originalPrice;
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

    public Double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(Double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public Double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(Double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public Double getBuyFxRate() {
        return buyFxRate;
    }

    public void setBuyFxRate(Double buyFxRate) {
        this.buyFxRate = buyFxRate;
    }

    public Double getSellFxRate() {
        return sellFxRate;
    }

    public void setSellFxRate(Double sellFxRate) {
        this.sellFxRate = sellFxRate;
    }

    public String getBuyCurrencyCode() {
        return buyCurrencyCode;
    }

    public void setBuyCurrencyCode(String buyCurrencyCode) {
        this.buyCurrencyCode = buyCurrencyCode;
    }

    public String getSellCurrencyCode() {
        return sellCurrencyCode;
    }

    public void setSellCurrencyCode(String sellCurrencyCode) {
        this.sellCurrencyCode = sellCurrencyCode;
    }

    public BigDecimal getBuyCharges() {
        return buyCharges;
    }

    public void setBuyCharges(BigDecimal buyCharges) {
        this.buyCharges = buyCharges;
    }

    public BigDecimal getSellCharges() {
        return sellCharges;
    }

    public void setSellCharges(BigDecimal sellCharges) {
        this.sellCharges = sellCharges;
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

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public BigDecimal getSaleProceeds() {
        return saleProceeds;
    }

    public void setSaleProceeds(BigDecimal saleProceeds) {
        this.saleProceeds = saleProceeds;
    }

    public BigDecimal getRealisedGainLoss() {
        return realisedGainLoss;
    }

    public void setRealisedGainLoss(BigDecimal realisedGainLoss) {
        this.realisedGainLoss = realisedGainLoss;
    }
}
