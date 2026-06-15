package ovaro.plat4m.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.AIService.AiServiceCallType;
import ovaro.plat4m.service.dto.AiAssistantMessageDTO;
import ovaro.plat4m.service.dto.AiAssistantQueryDTO;
import ovaro.plat4m.service.dto.AiAssistantResponseDTO;
import ovaro.plat4m.service.dto.FinanceAccountDTO;
import ovaro.plat4m.service.dto.FinanceIndicatorDTO;
import ovaro.plat4m.service.dto.FinanceIndicatorsDTO;
import ovaro.plat4m.service.dto.FinanceManageCategoryDTO;
import ovaro.plat4m.service.dto.FinanceManagePayeeDTO;
import ovaro.plat4m.service.dto.FinanceSecurityHoldingDTO;
import ovaro.plat4m.service.dto.FinanceTransactionRowDTO;
import ovaro.plat4m.web.rest.WebUIUtils;
import ovaro.plat4m.web.rest.errors.BadRequestAlertException;

@Service
@Transactional(readOnly = true)
public class AiAssistantService {

    private static final int MAX_CATEGORIES = 250;
    private static final int MAX_PAYEES = 250;
    private static final int MAX_HOLDINGS = 200;
    private static final int MAX_TRANSACTIONS = 80;
    private static final List<String> DASHBOARD_PERIODS = List.of("", "1D", "1W", "1M", "1Q", "1Y", "3Y", "5Y", "10Y");

