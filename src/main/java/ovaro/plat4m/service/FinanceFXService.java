package ovaro.plat4m.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceFXRepository;
import ovaro.plat4m.service.dto.FinanceFXImportRequestDTO;
import ovaro.plat4m.service.dto.FinanceFXImportResultDTO;
import ovaro.plat4m.service.dto.FinanceFXUpdateDTO;
import ovaro.plat4m.service.dto.FinanceSnapshot;

@Service
public class FinanceFXService {

    private final Logger log = LoggerFactory.getLogger(FinanceFXService.class);
    private static final String FRANKFURTER_RATES_URL = "https://api.frankfurter.dev/v2/rates";

    private FinanceFXRepository fxRepository;
    private final ObjectMapper objectMapper;

    //private FinanceCurrencyRepository currencyRepository;

    public FinanceFXService(FinanceFXRepository fxRepository, ObjectMapper objectMapper) {
        // , FinanceCurrencyRepository currencyRepository
        this.fxRepository = fxRepository;
        this.objectMapper = objectMapper;
        //this.currencyRepository = currencyRepository;
    }

    public FinanceFX getLatestFX(String from, String to, LocalDate date) {
        StopWatch sw = new StopWatch();
        sw.start("getLastestFX");
        List<FinanceFX> fxs = this.fxRepository.findByFromIsoCodeAndToIsoCode(from, to, date);
        // Check result:
        FinanceFX result = null;
        FinanceFX other = null;

        for (FinanceFX fx : fxs) {
            // Find the right one
            if (fx.getFromIsoCode().equals(from)) {
                result = fx;
            }

            if (fx.getToIsoCode().equals(from)) {
                other = fx;
            }
        }

        if (result == null && other != null) {
            result = new FinanceFX(other.getDate(), from, to, 1 / other.getRate());
        }

        sw.stop();
        log.info(sw.prettyPrint());
        return result;
    }

    // public List<FinanceFX> getLatestFX(List<String> from, String to) {
    //     StopWatch sw = new StopWatch();
    //     sw.start("getLastestFX");
    //     List<FinanceFX> accounts = this.fxRepository.findLatestFX(from, to);
    //     sw.stop();
    //     log.info(sw.prettyPrint());
    //     return accounts;
    // }

    public List<FinanceFX> getLatestFXAll() {
        StopWatch sw = new StopWatch();
        sw.start("getLastestFX");
        List<FinanceFX> accounts = this.fxRepository.findLatestDistinctPairs();
        sw.stop();
        log.info(sw.prettyPrint());
        return accounts;
    }

    public List<FinanceFX> getFXHistory(String fromIsoCode, String toIsoCode) {
        StopWatch sw = new StopWatch();
        sw.start("getFXHistory");
        String normalizedFromIsoCode = normalizeCurrencyCode(fromIsoCode, null);
        String normalizedToIsoCode = normalizeCurrencyCode(toIsoCode, null);
        if (normalizedFromIsoCode == null || normalizedToIsoCode == null) {
            throw new IllegalArgumentException("From and to currencies are required");
        }

        List<FinanceFX> history = this.fxRepository.findByFromIsoCodeAndToIsoCode(
            normalizedFromIsoCode,
            normalizedToIsoCode,
            Sort.by(Sort.Direction.DESC, "date")
        );
        sw.stop();
        log.info(sw.prettyPrint());
        return history;
    }

    @Transactional
    public FinanceFX createFX(FinanceFXUpdateDTO update) {
        FinanceFX fx = new FinanceFX();
        applyFXUpdate(fx, update);
        return this.fxRepository.save(fx);
    }

