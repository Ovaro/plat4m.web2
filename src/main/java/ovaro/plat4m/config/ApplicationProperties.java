package ovaro.plat4m.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Plat 4 M.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();
    private final MarketData marketData = new MarketData();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public MarketData getMarketData() {
        return marketData;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    // jhipster-needle-application-properties-property-class

    public static class MarketData {

        private boolean enabled = true;
        private final TwelveData twelveData = new TwelveData();
        private final YahooFinance yahooFinance = new YahooFinance();
        private final QuoteRefresh quoteRefresh = new QuoteRefresh();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public TwelveData getTwelveData() {
            return twelveData;
        }

        public YahooFinance getYahooFinance() {
            return yahooFinance;
        }

        public QuoteRefresh getQuoteRefresh() {
            return quoteRefresh;
        }
    }

    public static class TwelveData {

        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class YahooFinance {

        private String cookie;

        public String getCookie() {
            return cookie;
        }

        public void setCookie(String cookie) {
            this.cookie = cookie;
        }
    }

    public static class QuoteRefresh {

        private String cron = "0 0 8 * * MON-FRI";
        private String zone = "Australia/Brisbane";
        private long requestDelayMs = 250;

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public long getRequestDelayMs() {
            return requestDelayMs;
        }

        public void setRequestDelayMs(long requestDelayMs) {
            this.requestDelayMs = requestDelayMs;
        }
    }

    private String rapidapiKey;

    public String getRapidapiKey() {
        return rapidapiKey;
    }

    public void setRapidapiKey(String rapidapiKey) {
        this.rapidapiKey = rapidapiKey;
    }
}
