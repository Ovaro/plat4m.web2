package ovaro.plat4m.service.dto;

import java.util.ArrayList;
import java.util.List;

public class FinanceDemoDataRequestDTO {

    private String persona;
    private String currencyCode;
    private String hints;
    private Boolean domesticStocks;
    private Boolean internationalStocks;
    private Boolean realEstateInvestor;
    private Boolean homeOwner;
    private Boolean renter;
    private Boolean privateSchool;
    private Boolean noInvestments;
    private Boolean useAiHints;
    private Integer spendingRatePercent;
    private List<FinanceDemoDataAccountSpecDTO> accounts = new ArrayList<>();

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getHints() {
        return hints;
    }

    public void setHints(String hints) {
        this.hints = hints;
    }

    public Boolean getDomesticStocks() {
        return domesticStocks;
    }

    public void setDomesticStocks(Boolean domesticStocks) {
        this.domesticStocks = domesticStocks;
    }

    public Boolean getInternationalStocks() {
        return internationalStocks;
    }

    public void setInternationalStocks(Boolean internationalStocks) {
        this.internationalStocks = internationalStocks;
    }

    public Boolean getRealEstateInvestor() {
        return realEstateInvestor;
    }

    public void setRealEstateInvestor(Boolean realEstateInvestor) {
        this.realEstateInvestor = realEstateInvestor;
    }

    public Boolean getHomeOwner() {
        return homeOwner;
    }

    public void setHomeOwner(Boolean homeOwner) {
        this.homeOwner = homeOwner;
    }

    public Boolean getRenter() {
        return renter;
    }

    public void setRenter(Boolean renter) {
        this.renter = renter;
    }

    public Boolean getPrivateSchool() {
        return privateSchool;
    }

    public void setPrivateSchool(Boolean privateSchool) {
        this.privateSchool = privateSchool;
    }

    public Boolean getNoInvestments() {
        return noInvestments;
    }

    public void setNoInvestments(Boolean noInvestments) {
        this.noInvestments = noInvestments;
    }

    public Boolean getUseAiHints() {
        return useAiHints;
    }

    public void setUseAiHints(Boolean useAiHints) {
        this.useAiHints = useAiHints;
    }

    public Integer getSpendingRatePercent() {
        return spendingRatePercent;
    }

    public void setSpendingRatePercent(Integer spendingRatePercent) {
        this.spendingRatePercent = spendingRatePercent;
    }

    public List<FinanceDemoDataAccountSpecDTO> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<FinanceDemoDataAccountSpecDTO> accounts) {
        this.accounts = accounts;
    }
}
