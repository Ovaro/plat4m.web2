package ovaro.plat4m.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceSecurityPriceRepository;
import ovaro.plat4m.service.FinanceLotService;
import ovaro.plat4m.service.FinanceSecurityIntegrationService;
import ovaro.plat4m.service.FinanceSecurityPriceRefreshService;
import ovaro.plat4m.service.FinanceSecurityService;
import ovaro.plat4m.service.FinanceTransactionService;
import ovaro.plat4m.service.UserService;
import ovaro.plat4m.service.dto.FinanceSecurityPriceRefreshRequestDTO;
import ovaro.plat4m.service.dto.FinanceSecurityPriceRefreshResultDTO;

@ExtendWith(MockitoExtension.class)
class FinanceSecurityResourceTest {

    @Mock
    private UserService userService;

    @Mock
    private FinanceSecurityService financeSecurityService;

    @Mock
    private FinanceTransactionService financeTransactionService;

    @Mock
    private FinanceSecurityIntegrationService financeSecurityIntegrationService;

    @Mock
    private FinanceSecurityPriceRefreshService financeSecurityPriceRefreshService;

    @Mock
    private FinanceLotService financeLotService;

    @Mock
    private FinanceSecurityPriceRepository financeSecurityPriceRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        FinanceSecurityResource resource = new FinanceSecurityResource(
            userService,
            financeSecurityService,
            financeTransactionService,
            financeSecurityIntegrationService,
            financeLotService,
            financeSecurityPriceRefreshService,
            financeSecurityPriceRepository
        );

        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(resource).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("quote-user", "password"));

        User user = new User();
        user.setGuid(UUID.randomUUID());
        user.setLogin("quote-user");
        when(userService.getUserWithAuthoritiesByLogin("quote-user")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refreshSecurityPricesReturnsRefreshCounts() throws Exception {
        FinanceSecurityPriceRefreshResultDTO result = new FinanceSecurityPriceRefreshResultDTO();
        result.setRequestedCount(2);
        result.setRefreshedCount(1);
        when(financeSecurityPriceRefreshService.refreshQuotes(any(), any())).thenReturn(result);

        FinanceSecurityPriceRefreshRequestDTO request = new FinanceSecurityPriceRefreshRequestDTO();
        request.setAccountId("account-1");

        mockMvc
            .perform(
                post("/api/security-prices/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestedCount").value(2))
            .andExpect(jsonPath("$.refreshedCount").value(1));
    }

    @Test
    void refreshSecurityPricesReturnsServiceUnavailableWhenProviderDisabled() throws Exception {
        when(financeSecurityPriceRefreshService.refreshQuotes(any(), any())).thenThrow(
            new IllegalStateException("Market data refresh is disabled.")
        );

        mockMvc
            .perform(post("/api/security-prices/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isServiceUnavailable());
    }
}
