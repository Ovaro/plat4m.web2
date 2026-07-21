package ovaro.plat4m.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import ovaro.plat4m.config.ApplicationProperties;

class FinanceMarketDataServiceTest {

    private final FinanceMarketDataService service = new FinanceMarketDataService(new ApplicationProperties(), new ObjectMapper());

    @Test
    void parseAsxQuoteReadsLastPriceChangeAndVolume() throws Exception {
        String body = """
            {
              "data": {
                "displayName": "VANGUARD AUSTRALIAN SHARES INDEX ETF",
                "priceAsk": 108.99,
                "priceBid": 108.98,
                "priceChange": -0.10,
                "priceChangePercent": -0.0916,
                "priceLast": 109,
                "securityType": 7,
                "symbol": "VAS",
                "volume": 194837,
                "statusCode": "XD"
              }
            }
            """;

        FinanceMarketDataService.MarketQuoteSnapshot snapshot = service.parseAsxQuote(body, "VAS");

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getPrice()).isEqualByComparingTo("109");
        assertThat(snapshot.getClose()).isEqualByComparingTo("109");
        assertThat(snapshot.getChange()).isEqualByComparingTo("-0.10");
        assertThat(snapshot.getVolume()).isEqualTo(194837);
    }

    @Test
    void yahooFinanceChartHeadersMatchBrowserStyleRequest() {
        String[] headers = service.yahooFinanceChartHeaders();
        Map<String, String> headersByName = IntStream.range(0, headers.length / 2)
            .boxed()
            .collect(Collectors.toMap(index -> headers[index * 2], index -> headers[index * 2 + 1]));

        assertThat(headersByName.get("Accept")).contains("text/html");
        assertThat(headersByName.get("Accept-Language")).isEqualTo("en-GB,en-US;q=0.9,en;q=0.8");
        assertThat(headersByName.get("Sec-Fetch-Mode")).isEqualTo("navigate");
        assertThat(headersByName.get("User-Agent")).contains("Chrome/149.0.0.0");
    }

    @Test
    void yahooFinanceChartHeadersIncludeConfiguredCookie() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getMarketData().getYahooFinance().setCookie("A1=test; A3=test");
        FinanceMarketDataService serviceWithCookie = new FinanceMarketDataService(properties, new ObjectMapper());

        String[] headers = serviceWithCookie.yahooFinanceChartHeaders();
        Map<String, String> headersByName = IntStream.range(0, headers.length / 2)
            .boxed()
            .collect(Collectors.toMap(index -> headers[index * 2], index -> headers[index * 2 + 1]));

        assertThat(headersByName.get("Cookie")).isEqualTo("A1=test; A3=test");
    }

    @Test
    void parseYahooFinanceChartQuoteReadsRegularMarketPriceAndLatestDailyValues() throws Exception {
        String body = """
            {
              "chart": {
                "result": [
                  {
                    "meta": {
                      "symbol": "QBE.AX",
                      "regularMarketPrice": 20.64,
                      "regularMarketTime": 1783965600
                    },
                    "timestamp": [1783879200, 1783965600],
                    "indicators": {
                      "quote": [
                        {
                          "open": [20.10, 20.42],
                          "high": [20.50, 20.80],
                          "low": [19.95, 20.25],
                          "close": [20.40, 20.64],
                          "volume": [1000000, 1200000]
                        }
                      ]
                    }
                  }
                ],
                "error": null
              }
            }
            """;

        FinanceMarketDataService.MarketQuoteSnapshot snapshot = service.parseYahooFinanceChartQuote(body, "QBE.AX");

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getPrice()).isEqualByComparingTo("20.64");
        assertThat(snapshot.getClose()).isEqualByComparingTo("20.64");
        assertThat(snapshot.getOpen()).isEqualByComparingTo("20.42");
        assertThat(snapshot.getHigh()).isEqualByComparingTo("20.80");
        assertThat(snapshot.getLow()).isEqualByComparingTo("20.25");
        assertThat(snapshot.getVolume()).isEqualTo(1200000);
        assertThat(snapshot.getPriceDate()).isEqualTo(LocalDate.of(2026, 7, 13));
    }
}
