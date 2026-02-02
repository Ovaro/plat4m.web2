package ovaro.plat4m.service.idto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ovaro.plat4m.domain.User;

/**
 * A DTO representing a user, with only the public attributes.
 */
public class TwelveDataStockDataIDTO {

    private String symbol;
    private String name;
    private String currency;
    private String exchange;

    @JsonProperty("mic_code")
    private String micCode;

    private String country;
    private String type;

    public TwelveDataStockDataIDTO() {
        // Empty constructor needed for Jackson.
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getMicCode() {
        return micCode;
    }

    public void setMicCode(String mic_code) {
        this.micCode = mic_code;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return (
            "TwelveDataStockDataIDTO [country=" +
            country +
            ", currency=" +
            currency +
            ", exchange=" +
            exchange +
            ", micCode=" +
            micCode +
            ", name=" +
            name +
            ", symbol=" +
            symbol +
            ", type=" +
            type +
            "]"
        );
    }
}
