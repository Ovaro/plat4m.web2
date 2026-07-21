package ovaro.plat4m.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ovaro.plat4m.config.ApplicationProperties;
import ovaro.plat4m.domain.FinanceSecurity;

@Service
public class FinanceMarketDataService {

    private static final Logger LOG = LoggerFactory.getLogger(FinanceMarketDataService.class);
    private static final CookieManager COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .cookieHandler(COOKIE_MANAGER)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private static final String ASX_MARKET_DATA_HEADER_URL = "https://asx.api.markitdigital.com/asx-research/1.0/companies/";
    private static final String TWELVE_DATA_EOD_URL = "https://api.twelvedata.com/eod";
    private static final String YAHOO_FINANCE_CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final String YAHOO_FINANCE_QUOTE_URL = "https://finance.yahoo.com/quote/";

    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;
    private final Object yahooFinanceThrottleLock = new Object();
    private final Object yahooFinanceCookieWarmUpLock = new Object();
    private long lastYahooFinanceRequestAtEpochMs = 0L;
    private boolean yahooFinanceCookieWarmUpAttempted = false;

    public FinanceMarketDataService(ApplicationProperties applicationProperties, ObjectMapper objectMapper) {
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
    }

    public Optional<MarketQuoteSnapshot> fetchPreviousClose(FinanceSecurity security) {
        if (security == null || security.getSymbol() == null || security.getSymbol().isBlank()) {
            return Optional.empty();
        }

        if (isAsxSecurity(security)) {
            return fetchAsxQuote(security);
        }

        String apiKey = applicationProperties.getMarketData().getTwelveData().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Twelve Data API key is not configured.");
        }