    @Transactional
    public FinanceFX updateFX(UUID id, FinanceFXUpdateDTO update) {
        FinanceFX fx = this.fxRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("FX rate not found"));
        applyFXUpdate(fx, update);
        return this.fxRepository.save(fx);
    }

    @Transactional
    public void deleteFX(UUID id) {
        FinanceFX fx = this.fxRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("FX rate not found"));
        this.fxRepository.delete(fx);
    }

    @Transactional
    public FinanceFXImportResultDTO importFrankfurterRates(FinanceFXImportRequestDTO request) {
        String baseCurrency = normalizeCurrencyCode(request.getBaseCurrency(), "AUD");
        List<String> quoteCurrencies = normalizeQuoteCurrencies(request.getQuoteCurrencies(), baseCurrency);
        LocalDate requestedDate = request.getDate();

        FrankfurterRatesResponse response = fetchFrankfurterRates(baseCurrency, quoteCurrencies, requestedDate);

        if (response == null || response.rates() == null || response.date() == null) {
            throw new IllegalStateException("No FX rates were returned by Frankfurter");
        }

        int updated = 0;
        ZonedDateTime fxDate = toZonedDateTime(response.date());
        for (Map.Entry<String, Double> entry : response.rates().entrySet()) {
            String quoteCurrency = normalizeCurrencyCode(entry.getKey(), null);
            Double rate = entry.getValue();
            if (quoteCurrency == null || rate == null) {
                continue;
            }

            FinanceFX fx = this.fxRepository
                .findByDateAndFromIsoCodeAndToIsoCode(fxDate, baseCurrency, quoteCurrency)
                .orElseGet(FinanceFX::new);
            fx.setDate(fxDate);
            fx.setFromIsoCode(baseCurrency);
            fx.setToIsoCode(quoteCurrency);
            fx.setRate(rate);
            this.fxRepository.save(fx);
            updated++;
        }

        return new FinanceFXImportResultDTO(response.date(), baseCurrency, updated);
    }

    public List<FinanceFX> findFXSummaries(String fromIsoCode, String toIsoCode, LocalDate fromDate, LocalDate toDate, int days) {
        if (days > 1) {
            return this.fxRepository.findFXSummaries(fromIsoCode, toIsoCode, fromDate, toDate, days);
        } else {
            return this.fxRepository.monthlyFX(fromIsoCode, toIsoCode, fromDate, toDate);
        }
    }

    // Expects snapshots to be in order
    public void addFXDetailsToMonthlySnapshots(User user, String currencyCode, List<FinanceSnapshot> snapshots) {
        Map<String, List<FinanceFX>> cache = new HashMap<String, List<FinanceFX>>();
        if (snapshots != null && snapshots.size() > 0) {
            // Check the snapshots which should be consistent on what currency they are in (i.e. cannot change)
            FinanceSnapshot sFirst = snapshots.get(0);

            if (currencyCode != null && !currencyCode.equals(user.getLocalCurrency())) {
                // If currency code is not the local one (or not set which implies local)

                FinanceSnapshot sLast = snapshots.get(snapshots.size() - 1);
                LocalDate start = sFirst.getDate();
                LocalDate end = sLast.getDate();

                List<FinanceFX> fxs = cache.get(currencyCode);
                if (fxs == null) {
                    fxs = this.fxRepository.monthlyFXAsending(currencyCode, user.getLocalCurrency(), start.minus(Period.ofMonths(2)), end); // Just get two months just in case.

                    // If no records we should at least grab the earliest value available and shove in.
                    if (fxs == null) {
                        fxs = new ArrayList<FinanceFX>();
                    }
                    if (fxs.size() == 0) {
                        FinanceFX fx = this.getLatestFX(currencyCode, user.getLocalCurrency(), start);
                        fxs.add(fx);
                    }
                    cache.put(currencyCode, fxs);
                }

                addFXDetailsToMonthlySnapshots(user, currencyCode, snapshots, fxs);
            }
        }
    }

    private void addFXDetailsToMonthlySnapshots(User user, String currencyCode, List<FinanceSnapshot> snapshots, List<FinanceFX> fxs) {
        Iterator<FinanceFX> fxIterator = fxs.iterator();
        FinanceFX lastFX = null;
        FinanceFX fx = null;
        LocalDate fxDate = null;
        for (FinanceSnapshot s : snapshots) {
            if (fx == null && fxIterator.hasNext()) {
                fx = fxIterator.next();
                if (fx != null) {
                    fxDate = fx.getDate().toLocalDate();
                }
            } else {
                // End of FXs
            }

            if (fxDate != null && fx != null && s.getDate().compareTo(fxDate) == 0) {
                lastFX = fx;
                s.setFxDate(fxDate);
                s.setFxToLocal(fx.getRate());
            } else if (fxDate != null && fx != null && s.getDate().compareTo(fxDate) > 0) {
                // Continue rolling forward until equal or less
                lastFX = fx;
                while (s.getDate().compareTo(fxDate) > 0) {
                    if (fxIterator.hasNext()) {
                        fx = fxIterator.next();

                        fxDate = fx.getDate().toLocalDate();
                        if (s.getDate().compareTo(fxDate) >= 0) {
                            lastFX = fx;
                        }
                    } else {
                        break;
                    }
                }
                s.setFxDate(lastFX.getDate().toLocalDate());
                s.setFxToLocal(lastFX.getRate());
            } else if (lastFX != null) {
                // Snapshot date is before the FX date but there was a lastFX we can use
                s.setFxDate(lastFX.getDate().toLocalDate());
                s.setFxToLocal(lastFX.getRate());
            } else {
                log.warn("No previous FX rate to use for snapshot date: " + s.getDate() + ", querying again (inefficient)");
                lastFX = this.getLatestFX(currencyCode, user.getLocalCurrency(), s.getDate());
                if (lastFX == null) {
                    // get the latest rate we can
                    lastFX = this.getLatestFX(currencyCode, user.getLocalCurrency(), LocalDate.now());
                }

                if (lastFX != null) {
                    s.setFxDate(lastFX.getDate().toLocalDate());
                    s.setFxToLocal(lastFX.getRate());
                }
            }
        }
    }

    private void applyFXUpdate(FinanceFX fx, FinanceFXUpdateDTO update) {
        if (update.getDate() == null) {
            throw new IllegalArgumentException("Date is required");
        }
        String fromIsoCode = normalizeCurrencyCode(update.getFromIsoCode(), null);
        String toIsoCode = normalizeCurrencyCode(update.getToIsoCode(), null);
        if (fromIsoCode == null || toIsoCode == null) {
            throw new IllegalArgumentException("From and to currencies are required");
        }
        if (Objects.equals(fromIsoCode, toIsoCode)) {
            throw new IllegalArgumentException("From and to currencies must be different");
        }
        if (update.getRate() == null || update.getRate() <= 0) {
            throw new IllegalArgumentException("Rate must be greater than zero");
        }

        fx.setDate(toZonedDateTime(update.getDate()));
        fx.setFromIsoCode(fromIsoCode);
        fx.setToIsoCode(toIsoCode);
        fx.setRate(update.getRate());
    }

    private List<String> normalizeQuoteCurrencies(List<String> quoteCurrencies, String baseCurrency) {
        List<String> normalizedCurrencies =
            quoteCurrencies == null
                ? List.of("USD", "EUR", "GBP", "NZD", "CAD", "JPY", "CHF", "SGD")
                : quoteCurrencies
                      .stream()
                      .map(currency -> normalizeCurrencyCode(currency, null))
                      .filter(Objects::nonNull)
                      .filter(currency -> !currency.equals(baseCurrency))
                      .distinct()
                      .toList();
        if (normalizedCurrencies.isEmpty()) {
            throw new IllegalArgumentException("At least one quote currency is required");
        }
        return normalizedCurrencies;
    }

    private String normalizeCurrencyCode(String currencyCode, String defaultValue) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return defaultValue;
        }
        return currencyCode.trim().toUpperCase();
    }

    private ZonedDateTime toZonedDateTime(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault());
    }

    private FrankfurterRatesResponse fetchFrankfurterRates(String baseCurrency, List<String> quoteCurrencies, LocalDate requestedDate) {
        try {
            String uri = buildFrankfurterRatesUri(baseCurrency, quoteCurrencies, requestedDate);
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Frankfurter returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return parseFrankfurterRatesResponse(root, response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read Frankfurter FX response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Frankfurter FX request was interrupted", e);
        }
    }

    private String buildFrankfurterRatesUri(String baseCurrency, List<String> quoteCurrencies, LocalDate requestedDate) {
        StringBuilder uri = new StringBuilder(FRANKFURTER_RATES_URL)
            .append("?base=")
            .append(encodeQueryValue(baseCurrency))
            .append("&quotes=")
            .append(encodeQueryValue(String.join(",", quoteCurrencies)));
        if (requestedDate != null) {
            uri.append("&date=").append(encodeQueryValue(requestedDate.toString()));
        }
        return uri.toString();
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private FrankfurterRatesResponse parseFrankfurterRatesResponse(JsonNode root, String responseBody) {
        if (root.isArray()) {
            if (root.isEmpty()) {
                throw new IllegalStateException("No FX rates were returned by Frankfurter");
            }
            return parseFrankfurterRateRows(root, responseBody);
        }

        if (root.has("data") && root.get("data").isArray()) {
            return parseFrankfurterRateRows(root.get("data"), responseBody);
        }

        return parseFrankfurterRatesObject(root, responseBody);
    }

    private FrankfurterRatesResponse parseFrankfurterRatesObject(JsonNode rateNode, String responseBody) {
        JsonNode dateNode = rateNode.get("date");
        JsonNode baseNode = rateNode.get("base");
        JsonNode ratesNode = rateNode.get("rates");
        if (dateNode == null || ratesNode == null || !ratesNode.isObject()) {
            throw new IllegalStateException("Frankfurter FX response was not a recognised rates shape: " + previewResponse(responseBody));
        }

        Map<String, Double> rates = new HashMap<>();
        ratesNode.fields().forEachRemaining(entry -> {
            if (entry.getValue().isNumber()) {
                rates.put(entry.getKey(), entry.getValue().asDouble());
            }
        });
        return new FrankfurterRatesResponse(LocalDate.parse(dateNode.asText()), baseNode == null ? null : baseNode.asText(), rates);
    }

    private FrankfurterRatesResponse parseFrankfurterRateRows(JsonNode rows, String responseBody) {
        LocalDate date = null;
        String base = null;
        Map<String, Double> rates = new HashMap<>();

        for (JsonNode row : rows) {
            JsonNode dateNode = row.get("date");
            JsonNode baseNode = row.get("base");
            JsonNode quoteNode = row.get("quote");
            JsonNode rateNode = row.get("rate");
            if (dateNode == null || quoteNode == null || rateNode == null || !rateNode.isNumber()) {
                continue;
            }

            if (date == null) {
                date = LocalDate.parse(dateNode.asText());
            }
            if (base == null && baseNode != null) {
                base = baseNode.asText();
            }
            rates.put(quoteNode.asText(), rateNode.asDouble());
        }

        if (date == null || rates.isEmpty()) {
            throw new IllegalStateException(
                "Frankfurter FX response was not a recognised rate-row shape: " + previewResponse(responseBody)
            );
        }

        return new FrankfurterRatesResponse(date, base, rates);
    }

    private String previewResponse(String responseBody) {
        if (responseBody == null) {
            return "";
        }
        return responseBody.length() <= 300 ? responseBody : responseBody.substring(0, 300) + "...";
    }

    private record FrankfurterRatesResponse(LocalDate date, String base, Map<String, Double> rates) {}
}
