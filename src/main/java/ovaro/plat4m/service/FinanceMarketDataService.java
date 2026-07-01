package ovaro.plat4m.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ovaro.plat4m.config.ApplicationProperties;
import ovaro.plat4m.domain.FinanceSecurity;

@Service
public class FinanceMarketDataService {

    private static final Logger LOG = LoggerFactory.getLogger(FinanceMarketDataService.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String TWELVE_DATA_EOD_URL = "https://api.twelvedata.com/eod";

    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;

    public FinanceMarketDataService(ApplicationProperties applicationProperties, ObjectMapper objectMapper) {
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
    }

    public Optional<MarketQuoteSnapshot> fetchPreviousClose(FinanceSecurity security) {
        if (security == null || security.getSymbol() == null || security.getSymbol().isBlank()) {
            return Optional.empty();
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
            return Optional.of(snapshot);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.error("Failed to fetch Twelve Data EOD quote for {}", security.getSymbol(), e);
            throw new IllegalStateException("Twelve Data quote request failed.", e);
        }
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

        if (security.getExchangeMic() != null && !security.getExchangeMic().isBlank()) {
            builder.append("&mic_code=").append(encode(security.getExchangeMic()));
        } else if (security.getExchangeName() != null && !security.getExchangeName().isBlank()) {
            builder.append("&exchange=").append(encode(security.getExchangeName()));
        }

        return URI.create(builder.toString());
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
    }
}