        try {
            URI requestUri = buildEodUri(security, apiKey);
            LOG.info(
                "Twelve Data EOD request prepared. symbol={}, exchangeMic={}, exchangeName={}, currencyCode={}, url={}",
                security.getSymbol(),
                security.getExchangeMic(),
                security.getExchangeName(),
                security.getCurrencyCode(),
                sanitizeUriForLogs(requestUri, apiKey)
            );
            HttpRequest request = HttpRequest.newBuilder(requestUri).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = readJsonSafely(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warn(
                    "Twelve Data EOD request failed. symbol={}, exchangeMic={}, exchangeName={}, statusCode={}, url={}, responseBody={}",
                    security.getSymbol(),
                    security.getExchangeMic(),
                    security.getExchangeName(),
                    response.statusCode(),
                    sanitizeUriForLogs(requestUri, apiKey),
                    abbreviate(response.body())
                );
                throw new IllegalStateException(providerHttpErrorMessage(response.statusCode(), json));
            }

            if (hasProviderError(json)) {
                LOG.warn(
                    "Twelve Data EOD provider error. symbol={}, exchangeMic={}, exchangeName={}, url={}, responseBody={}",
                    security.getSymbol(),
                    security.getExchangeMic(),
                    security.getExchangeName(),
                    sanitizeUriForLogs(requestUri, apiKey),
                    abbreviate(response.body())
                );
                throw new IllegalStateException(providerErrorMessage(json));
            }

            BigDecimal close = decimalOrNull(json.path("close"));
            LocalDate priceDate = parsePriceDate(json.path("datetime").asText(null), json.path("date").asText(null));
            if (close == null || priceDate == null) {
                return Optional.empty();
            }

            MarketQuoteSnapshot snapshot = new MarketQuoteSnapshot();
            snapshot.setSymbol(security.getSymbol());
            snapshot.setPriceDate(priceDate);
            snapshot.setPrice(close);
            snapshot.setClose(close);
            snapshot.setOpen(decimalOrNull(json.path("open")));
            snapshot.setHigh(decimalOrNull(json.path("high")));
            snapshot.setLow(decimalOrNull(json.path("low")));
            snapshot.setChange(decimalOrNull(json.path("change")));
            snapshot.setVolume(integerOrNull(json.path("volume")));
            snapshot.setFetchedAt(ZonedDateTime.now(ZoneOffset.UTC));
            snapshot.setSource("Twelve Data");
            return Optional.of(snapshot);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.error("Failed to fetch Twelve Data EOD quote for {}", security.getSymbol(), e);
            throw new IllegalStateException("Twelve Data quote request failed.", e);
        }
    }

    private Optional<MarketQuoteSnapshot> fetchAsxQuote(FinanceSecurity security) {
        String asxSymbol = asxSymbol(security);
        if (asxSymbol == null) {
            return Optional.empty();
        }

        URI requestUri = URI.create(ASX_MARKET_DATA_HEADER_URL + encode(asxSymbol) + "/header");
        try {
            LOG.info(
                "ASX quote request prepared. symbol={}, asxSymbol={}, exchangeMic={}, exchangeName={}, url={}",
                security.getSymbol(),
                asxSymbol,
                security.getExchangeMic(),
                security.getExchangeName(),
                requestUri
            );
            HttpRequest request = HttpRequest.newBuilder(requestUri)
                .header("Accept", "application/json,text/plain,*/*")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .GET()
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warn(
                    "ASX quote request failed. symbol={}, asxSymbol={}, statusCode={}, url={}, responseBody={}",
                    security.getSymbol(),
                    asxSymbol,
                    response.statusCode(),
                    requestUri,
                    abbreviate(response.body())
                );
                return Optional.empty();
            }

            MarketQuoteSnapshot snapshot = parseAsxQuote(response.body(), asxSymbol);
            if (snapshot == null) {
                return Optional.empty();
            }
            snapshot.setSymbol(security.getSymbol());
            snapshot.setSource("ASX");
            return Optional.of(snapshot);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warn("ASX quote request failed for {}", asxSymbol, e);
            return Optional.empty();
        }
    }

    MarketQuoteSnapshot parseAsxQuote(String body, String asxSymbol) throws IOException {
        JsonNode data = objectMapper.readTree(body).path("data");
        if (data.isMissingNode() || data.isNull()) {
            LOG.warn("ASX quote response did not contain data for {}", asxSymbol);
            return null;
        }

        BigDecimal price = decimalOrNull(data.path("priceLast"));
        if (price == null) {
            LOG.warn("ASX quote response did not contain priceLast for {}", asxSymbol);
            return null;
        }

        MarketQuoteSnapshot snapshot = new MarketQuoteSnapshot();
        snapshot.setPriceDate(LocalDate.now(ZoneId.of("Australia/Sydney")));
        snapshot.setPrice(price);
        snapshot.setClose(price);
        snapshot.setChange(decimalOrNull(data.path("priceChange")));
        snapshot.setVolume(integerOrNull(data.path("volume")));
        snapshot.setFetchedAt(ZonedDateTime.now(ZoneOffset.UTC));
        return snapshot;
    }

    private Optional<MarketQuoteSnapshot> fetchYahooFinanceQuote(FinanceSecurity security) {
        String yahooSymbol = yahooFinanceSymbol(security);
        if (yahooSymbol == null) {
            return Optional.empty();
        }

        URI chartUri = URI.create(YAHOO_FINANCE_CHART_URL + encode(yahooSymbol) + "?range=5d&interval=1d");
        return fetchYahooFinanceChartQuote(security, yahooSymbol, chartUri);
    }

    private Optional<MarketQuoteSnapshot> fetchYahooFinanceChartQuote(FinanceSecurity security, String yahooSymbol, URI requestUri) {
        try {
            ensureYahooFinanceCookies(yahooSymbol);
            LOG.info(
                "Yahoo Finance chart request prepared. symbol={}, yahooSymbol={}, exchangeMic={}, exchangeName={}, url={}",
                security.getSymbol(),
                yahooSymbol,
                security.getExchangeMic(),
                security.getExchangeName(),
                requestUri
            );
            HttpResponse<String> response = null;
            for (int attempt = 0; attempt < 2; attempt++) {
                throttleYahooFinanceRequest();
                HttpRequest request = HttpRequest.newBuilder(requestUri).headers(yahooFinanceChartHeaders()).GET().build();
                response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 429 || attempt > 0) {
                    break;
                }
                LOG.warn(
                    "Yahoo Finance chart request was rate limited. symbol={}, yahooSymbol={}, url={}. Retrying after {} ms.",
                    security.getSymbol(),
                    yahooSymbol,
                    requestUri,
                    yahooFinanceRequestDelayMs()
                );
                sleep(yahooFinanceRequestDelayMs());
            }
            if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
                int statusCode = response != null ? response.statusCode() : 0;
                String body = response != null ? response.body() : null;
                LOG.warn(
                    "Yahoo Finance chart request failed. symbol={}, yahooSymbol={}, statusCode={}, url={}, responseBody={}",
                    security.getSymbol(),
                    yahooSymbol,
                    statusCode,
                    requestUri,
                    abbreviate(body)
                );
                return Optional.empty();
            }

            MarketQuoteSnapshot snapshot = parseYahooFinanceChartQuote(response.body(), yahooSymbol);
            if (snapshot == null) {
                return Optional.empty();
            }
            snapshot.setSymbol(security.getSymbol());
            snapshot.setSource("Yahoo Finance");
            return Optional.of(snapshot);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warn("Yahoo Finance chart quote request failed for {}", yahooSymbol, e);
            return Optional.empty();
        }
    }

    private void ensureYahooFinanceCookies(String yahooSymbol) {
        String configuredCookie = applicationProperties.getMarketData().getYahooFinance().getCookie();
        if (configuredCookie != null && !configuredCookie.isBlank()) {
            return;
        }
        synchronized (yahooFinanceCookieWarmUpLock) {
            if (yahooFinanceCookieWarmUpAttempted) {
                return;
            }
            yahooFinanceCookieWarmUpAttempted = true;
        }
        URI requestUri = URI.create(YAHOO_FINANCE_QUOTE_URL + encode(yahooSymbol));
        try {
            throttleYahooFinanceRequest();
            HttpRequest request = HttpRequest.newBuilder(requestUri).headers(yahooFinanceChartHeaders()).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.debug(
                "Yahoo Finance cookie warm-up completed. yahooSymbol={}, statusCode={}, cookieCount={}",
                yahooSymbol,
                response.statusCode(),
                COOKIE_MANAGER.getCookieStore().getCookies().size()
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warn("Yahoo Finance cookie warm-up failed for {}. Continuing with chart request.", yahooSymbol, e);
        }
    }

    String[] yahooFinanceChartHeaders() {
        List<String> headers = new ArrayList<>(
            List.of(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "Accept-Language",
                "en-GB,en-US;q=0.9,en;q=0.8",
                "Cache-Control",
                "max-age=0",
                "DNT",
                "1",
                "Priority",
                "u=0, i",
                "Sec-CH-UA",
                "\"Google Chrome\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"",
                "Sec-CH-UA-Mobile",
                "?0",
                "Sec-CH-UA-Platform",
                "\"macOS\"",
                "Sec-Fetch-Dest",
                "document",
                "Sec-Fetch-Mode",
                "navigate",
                "Sec-Fetch-Site",
                "none",
                "Sec-Fetch-User",
                "?1",
                "Upgrade-Insecure-Requests",
                "1",
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"
            )
        );
        String cookie = applicationProperties.getMarketData().getYahooFinance().getCookie();
        if (cookie != null && !cookie.isBlank()) {
            headers.add("Cookie");
            headers.add(cookie.trim());
        }
        return headers.toArray(String[]::new);
    }

    private void throttleYahooFinanceRequest() throws InterruptedException {
        long delayMs = yahooFinanceRequestDelayMs();
        if (delayMs <= 0) {
            return;
        }
        synchronized (yahooFinanceThrottleLock) {
            long waitMs = delayMs - (System.currentTimeMillis() - lastYahooFinanceRequestAtEpochMs);
            while (waitMs > 0) {
                LOG.debug("Throttling Yahoo Finance request for {} ms.", waitMs);
                yahooFinanceThrottleLock.wait(waitMs);
                waitMs = delayMs - (System.currentTimeMillis() - lastYahooFinanceRequestAtEpochMs);
            }
            lastYahooFinanceRequestAtEpochMs = System.currentTimeMillis();
        }
    }

    private long yahooFinanceRequestDelayMs() {
        return Math.max(1000L, applicationProperties.getMarketData().getQuoteRefresh().getRequestDelayMs());
    }

    private void sleep(long delayMs) throws InterruptedException {
        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }
    }

    MarketQuoteSnapshot parseYahooFinanceChartQuote(String body, String yahooSymbol) throws IOException {
        JsonNode result = objectMapper.readTree(body).path("chart").path("result").path(0);
        if (result.isMissingNode() || result.isNull()) {
            JsonNode error = objectMapper.readTree(body).path("chart").path("error");
            if (!error.isMissingNode() && !error.isNull()) {
                LOG.warn("Yahoo Finance chart response contained an error for {}: {}", yahooSymbol, error);
            }
            return null;
        }

        JsonNode meta = result.path("meta");
        BigDecimal price = decimalOrNull(meta.path("regularMarketPrice"));
        if (price == null) {
            price = latestDecimal(result.path("indicators").path("quote").path(0).path("close"));
        }
        if (price == null) {
            LOG.warn("Yahoo Finance chart response did not contain a market price for {}", yahooSymbol);
            return null;
        }

        ZonedDateTime priceDateTime = epochSecondsOrNull(meta.path("regularMarketTime"))
            .map(epochSeconds -> ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC))
            .orElseGet(() ->
                latestLong(result.path("timestamp"))
                    .map(epochSeconds -> ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC))
                    .orElseGet(() -> ZonedDateTime.now(ZoneOffset.UTC))
            );

        JsonNode quote = result.path("indicators").path("quote").path(0);
        MarketQuoteSnapshot snapshot = new MarketQuoteSnapshot();
        snapshot.setPriceDate(priceDateTime.toLocalDate());
        snapshot.setPrice(price);
        snapshot.setClose(price);
        snapshot.setOpen(latestDecimal(quote.path("open")));
        snapshot.setHigh(latestDecimal(quote.path("high")));
        snapshot.setLow(latestDecimal(quote.path("low")));
        snapshot.setVolume(latestInteger(quote.path("volume")));
        snapshot.setFetchedAt(ZonedDateTime.now(ZoneOffset.UTC));
        return snapshot;
    }

    private Optional<Long> epochSecondsOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Optional.empty();
        }
        try {
            return Optional.of(node.asLong());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private BigDecimal latestDecimal(JsonNode nodes) {
        if (nodes == null || !nodes.isArray()) {
            return null;
        }
        for (int i = nodes.size() - 1; i >= 0; i--) {
            BigDecimal value = decimalOrNull(nodes.get(i));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Optional<Long> latestLong(JsonNode nodes) {
        if (nodes == null || !nodes.isArray()) {
            return Optional.empty();
        }
        for (int i = nodes.size() - 1; i >= 0; i--) {
            JsonNode node = nodes.get(i);
            if (node != null && !node.isNull()) {
                return Optional.of(node.asLong());
            }
        }
        return Optional.empty();
    }

    private Integer latestInteger(JsonNode nodes) {
        Optional<Long> value = latestLong(nodes);
        if (value.isEmpty()) {
            return null;
        }
        long number = value.get();
        if (number > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (number < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) number;
    }

    private String sanitizeUriForLogs(URI uri, String apiKey) {
        return uri.toString().replace(encode(apiKey), "****");
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500) + "...";
    }

    private URI buildEodUri(FinanceSecurity security, String apiKey) {
        StringBuilder builder = new StringBuilder(TWELVE_DATA_EOD_URL)
            .append("?symbol=")
            .append(encode(security.getSymbol()))
            .append("&apikey=")
            .append(encode(apiKey));

        String twelveDataExchangeName = twelveDataExchangeName(security);
        if (twelveDataExchangeName != null) {
            builder.append("&exchange=").append(encode(twelveDataExchangeName));
        } else if (security.getExchangeMic() != null && !security.getExchangeMic().isBlank()) {
            builder.append("&mic_code=").append(encode(security.getExchangeMic()));
        } else if (security.getExchangeName() != null && !security.getExchangeName().isBlank()) {
            builder.append("&exchange=").append(encode(security.getExchangeName()));
        }

        return URI.create(builder.toString());
    }

    private String twelveDataExchangeName(FinanceSecurity security) {
        String exchangeMic = normalizeUpper(security.getExchangeMic());
        String exchangeName = normalizeUpper(security.getExchangeName());
        if ("XNAS".equals(exchangeMic) || "NASDAQ".equals(exchangeName)) {
            return "NASDAQ";
        }
        if ("XNYS".equals(exchangeMic) || "NYSE".equals(exchangeName) || "NEW YORK STOCK EXCHANGE".equals(exchangeName)) {
            return "NYSE";
        }
        return null;
    }

    private boolean isAsxSecurity(FinanceSecurity security) {
        String exchangeMic = normalizeUpper(security.getExchangeMic());
        String exchangeName = normalizeUpper(security.getExchangeName());
        String exchangeSuffix = normalizeUpper(security.getExchangeSuffix());
        String symbol = normalizeUpper(security.getSymbol());
        return (
            "XASX".equals(exchangeMic) ||
            "ASX".equals(exchangeName) ||
            "AUSTRALIAN SECURITIES EXCHANGE".equals(exchangeName) ||
            "AX".equals(exchangeSuffix) ||
            (symbol != null && symbol.endsWith(".AX"))
        );
    }

    private String yahooFinanceSymbol(FinanceSecurity security) {
        String symbol = security.getSymbol();
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        symbol = symbol.trim();
        int prefixIndex = symbol.indexOf(':');
        if (prefixIndex >= 0 && prefixIndex < symbol.length() - 1) {
            symbol = symbol.substring(prefixIndex + 1);
        }
        if (symbol.toUpperCase().endsWith(".AX")) {
            return symbol;
        }
        return symbol + ".AX";
    }

    private String asxSymbol(FinanceSecurity security) {
        String symbol = security.getSymbol();
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        symbol = symbol.trim();
        int prefixIndex = symbol.indexOf(':');
        if (prefixIndex >= 0 && prefixIndex < symbol.length() - 1) {
            symbol = symbol.substring(prefixIndex + 1);
        }
        if (symbol.toUpperCase().endsWith(".AX")) {
            symbol = symbol.substring(0, symbol.length() - 3);
        }
        return symbol.toUpperCase();
    }

    private String normalizeUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private JsonNode readJsonSafely(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(body);
    }

    private boolean hasProviderError(JsonNode json) {
        return (
            "error".equalsIgnoreCase(json.path("status").asText(null)) ||
            json.hasNonNull("code") ||
            (json.hasNonNull("message") && !json.hasNonNull("close"))
        );
    }

    private String providerErrorMessage(JsonNode json) {
        String message = json.path("message").asText(null);
        if (message == null || message.isBlank()) {
            return "Twelve Data did not return a quote.";
        }
        return message;
    }

    private String providerHttpErrorMessage(int statusCode, JsonNode json) {
        String providerMessage = providerErrorMessage(json);
        if (providerMessage == null || providerMessage.isBlank() || "Twelve Data did not return a quote.".equals(providerMessage)) {
            return "Twelve Data EOD request failed with HTTP " + statusCode + ".";
        }
        return "Twelve Data EOD request failed with HTTP " + statusCode + ": " + providerMessage;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer integerOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(text);
            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (value < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parsePriceDate(String dateTime, String date) {
        if (dateTime != null && !dateTime.isBlank()) {
            return LocalDate.parse(dateTime.substring(0, 10));
        }
        if (date != null && !date.isBlank()) {
            return LocalDate.parse(date.substring(0, 10));
        }
        return null;
    }

    public static class MarketQuoteSnapshot {

        private String symbol;
        private LocalDate priceDate;
        private BigDecimal price;
        private BigDecimal open;
        private BigDecimal close;
        private BigDecimal high;
        private BigDecimal low;
        private Integer volume;
        private BigDecimal change;
        private ZonedDateTime fetchedAt;
        private String source;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public LocalDate getPriceDate() {
            return priceDate;
        }

        public void setPriceDate(LocalDate priceDate) {
            this.priceDate = priceDate;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public BigDecimal getOpen() {
            return open;
        }

        public void setOpen(BigDecimal open) {
            this.open = open;
        }

        public BigDecimal getClose() {
            return close;
        }

        public void setClose(BigDecimal close) {
            this.close = close;
        }

        public BigDecimal getHigh() {
            return high;
        }

        public void setHigh(BigDecimal high) {
            this.high = high;
        }

        public BigDecimal getLow() {
            return low;
        }

        public void setLow(BigDecimal low) {
            this.low = low;
        }

        public Integer getVolume() {
            return volume;
        }

        public void setVolume(Integer volume) {
            this.volume = volume;
        }

        public BigDecimal getChange() {
            return change;
        }

        public void setChange(BigDecimal change) {
            this.change = change;
        }

        public ZonedDateTime getFetchedAt() {
            return fetchedAt;
        }

        public void setFetchedAt(ZonedDateTime fetchedAt) {
            this.fetchedAt = fetchedAt;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
