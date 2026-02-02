package ovaro.plat4m.service.dto;

public class FinanceResourceDTO {

    private String id;
    private String name;
    private String symbol; // symbol
    private String currencyCode; // currencyCode
    private String type;
    private String indicatorSource;
    private String historySource;

    public FinanceResourceDTO() {}

    public FinanceResourceDTO(String resourceId, String resourceName) {
        this.id = resourceId;
        this.name = resourceName;
    }

    public FinanceResourceDTO(String resourceId, String resourceName, String resourceSymbol, String resourceCurrencyCode, String type) {
        this.id = resourceId;
        this.name = resourceName;
        this.symbol = resourceSymbol;
        this.currencyCode = resourceCurrencyCode;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIndicatorSource() {
        return indicatorSource;
    }

    public void setIndicatorSource(String indicatorSource) {
        this.indicatorSource = indicatorSource;
    }

    public String getHistorySource() {
        return historySource;
    }

    public void setHistorySource(String historySource) {
        this.historySource = historySource;
    }
}