    private final AIService aiService;
    private final FinanceAccountService financeAccountService;
    private final FinanceTransactionService financeTransactionService;
    private final FinanceFXService financeFXService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AiAssistantService(
        AIService aiService,
        FinanceAccountService financeAccountService,
        FinanceTransactionService financeTransactionService,
        FinanceFXService financeFXService,
        UserService userService,
        ObjectMapper objectMapper
    ) {
        this.aiService = aiService;
        this.financeAccountService = financeAccountService;
        this.financeTransactionService = financeTransactionService;
        this.financeFXService = financeFXService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public AiAssistantResponseDTO query(AiAssistantQueryDTO query) throws IOException {
        User user = getCurrentUser();
        Map<String, Object> context = buildAssistantContext(user);
        List<String> contextSources = List.of(
            "Dashboard metrics",
            "Accounts",
            "Categories",
            "Payees",
            "Portfolio holdings",
            "FX rates",
            "Recent transactions"
        );

        String prompt = buildPrompt(query, context);
        AiAssistantResponseDTO responseDTO = new AiAssistantResponseDTO();
        responseDTO.setContextSources(contextSources);

        aiService.generateText(AiServiceCallType.AI_ASSISTANT, prompt).ifPresentOrElse(
            response -> {
                responseDTO.setAiConfigured(true);
                responseDTO.setModel(response.model());
                responseDTO.setAnswer(response.text());
            },
            () -> {
                responseDTO.setAiConfigured(false);
                responseDTO.setAnswer(
                    "The AI assistant is ready, but Gemini is not configured yet. Add a Gemini API key in Settings > AI, then ask again."
                );
            }
        );

        return responseDTO;
    }

    private Map<String, Object> buildAssistantContext(User user) throws IOException {
        List<FinanceAccountDTO> accounts = financeAccountService.getAccountsOptimised(user, true, null, false);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("asOfDate", LocalDate.now());
        context.put("localCurrency", user.getLocalCurrency());
        context.put("dashboardMetrics", buildDashboardMetrics(user, accounts));
        context.put("accounts", compactAccounts(accounts));
        context.put("categories", compactCategories(limit(financeTransactionService.getManagedCategories(user), MAX_CATEGORIES)));
        context.put("payees", compactPayees(limit(financeTransactionService.getManagedPayees(user, false), MAX_PAYEES)));
        context.put(
            "portfolioHoldings",
            compactHoldings(limitPortfolio(financeAccountService.investmentAccountHoldings(user, null, false, LocalDate.now())))
        );
        context.put("fxRates", compactFxRates(financeFXService.getLatestFXAll()));
        context.put("recentTransactions", compactTransactions(recentTransactions(user, accounts)));
        return context;
    }

    private Map<String, FinanceIndicatorsDTO> buildDashboardMetrics(User user, List<FinanceAccountDTO> accountsNow) {
        Map<String, FinanceIndicatorsDTO> metricsByPeriod = new LinkedHashMap<>();

        for (String period : DASHBOARD_PERIODS) {
            List<FinanceIndicatorDTO> indicators = new ArrayList<>();
            financeAccountService.processBasicAccountValues(accountsNow, indicators);
            financeAccountService.processAccountValuesByType(accountsNow, indicators);

            if (!period.isBlank()) {
                LocalDate comparisonDate = WebUIUtils.getDateFromPeriod(period);
                List<FinanceAccountDTO> accountsOnDate = financeAccountService.getAccountsOptimised(user, true, comparisonDate, true);
                financeAccountService.updateBasicAccountValuesWithDelta(accountsOnDate, comparisonDate, indicators);
                financeAccountService.updateAccountValuesByTypeWithDelta(accountsOnDate, comparisonDate, indicators);
            }

            FinanceIndicatorsDTO dto = new FinanceIndicatorsDTO();
            dto.setIndicators(indicators);
            metricsByPeriod.put(period.isBlank() ? "current" : period, dto);
        }

        return metricsByPeriod;
    }

    private List<FinanceTransactionRowDTO> recentTransactions(User user, List<FinanceAccountDTO> accounts) {
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date", "number", "id"));
        List<FinanceTransactionRowDTO> transactions = new ArrayList<>();

        for (FinanceAccountDTO account : accounts) {
            if (transactions.size() >= MAX_TRANSACTIONS) {
                break;
            }
            transactions.addAll(
                financeTransactionService.getTransactionsPaging(user, account.getId().toString(), pageRequest, null).getContent()
            );
        }

        return transactions
            .stream()
            .sorted((left, right) -> {
                if (left.getDate() == null && right.getDate() == null) {
                    return 0;
                }
                if (left.getDate() == null) {
                    return 1;
                }
                if (right.getDate() == null) {
                    return -1;
                }
                return right.getDate().compareTo(left.getDate());
            })
            .limit(MAX_TRANSACTIONS)
            .toList();
    }

    private List<Map<String, Object>> compactAccounts(List<FinanceAccountDTO> accounts) {
        return accounts
            .stream()
            .map(account ->
                mapOf(
                    "id",
                    account.getId(),
                    "name",
                    account.getName(),
                    "type",
                    account.getAccountType(),
                    "currency",
                    account.getCurrencyCode(),
                    "balance",
                    account.getBalance(),
                    "fxRateToLocal",
                    account.getFxRateToLocal()
                )
            )
            .toList();
    }

    private List<Map<String, Object>> compactCategories(List<FinanceManageCategoryDTO> categories) {
        return categories
            .stream()
            .map(category ->
                mapOf(
                    "id",
                    category.getId(),
                    "name",
                    category.getName(),
                    "displayName",
                    category.getDisplayName(),
                    "parentName",
                    category.getParentName(),
                    "classificationId",
                    category.getClassificationId(),
                    "comment",
                    category.getComment()
                )
            )
            .toList();
    }

    private List<Map<String, Object>> compactPayees(List<FinanceManagePayeeDTO> payees) {
        return payees
            .stream()
            .map(payee -> mapOf("id", payee.getId(), "name", payee.getName(), "parentId", payee.getParentId(), "hidden", payee.getHidden()))
            .toList();
    }

    private List<Map<String, Object>> compactHoldings(List<FinanceSecurityHoldingDTO> holdings) {
        return holdings
            .stream()
            .map(holding ->
                mapOf(
                    "id",
                    holding.getId(),
                    "name",
                    holding.getName(),
                    "symbol",
                    holding.getSymbol(),
                    "accountName",
                    holding.getAccountName(),
                    "currency",
                    holding.getCurrencyCode(),
                    "quantity",
                    holding.getQuantity(),
                    "price",
                    holding.getPrice(),
                    "value",
                    holding.getValue(),
                    "fxRateToLocal",
                    holding.getFxRateToLocal(),
                    "sector",
                    holding.getSector(),
                    "industry",
                    holding.getIndustry()
                )
            )
            .toList();
    }

    private List<Map<String, Object>> compactFxRates(List<ovaro.plat4m.domain.FinanceFX> rates) {
        return rates
            .stream()
            .map(rate -> mapOf("from", rate.getFromIsoCode(), "to", rate.getToIsoCode(), "rate", rate.getRate(), "date", rate.getDate()))
            .toList();
    }

    private List<Map<String, Object>> compactTransactions(List<FinanceTransactionRowDTO> transactions) {
        return transactions
            .stream()
            .<Map<String, Object>>map(transaction ->
                mapOf(
                    "id",
                    transaction.getId(),
                    "date",
                    transaction.getDate(),
                    "accountId",
                    transaction.getAccountId(),
                    "payee",
                    transaction.getPayeeName(),
                    "category",
                    buildTransactionCategory(transaction),
                    "who",
                    transaction.getWhoName(),
                    "memo",
                    transaction.getMemo(),
                    "amount",
                    transaction.getAmount(),
                    "tags",
                    transaction.getTagsDisplay()
                )
            )
            .toList();
    }

    private String buildTransactionCategory(FinanceTransactionRowDTO transaction) {
        if (Boolean.TRUE.equals(transaction.getSplitParent())) {
            return "Split / Multiple Categories";
        }
        if (transaction.getCategoryName() == null || transaction.getCategoryName().isBlank()) {
            return null;
        }
        if (transaction.getParentCategoryName() == null || transaction.getParentCategoryName().isBlank()) {
            return transaction.getCategoryName();
        }
        return transaction.getParentCategoryName() + ": " + transaction.getCategoryName();
    }

    private String buildPrompt(AiAssistantQueryDTO query, Map<String, Object> context) throws JsonProcessingException {
        return """
        You are the Plat4m Finance AI Assistant. Answer using only the supplied read-only app context.
        Be concise, professional, and practical. If the context does not contain enough data, say what is missing.
        Do not claim to have made edits, created transactions, changed categories, updated payees, or modified data.
        Prefer currency-aware answers and mention dates/periods when comparing metrics.

        Recent conversation:
        %s

        User question:
        %s

        Read-only app context JSON:
        %s
        """.formatted(formatHistory(query.getHistory()), query.getMessage().trim(), objectMapper.writeValueAsString(context));
    }

    private String formatHistory(List<AiAssistantMessageDTO> history) {
        if (history == null || history.isEmpty()) {
            return "No previous messages.";
        }

        return history
            .stream()
            .filter(message -> message.getRole() != null && message.getContent() != null && !message.getContent().isBlank())
            .skip(Math.max(0, history.size() - 8))
            .map(message -> message.getRole() + ": " + message.getContent())
            .toList()
            .toString();
    }

    private <T> List<T> limit(List<T> source, int maxSize) {
        if (source == null) {
            return List.of();
        }
        return source.stream().limit(maxSize).toList();
    }

    private List<FinanceSecurityHoldingDTO> limitPortfolio(Collection<FinanceSecurityHoldingDTO> holdings) {
        if (holdings == null) {
            return List.of();
        }
        return holdings.stream().limit(MAX_HOLDINGS).toList();
    }

    private Map<String, Object> mapOf(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private User getCurrentUser() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user login not found", "aiAssistant", "currentusernotfound")
        );
        return userService
            .getUserWithAuthoritiesByLogin(userLogin)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", "aiAssistant", "currentusernotfound"));
    }
}
