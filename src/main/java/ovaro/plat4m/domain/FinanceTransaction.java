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
@Table(name = "fin_transaction")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    private String userGuid;

    private Integer number;
    private String accountId;
    private BigDecimal amount;
    private Integer statusFlag;
    private LocalDate date;
    private boolean recurring;

    //@Transient
    //private  BigDecimal runningBalance;
    //private  Integer categoryId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    @Fetch(FetchMode.JOIN)
    private FinanceCategory category;

    private Integer sourcePayeeId;
    private String payeeId;
    private String transferredAccountId;
    //private  Frequency getFrequency;
    //private  TransactionInfo getTransactionInfo;

    // private  InvestmentActivityImpl getInvestmentActivity;
    //private  InvestmentTransaction getInvestmentTransaction;
    private boolean transfer;
    private boolean transferTo;

    private boolean splitParent;
    private boolean splitChild;
    private UUID parentId;

    private boolean voided;
    private String statementId;
    private boolean cleared; // Cleared or Reconciled
    private boolean reconciled; // reconciled
    private String memo;

    //    private  TransactionState getState;
    private String payeeName;

    /* Investment Transaction Details */
    private boolean investment;
    private String securityId;
    private Integer investmentActivityType;
    private Double quantity;
    private Double price;
    private LocalDate dividendExDate;
    private BigDecimal dividendFrankedAmount;
    private BigDecimal dividendUnfrankedAmount;
    private BigDecimal dividendTFNWithholdingAmount;
    private BigDecimal dividendFrankingCredits;

    private String currencyCode;
    private BigDecimal amountBase;
    private Double rateToBase;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /* Meta-data */
    private ZonedDateTime serialDateTime;

    @Column(unique = true)
    private String masterGuid;

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getStatusFlag() {
        return statusFlag;
    }

    public void setStatusFlag(Integer statusFlag) {
        this.statusFlag = statusFlag;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    // public BigDecimal getRunningBalance() {
    //     return runningBalance;
    // }

    // public void setRunningBalance(BigDecimal runningBalance) {
    //     this.runningBalance = runningBalance;
    // }

    public Integer getSourcePayeeId() {
        return sourcePayeeId;
    }

    public FinanceCategory getCategory() {
        return category;
    }

    public void setCategory(FinanceCategory category) {
        this.category = category;
    }

    public void setSourcePayeeId(Integer payeeId) {
        this.sourcePayeeId = payeeId;
    }

    public String getTransferredAccountId() {
        return transferredAccountId;
    }

    public void setTransferredAccountId(String transferredAccountId) {
        this.transferredAccountId = transferredAccountId;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    public boolean isInvestment() {
        return investment;
    }

    public void setInvestment(boolean investment) {
        this.investment = investment;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public boolean isTransfer() {
        return transfer;
    }

    public void setTransfer(boolean transfer) {
        this.transfer = transfer;
    }

    public boolean isCleared() {
        return cleared;
    }

    public void setCleared(boolean cleared) {
        this.cleared = cleared;
    }

    public boolean isReconciled() {
        return reconciled;
    }

    public void setReconciled(boolean reconciled) {
        this.reconciled = reconciled;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getMasterGuid() {
        return masterGuid;
    }

    public void setMasterGuid(String masterGuid) {
        this.masterGuid = masterGuid;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public boolean isTransferTo() {
        return transferTo;
    }

    public void setTransferTo(boolean transferTo) {
        this.transferTo = transferTo;
    }

    public boolean isSplitParent() {
        return splitParent;
    }

    public void setSplitParent(boolean splitParent) {
        this.splitParent = splitParent;
    }

    public boolean isSplitChild() {
        return splitChild;
    }

    public void setSplitChild(boolean splitChild) {
        this.splitChild = splitChild;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public FinanceInvestmentActivityType getInvestmentActivityType() {
        if (investmentActivityType != null) {
            return FinanceInvestmentActivityType.valueOf(investmentActivityType);
        } else {
            return FinanceInvestmentActivityType.INVALID;
        }
    }

    public void setInvestmentActivityType(Integer investmentActivityType) {
        this.investmentActivityType = investmentActivityType;
    }

    public void setInvestmentActivityType(FinanceInvestmentActivityType investmentActivityType) {
        if (investmentActivityType != null) {
            this.investmentActivityType = investmentActivityType.value;
        } else {
            this.investmentActivityType = FinanceInvestmentActivityType.INVALID.value;
        }
    }

    public String getPayeeId() {
        return payeeId;
    }

    public void setPayeeId(String payeeId) {
        this.payeeId = payeeId;
    }

    public ZonedDateTime getSerialDateTime() {
        return serialDateTime;
    }

    public void setSerialDateTime(ZonedDateTime serialDateTime) {
        this.serialDateTime = serialDateTime;
    }

    public LocalDate getDividendExDate() {
        return dividendExDate;
    }

    public void setDividendExDate(LocalDate dividendExDate) {
        this.dividendExDate = dividendExDate;
    }

    public BigDecimal getDividendFrankedAmount() {
        return dividendFrankedAmount;
    }

    public void setDividendFrankedAmount(BigDecimal dividendFrankedAmount) {
        this.dividendFrankedAmount = dividendFrankedAmount;
    }

    public BigDecimal getDividendUnfrankedAmount() {
        return dividendUnfrankedAmount;
    }

    public void setDividendUnfrankedAmount(BigDecimal dividendUnfrankedAmount) {
        this.dividendUnfrankedAmount = dividendUnfrankedAmount;
    }

    public BigDecimal getDividendTFNWithholdingAmount() {
        return dividendTFNWithholdingAmount;
    }

    public void setDividendTFNWithholdingAmount(BigDecimal dividendTFNWithholdingAmount) {
        this.dividendTFNWithholdingAmount = dividendTFNWithholdingAmount;
    }

    public BigDecimal getDividendFrankingCredits() {
        return dividendFrankingCredits;
    }

    public void setDividendFrankingCredits(BigDecimal dividendFrankingCredits) {
        this.dividendFrankingCredits = dividendFrankingCredits;
    }

    public BigDecimal getAmountBase() {
        return amountBase;
    }

    public void setAmountBase(BigDecimal amountBase) {
        this.amountBase = amountBase;
    }

    public Double getRateToBase() {
        return rateToBase;
    }

    public void setRateToBase(Double rateToBase) {
        this.rateToBase = rateToBase;
    }
}
