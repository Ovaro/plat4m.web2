package ovaro.plat4m.service;

import java.net.UnknownHostException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ovaro.plat4m.config.ApplicationProperties;
import ovaro.plat4m.domain.FinanceExchange;
import ovaro.plat4m.domain.FinanceSecurity;
import ovaro.plat4m.domain.FinanceSecurityType;
import ovaro.plat4m.repository.FinanceExchangeRepository;
import ovaro.plat4m.repository.FinanceSecurityPriceRepository;
import ovaro.plat4m.repository.FinanceSecurityRepository;
import ovaro.plat4m.service.idto.LatticeCompanyProfileHolderIDTO;
import ovaro.plat4m.service.idto.LatticeCompanyProfileIDTO;
import ovaro.plat4m.service.idto.MultipleResultsException;
import ovaro.plat4m.service.idto.TwelveDataStockDataIDTO;
import ovaro.plat4m.service.idto.TwelveDataStocksIDTO;
import reactor.core.publisher.Mono;

@Service
public class FinanceSecurityIntegrationService {

    private final Logger log = LoggerFactory.getLogger(FinanceSecurityService.class);

    private ApplicationProperties properties;
    private FinanceSecurityRepository securityRepository;
    private FinanceSecurityPriceRepository securitySPRepository;
    private FinanceExchangeRepository exchangeRepository;

    public FinanceSecurityIntegrationService(
        ApplicationProperties properties,
        FinanceExchangeRepository exchangeRepository,
        FinanceSecurityRepository securityRepository,
        FinanceSecurityPriceRepository securitySPRepository
    ) {
        this.securityRepository = securityRepository;
        this.securitySPRepository = securitySPRepository;
        this.properties = properties;
        this.exchangeRepository = exchangeRepository;
    }

    public FinanceSecurity fetchSecurityInformation(String symbol, String defaultCurrency, String currencyCode)
        throws MultipleResultsException {
        WebClient client = WebClient.create();

        //WebClient.ResponseSpec responseSpec
        TwelveDataStockDataIDTO match = null;

        TwelveDataStockDataIDTO[] stocks = this.fetchTwelveDataStocks(client, symbol);
        match = this.match(stocks, defaultCurrency, currencyCode);
        if (match == null) {
            stocks = fetchTwelveDataETFs(client, symbol);
            match = this.match(stocks, defaultCurrency, currencyCode);
            if (match == null) {
                //stocks = fetchTwelveDataIndicies(client, symbol);
                log.warn("Couldn't find: '" + symbol + "'. Returning nothing.");
                return null;
            } else {
                // Set type to ETF
                if (match.getType() == null) {
                    match.setType("ETF");
                }
            }
        }
        // if(stocks.length > 1) {
        //     throw new MultipleResultsException();
        // }

        FinanceSecurity security = new FinanceSecurity();

        security.setCountry(match.getCountry());
        security.setCurrencyCode(match.getCurrency());
        security.setExchangeMic(match.getMicCode());
        //security.setIndustry(stocks[0].get());
        security.setName(match.getName());
        security.setSymbol(match.getSymbol());

        security.setExchangeName(match.getExchange());
        security.setType(translateType(match.getType()).value());

        Optional<FinanceExchange> oexchange = exchangeRepository.findByMic(match.getMicCode());

        String yahooSymbol = symbol;
        if (!oexchange.isEmpty()) {
            FinanceExchange exchange = oexchange.get();
            security.setExchangeSuffix(exchange.getSuffix());
            if (exchange.getSuffix() != null) {
                yahooSymbol = yahooSymbol + "." + exchange.getSuffix();
            }
        }

        // Now get extended info from Yahoo Finance
        LatticeCompanyProfileIDTO profile = fetchLatticeCompanyProfile(client, yahooSymbol);
        if (profile != null) {
            security.setIndustry(profile.getIndustry());
            security.setSector(profile.getSector());
        }

        return security;
    }

    private TwelveDataStockDataIDTO match(TwelveDataStockDataIDTO[] stocks, String defaultCurrency, String currencyCode) {
        if (stocks == null || stocks.length == 0) {
            return null;
        }
        TwelveDataStockDataIDTO match = null;
        TwelveDataStockDataIDTO usMatch = null;
        if (stocks.length > 1) {
            for (TwelveDataStockDataIDTO security : stocks) {
                log.info("Security: " + security);
                if (security.getCurrency().equals(currencyCode) && !security.getExchange().equals("CXA")) {
                    match = security;
                    break;
                } else if (security.getCurrency().equals(defaultCurrency) && !security.getExchange().equals("CXA")) {
                    match = security;
                    break;
                } else if (defaultCurrency == null && security.getCurrency() == "USD") {
                    // Assume match
                    match = security;
                    break;
                } else if (security.getCurrency() == "USD") {
                    // Hold the US one in case nothing else matches
                    usMatch = security;
                }
            }
        } else {
            match = stocks[0];
        }

        if (match == null && usMatch != null) {
            return usMatch;
        }

        return match;
    }

