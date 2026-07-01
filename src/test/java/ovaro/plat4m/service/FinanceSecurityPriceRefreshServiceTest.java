package ovaro.plat4m.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ovaro.plat4m.config.ApplicationProperties;
import ovaro.plat4m.domain.FinanceSecurity;
import ovaro.plat4m.domain.FinanceSecurityInvestmentSummary;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceSecurityPriceRepository;
import ovaro.plat4m.repository.UserRepository;
import ovaro.plat4m.service.dto.FinanceSecurityPriceRefreshRequestDTO;
import ovaro.plat4m.service.dto.FinanceSecurityPriceRefreshResultDTO;

@ExtendWith(MockitoExtension.class)
class FinanceSecurityPriceRefreshServiceTest {

    @Mock
    private FinanceSecurityService financeSecurityService;

    @Mock
    private FinanceTransactionService financeTransactionService;

    @Mock
    private FinanceSecurityPriceRepository financeSecurityPriceRepository;

    @Mock
    private FinanceMarketDataService financeMarketDataService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIService aiService;

    private FinanceSecurityPriceRefreshService service;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getMarketData().setEnabled(true);
        applicationProperties.getMarketData().getTwelveData().setApiKey("test-key");

        service = new FinanceSecurityPriceRefreshService(
            applicationProperties,
            financeSecurityService,
            financeTransactionService,
            financeSecurityPriceRepository,
            financeMarketDataService,
            userRepository,
            aiService,
            new ObjectMapper()
        );
    }

    @Test
    void refreshQuotesInsertsNewPriceWhenMissing() {
        User user = user();
        FinanceUserSecurity holding = eligibleHolding("AUD");
        when(financeSecurityService.getUserSecurities(user, null)).thenReturn(List.of(holding));
        when(financeTransactionService.getFinanceSecurityInvestmentTransactions(user, false, null)).thenReturn(
            List.of(summaryFor(holding, "BHP", "AU:BHP"))
        );
        when(financeMarketDataService.fetchPreviousClose(any(FinanceSecurity.class))).thenReturn(
            Optional.of(snapshot(holding.getSecurity().getSymbol()))
        );
        when(financeSecurityPriceRepository.findBySymbolAndDate(any(), any())).thenReturn(Optional.empty());
        when(financeSecurityPriceRepository.save(any(FinanceSecurityPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinanceSecurityPriceRefreshResultDTO result = service.refreshQuotes(user, new FinanceSecurityPriceRefreshRequestDTO());

        assertThat(result.getRequestedCount()).isEqualTo(1);
        assertThat(result.getRefreshedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("refreshed");
        assertThat(result.getItems().get(0).getRequestedSymbol()).isEqualTo("BHP");
        assertThat(result.getItems().get(0).getRequestedExchangeMic()).isEqualTo("XASX");

        ArgumentCaptor<FinanceSecurityPrice> captor = ArgumentCaptor.forClass(FinanceSecurityPrice.class);
        verify(financeSecurityPriceRepository).save(captor.capture());
        FinanceSecurityPrice saved = captor.getValue();
        assertThat(saved.getSymbol()).isEqualTo("BHP");
        assertThat(saved.getPrice()).isEqualByComparingTo("25.40");
        assertThat(saved.getClose()).isEqualByComparingTo("25.40");
        assertThat(saved.getDate().toLocalDate()).isEqualTo(LocalDate.of(2026, 6, 18));
    }

    @Test
    void refreshQuotesReusesExistingPriceRowForSameDate() {
        User user = user();
        FinanceUserSecurity holding = eligibleHolding("USD");
        FinanceSecurityPrice existing = new FinanceSecurityPrice();
        existing.setSymbol("MSFT");
        existing.setDate(LocalDate.of(2026, 6, 18).atStartOfDay().atZone(java.time.ZoneOffset.UTC));

        holding.getSecurity().setSymbol("MSFT");
        when(financeSecurityService.getUserSecurities(user, null)).thenReturn(List.of(holding));
        when(financeTransactionService.getFinanceSecurityInvestmentTransactions(user, false, null)).thenReturn(
            List.of(summaryFor(holding, "MSFT", "US:MSFT"))
        );
        when(financeMarketDataService.fetchPreviousClose(any(FinanceSecurity.class))).thenReturn(Optional.of(snapshot("MSFT")));
        when(financeSecurityPriceRepository.findBySymbolAndDate(any(), any())).thenReturn(Optional.of(existing));
        when(financeSecurityPriceRepository.save(existing)).thenReturn(existing);

        FinanceSecurityPriceRefreshResultDTO result = service.refreshQuotes(user, new FinanceSecurityPriceRefreshRequestDTO());

        assertThat(result.getRefreshedCount()).isEqualTo(1);
        assertThat(result.getItems())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getPreviousPrice()).isNull();
                assertThat(item.getPriceDeltaValue()).isNull();
                assertThat(item.getPriceDeltaPercent()).isNull();
            });
        verify(financeSecurityPriceRepository).save(existing);
        assertThat(existing.getPrice()).isEqualByComparingTo("25.40");
    }

    @Test
    void refreshQuotesSkipsUnsupportedCurrency() {
        User user = user();
        FinanceUserSecurity holding = eligibleHolding("EUR");
        when(financeSecurityService.getUserSecurities(user, null)).thenReturn(List.of(holding));
        when(financeTransactionService.getFinanceSecurityInvestmentTransactions(user, false, null)).thenReturn(
            List.of(summaryFor(holding, "BHP", "AU:BHP"))
        );

        FinanceSecurityPriceRefreshResultDTO result = service.refreshQuotes(user, new FinanceSecurityPriceRefreshRequestDTO());

        assertThat(result.getRequestedCount()).isEqualTo(1);
        assertThat(result.getRefreshedCount()).isZero();
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getItems())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getStatus()).isEqualTo("skipped");
                assertThat(item.getMessage()).contains("Requested code BHP skipped.");
                assertThat(item.getMessage()).contains("currency=EUR.");
            });
        verify(financeMarketDataService, never()).fetchPreviousClose(any());
        verify(financeSecurityPriceRepository, never()).save(any());
    }

    @Test
    void refreshQuotesIncludesRequestedCodeWhenProviderFails() {
        User user = user();
        FinanceUserSecurity holding = eligibleHolding("AUD");
        when(financeSecurityService.getUserSecurities(user, null)).thenReturn(List.of(holding));
        when(financeTransactionService.getFinanceSecurityInvestmentTransactions(user, false, null)).thenReturn(
            List.of(summaryFor(holding, "BHP", "AU:BHP"))
        );
        when(financeMarketDataService.fetchPreviousClose(any(FinanceSecurity.class))).thenThrow(
            new IllegalStateException("Twelve Data EOD request failed with HTTP 404.")
        );

        FinanceSecurityPriceRefreshResultDTO result = service.refreshQuotes(user, new FinanceSecurityPriceRefreshRequestDTO());

        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getItems())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getStatus()).isEqualTo("failed");
                assertThat(item.getMessage()).isEqualTo("Requested code BHP failed: Twelve Data EOD request failed with HTTP 404.");
            });
    }

    @Test
    void refreshQuotesUsesLinkedSummarySymbolWhenHoldingUsesSyntheticId() {
        User user = user();
        FinanceUserSecurity holding = eligibleHolding("AUD");
        holding.setSecurity(null);
        holding.setSymbol("715bbe5e-2628-4da5-8737-d0f7e10a8148:88");

        when(financeSecurityService.getUserSecurities(user, null)).thenReturn(List.of(holding));
        when(financeTransactionService.getFinanceSecurityInvestmentTransactions(user, false, null)).thenReturn(
            List.of(summaryFor(holding, "715bbe5e-2628-4da5-8737-d0f7e10a8148:88", "AU:BHP"))
        );
        when(financeMarketDataService.fetchPreviousClose(any(FinanceSecurity.class))).thenReturn(Optional.of(snapshot("BHP")));
        when(financeSecurityPriceRepository.findBySymbolAndDate(any(), any())).thenReturn(Optional.empty());
        when(financeSecurityPriceRepository.save(any(FinanceSecurityPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinanceSecurityPriceRefreshResultDTO result = service.refreshQuotes(user, new FinanceSecurityPriceRefreshRequestDTO());

        assertThat(result.getRefreshedCount()).isEqualTo(1);

        ArgumentCaptor<FinanceSecurity> securityCaptor = ArgumentCaptor.forClass(FinanceSecurity.class);
        verify(financeMarketDataService).fetchPreviousClose(securityCaptor.capture());
        assertThat(securityCaptor.getValue().getSymbol()).isEqualTo("BHP");
        assertThat(securityCaptor.getValue().getExchangeMic()).isEqualTo("XASX");

        ArgumentCaptor<FinanceSecurityPrice> priceCaptor = ArgumentCaptor.forClass(FinanceSecurityPrice.class);
        verify(financeSecurityPriceRepository).save(priceCaptor.capture());
        assertThat(priceCaptor.getValue().getSymbol()).isEqualTo("715bbe5e-2628-4da5-8737-d0f7e10a8148:88");
    }

    @Test
    void refreshQuotesOnlyProcessesOpenHoldings() {
        User user = user();
        FinanceUserSecurity openHolding = eligibleHolding("AUD");
        FinanceUserSecurity closedHolding = eligibleHolding("AUD");

        when(financeSecurityService.getUserSecurities(user, null)).thenReturn(List.of(openHolding, closedHolding));
        when(financeTransactionService.getFinanceSecurityInvestmentTransactions(user, false, null)).thenReturn(
            List.of(summaryFor(openHolding, "BHP", "AU:BHP"))
        );
        when(financeMarketDataService.fetchPreviousClose(any(FinanceSecurity.class))).thenReturn(Optional.of(snapshot("BHP")));
        when(financeSecurityPriceRepository.findBySymbolAndDate(any(), any())).thenReturn(Optional.empty());
        when(financeSecurityPriceRepository.save(any(FinanceSecurityPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinanceSecurityPriceRefreshResultDTO result = service.refreshQuotes(user, new FinanceSecurityPriceRefreshRequestDTO());

        assertThat(result.getRequestedCount()).isEqualTo(1);
        assertThat(result.getProcessedCount()).isEqualTo(1);
        verify(financeMarketDataService).fetchPreviousClose(any(FinanceSecurity.class));
    }

    @Test
    void refreshQuotesPassesMicToAiAndRejectsMismatchedMarket() {
        User user = user();
        FinanceUserSecurity holding = eligibleHolding("AUD");
        when(financeSecurityService.getUserSecurities(user, null)).thenReturn(List.of(holding));
        when(financeTransactionService.getFinanceSecurityInvestmentTransactions(user, false, null)).thenReturn(
            List.of(summaryFor(holding, "BHP", "AU:BHP"))
        );
        when(aiService.generateText(any(), any(), contains("exchangeMic: XASX"))).thenReturn(
            Optional.of(
                new AIService.AiTextResponse(
                    "gemini-test",
                    """
                    {
                      "quotes": [
                        {
                          "requestId": "%s",
                          "symbol": "BHP",
                          "exchangeMic": "XNYS",
                          "price": 99.99,
                          "priceDate": "2026-06-18"
                        }
                      ]
                    }
                    """.formatted(holding.getId())
                )
            )
        );
        when(financeMarketDataService.fetchPreviousClose(any(FinanceSecurity.class))).thenReturn(Optional.of(snapshot("BHP")));
        when(financeSecurityPriceRepository.findBySymbolAndDate(any(), any())).thenReturn(Optional.empty());
        when(financeSecurityPriceRepository.save(any(FinanceSecurityPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinanceSecurityPriceRefreshResultDTO result = service.refreshQuotes(user, new FinanceSecurityPriceRefreshRequestDTO());

        assertThat(result.getItems())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getStatus()).isEqualTo("refreshed");
                assertThat(item.getPrice()).isEqualByComparingTo("25.40");
                assertThat(item.getMessage()).isEqualTo("Previous close refreshed.");
            });
        verify(aiService).generateText(any(), any(), contains("Do not substitute another market"));
    }

    @Test
    void refreshQuotesCallsAiOnceForAllEligibleHoldings() {
        User user = user();
        FinanceUserSecurity bhpHolding = eligibleHolding("BHP", "AUD", "XASX");
        FinanceUserSecurity cbaHolding = eligibleHolding("CBA", "AUD", "XASX");
        when(financeSecurityService.getUserSecurities(user, null)).thenReturn(List.of(bhpHolding, cbaHolding));
        when(financeTransactionService.getFinanceSecurityInvestmentTransactions(user, false, null)).thenReturn(
            List.of(summaryFor(bhpHolding, "BHP", "AU:BHP"), summaryFor(cbaHolding, "CBA", "AU:CBA"))
        );
        when(
            aiService.generateText(
                any(),
                any(),
                argThat(
                    prompt ->
                        prompt.contains("requestId: " + bhpHolding.getId()) &&
                        prompt.contains("requestId: " + cbaHolding.getId()) &&
                        prompt.contains("symbol: BHP") &&
                        prompt.contains("symbol: CBA")
                )
            )
        ).thenReturn(
            Optional.of(
                new AIService.AiTextResponse(
                    "gemini-test",
                    """
                    {
                      "quotes": [
                        {
                          "requestId": "%s",
                          "symbol": "BHP",
                          "exchangeMic": "XASX",
                          "price": 40.10,
                          "priceDate": "2026-06-18"
                        },
                        {
                          "requestId": "%s",
                          "symbol": "CBA",
                          "exchangeMic": "XASX",
                          "price": 155.25,
                          "priceDate": "2026-06-18"
                        }
                      ]
                    }
                    """.formatted(bhpHolding.getId(), cbaHolding.getId())
                )
            )
        );
        when(financeMarketDataService.fetchPreviousClose(any(FinanceSecurity.class))).thenReturn(Optional.of(snapshot("BHP")));
        when(financeSecurityPriceRepository.findBySymbolAndDate(any(), any())).thenReturn(Optional.empty());
        when(financeSecurityPriceRepository.save(any(FinanceSecurityPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinanceSecurityPriceRefreshResultDTO result = service.refreshQuotes(user, new FinanceSecurityPriceRefreshRequestDTO());

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems()).allSatisfy(item -> assertThat(item.getAiPrice()).isNotNull());
        verify(aiService).generateText(
            any(),
            any(),
            argThat(prompt -> prompt.contains("requestId: " + bhpHolding.getId()) && prompt.contains("requestId: " + cbaHolding.getId()))
        );
    }

    @Test
    void refreshQuotesRejectsDisabledProvider() {
        ApplicationProperties disabledProperties = new ApplicationProperties();
        disabledProperties.getMarketData().setEnabled(false);
        disabledProperties.getMarketData().getTwelveData().setApiKey("test-key");

        FinanceSecurityPriceRefreshService disabledService = new FinanceSecurityPriceRefreshService(
            disabledProperties,
            financeSecurityService,
            financeTransactionService,
            financeSecurityPriceRepository,
            financeMarketDataService,
            userRepository,
            aiService,
            new ObjectMapper()
        );

        assertThatThrownBy(() -> disabledService.refreshQuotes(user(), new FinanceSecurityPriceRefreshRequestDTO()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Market data refresh is disabled.");
    }

    private User user() {
        User user = new User();
        user.setLogin("quote-user");
        user.setGuid(UUID.randomUUID());
        return user;
    }

    private FinanceUserSecurity eligibleHolding(String currencyCode) {
        return eligibleHolding("BHP", currencyCode, "XASX");
    }

    private FinanceUserSecurity eligibleHolding(String symbol, String currencyCode, String exchangeMic) {
        FinanceSecurity security = new FinanceSecurity();
        security.setSymbol(symbol);
        security.setCurrencyCode(currencyCode);
        security.setExchangeMic(exchangeMic);

        FinanceUserSecurity userSecurity = new FinanceUserSecurity();
        userSecurity.setId(UUID.randomUUID());
        userSecurity.setSecurity(security);
        userSecurity.setCurrencyCode(currencyCode);
        userSecurity.setSymbol(security.getSymbol());
        return userSecurity;
    }

    private FinanceMarketDataService.MarketQuoteSnapshot snapshot(String symbol) {
        FinanceMarketDataService.MarketQuoteSnapshot snapshot = new FinanceMarketDataService.MarketQuoteSnapshot();
        snapshot.setSymbol(symbol);
        snapshot.setPriceDate(LocalDate.of(2026, 6, 18));
        snapshot.setPrice(new BigDecimal("25.40"));
        snapshot.setClose(new BigDecimal("25.40"));
        snapshot.setOpen(new BigDecimal("25.10"));
        snapshot.setHigh(new BigDecimal("25.70"));
        snapshot.setLow(new BigDecimal("24.90"));
        snapshot.setFetchedAt(ZonedDateTime.now(java.time.ZoneOffset.UTC));
        return snapshot;
    }

    private FinanceSecurityInvestmentSummary summaryFor(FinanceUserSecurity holding, String symbol, String userSymbol) {
        return new FinanceSecurityInvestmentSummary() {
            @Override
            public String getAccountId() {
                return null;
            }

            @Override
            public void setAccountId(String account_id) {}

            @Override
            public String getAccountName() {
                return null;
            }

            @Override
            public void setAccountName(String account_name) {}

            @Override
            public String getSecurityId() {
                return holding.getId().toString();
            }

            @Override
            public void setSecurityId(String security_id) {}

            @Override
            public String getName() {
                return holding.getName();
            }

            @Override
            public void setName(String name) {}

            @Override
            public String getSymbol() {
                return symbol;
            }

            @Override
            public void setSymbol(String symbol) {}

            @Override
            public String getUserSymbol() {
                return userSymbol;
            }

            @Override
            public void setUserSymbol(String userSymbol) {}

            @Override
            public Double getAddSec() {
                return 1d;
            }

            @Override
            public void setAddSec(Double addSec) {}

            @Override
            public Double getRemoveSec() {
                return 0d;
            }

            @Override
            public void setRemoveSec(Double removeSec) {}

            @Override
            public String getCurrencyCode() {
                return holding.getCurrencyCode();
            }

            @Override
            public void setCurrencyCode(String currencyCode) {}

            @Override
            public String getSector() {
                return null;
            }

            @Override
            public void setSector(String sector) {}

            @Override
            public String getIndustry() {
                return null;
            }

            @Override
            public void setIndustry(String industry) {}

            @Override
            public String getExchangeName() {
                return "ASX";
            }

            @Override
            public void setExchangeName(String exchangeName) {}

            @Override
            public String getExchangeMic() {
                return "XASX";
            }

            @Override
            public void setExchangeMic(String exchangeMic) {}

            @Override
            public Integer getSecurityType() {
                return holding.getType();
            }

            @Override
            public void setSecurityType(Integer securityType) {}
        };
    }
}