    public TwelveDataStockDataIDTO[] fetchTwelveDataStocks(WebClient client, String symbol) {
        return this.fetchTwelveData(client, symbol, "stocks");
    }

    public TwelveDataStockDataIDTO[] fetchTwelveDataETFs(WebClient client, String symbol) {
        return this.fetchTwelveData(client, symbol, "etf");
    }

    public TwelveDataStockDataIDTO[] fetchTwelveDataIndices(WebClient client, String symbol) {
        return this.fetchTwelveData(client, symbol, "indices");
    }

    public TwelveDataStockDataIDTO[] fetchTwelveData(WebClient client, String symbol, String type) {
        Mono<TwelveDataStocksIDTO> monoStocks = client
            .get()
            .uri("https://twelve-data1.p.rapidapi.com/" + type + "?format=json&symbol=" + symbol)
            .header("X-RapidAPI-Key", properties.getRapidapiKey())
            .retrieve()
            .bodyToMono(TwelveDataStocksIDTO.class);

        //String responseBody = responseSpec.bodyToMono(String.class).block();
        log.info("Response: " + monoStocks);
        try {
            TwelveDataStocksIDTO stocksEnvelope = monoStocks.block();
            TwelveDataStockDataIDTO stocks[] = stocksEnvelope.getData();

            return stocks;
        } catch (RuntimeException rte) {
            if (rte.getCause() != null) {
                log.error("Web Service Error: " + rte.getCause().getLocalizedMessage());
            } else {
                log.error("Web Service Error: " + rte.getLocalizedMessage());
            }
            if (!(rte.getCause() instanceof UnknownHostException)) {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    // Try again
                    TwelveDataStocksIDTO stocksEnvelope = monoStocks.block();
                    TwelveDataStockDataIDTO stocks[] = stocksEnvelope.getData();

                    return stocks;
                } catch (RuntimeException rte2) {
                    if (rte2.getCause() != null) {
                        log.error("Web Service Error:" + rte2.getCause().getLocalizedMessage());
                    } else {
                        log.error("Web Service Error:" + rte2.getLocalizedMessage());
                    }
                }
            }
        }

        return null;
    }

    // Only Free for US
    public LatticeCompanyProfileIDTO fetchTwelveDataCompanyProfile(WebClient client, String symbol, String type) {
        Mono<LatticeCompanyProfileIDTO> monoStocks = client
            .get()
            .uri("https://twelve-data1.p.rapidapi.com/" + type + "?format=json&symbol=" + symbol)
            .header("X-RapidAPI-Key", properties.getRapidapiKey())
            .retrieve()
            .bodyToMono(LatticeCompanyProfileIDTO.class);

        //String responseBody = responseSpec.bodyToMono(String.class).block();
        log.info("Response: " + monoStocks);
        try {
            LatticeCompanyProfileIDTO res = monoStocks.block();

            return res;
        } catch (RuntimeException rte) {
            log.error("Web Service Error " + rte.getCause().getLocalizedMessage());
            if (!(rte.getCause() instanceof UnknownHostException)) {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    // Try again
                    LatticeCompanyProfileIDTO res = monoStocks.block();
                    return res;
                } catch (RuntimeException rte2) {
                    if (rte2.getCause() != null) {
                        log.error("Web Service Error:" + rte2.getCause().getLocalizedMessage());
                    } else {
                        log.error("Web Service Error:" + rte2.getLocalizedMessage());
                    }
                }
            }
        }

        return null;
    }

    public LatticeCompanyProfileIDTO fetchLatticeCompanyProfile(WebClient client, String symbol) {
        Mono<LatticeCompanyProfileHolderIDTO> monoStocks = client
            .get()
            .uri("https://stock-market-data.p.rapidapi.com/yfinance/company-profile?ticker_symbol=" + symbol)
            .header("X-RapidAPI-Key", properties.getRapidapiKey())
            .retrieve()
            .bodyToMono(LatticeCompanyProfileHolderIDTO.class);

        //String responseBody = responseSpec.bodyToMono(String.class).block();
        try {
            LatticeCompanyProfileHolderIDTO stocksEnvelope = monoStocks.block();
            if (stocksEnvelope.getStatus().equals("success") && stocksEnvelope.getCompanyProfile() != null) {
                if (stocksEnvelope.getCompanyProfile().getSector() != null) {
                    return stocksEnvelope.getCompanyProfile();
                }
            }
        } catch (RuntimeException rte) {
            log.error("Web Service Error:" + rte.getLocalizedMessage());
        }
        return null;
    }

    public FinanceSecurityType translateType(String type) {
        if (type == "Common Stock") {
            return FinanceSecurityType.STOCK;
        } else if (type == "ETF") {
            return FinanceSecurityType.ETF;
        }

        log.warn("Couldn't translate: '" + type + "'. Assuming STOCK");
        return FinanceSecurityType.STOCK;
    }
}
