package ovaro.plat4m.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceAccountType;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinanceInstitution;
import ovaro.plat4m.domain.FinanceInvestmentActivityType;
import ovaro.plat4m.domain.FinanceLot;
import ovaro.plat4m.domain.FinancePayee;
import ovaro.plat4m.domain.FinanceSecurity;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceCategoryRepository;
import ovaro.plat4m.repository.FinanceInstitutionRepository;
import ovaro.plat4m.repository.FinanceLotRepository;
import ovaro.plat4m.repository.FinancePayeeRepository;
import ovaro.plat4m.repository.FinanceSecurityPriceRepository;
import ovaro.plat4m.repository.FinanceSecurityRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.FinanceUserSecurityRepository;
import ovaro.plat4m.repository.SourceLinkRepository;
import ovaro.plat4m.service.AIService.AiServiceCallType;
import ovaro.plat4m.service.dto.FinanceDemoDataAccountSpecDTO;
import ovaro.plat4m.service.dto.FinanceDemoDataBatchDTO;
import ovaro.plat4m.service.dto.FinanceDemoDataDeleteResultDTO;
import ovaro.plat4m.service.dto.FinanceDemoDataOptionsDTO;
import ovaro.plat4m.service.dto.FinanceDemoDataRequestDTO;
import ovaro.plat4m.service.dto.FinanceDemoDataResultDTO;

@Service
@Transactional
public class FinanceDemoDataService {

    private static final List<String> PERSONAS = List.of("Single", "Couple", "Family", "Retiree");
    private static final List<String> VARIANTS = List.of(
        "Stock Investors-Domestic",
        "Stock Investors-Domestic & International",
        "Real Estate Investors",
        "Home Owners",
        "Renters",
        "Private School",
        "No investments"
    );
    private static final List<String> ACCOUNT_TYPES = List.of("bank", "credit", "loan", "asset", "investment");
    private static final ZoneId DEMO_ZONE = ZoneId.of("Australia/Brisbane");

    private final FinanceAccountRepository accountRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final FinancePayeeRepository payeeRepository;
    private final FinanceCategoryRepository categoryRepository;
    private final FinanceInstitutionRepository institutionRepository;
    private final FinanceSecurityRepository securityRepository;
    private final FinanceUserSecurityRepository userSecurityRepository;
    private final FinanceSecurityPriceRepository securityPriceRepository;
    private final FinanceLotRepository lotRepository;
    private final SourceLinkRepository sourceLinkRepository;
    private final FinanceCategoryTemplateService categoryTemplateService;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public FinanceDemoDataService(
        FinanceAccountRepository accountRepository,
        FinanceTransactionRepository transactionRepository,
        FinancePayeeRepository payeeRepository,
        FinanceCategoryRepository categoryRepository,
        FinanceInstitutionRepository institutionRepository,
        FinanceSecurityRepository securityRepository,
        FinanceUserSecurityRepository userSecurityRepository,
        FinanceSecurityPriceRepository securityPriceRepository,
        FinanceLotRepository lotRepository,
        SourceLinkRepository sourceLinkRepository,
        FinanceCategoryTemplateService categoryTemplateService,
        AIService aiService,
        ObjectMapper objectMapper
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.payeeRepository = payeeRepository;
        this.categoryRepository = categoryRepository;
        this.institutionRepository = institutionRepository;
        this.securityRepository = securityRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.securityPriceRepository = securityPriceRepository;
        this.lotRepository = lotRepository;
        this.sourceLinkRepository = sourceLinkRepository;
        this.categoryTemplateService = categoryTemplateService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    public FinanceDemoDataOptionsDTO options() {
        return new FinanceDemoDataOptionsDTO(PERSONAS, VARIANTS, ACCOUNT_TYPES, List.of(1, 2, 3, 5, 7, 10));
    }

    @Transactional(readOnly = true)
    public List<FinanceDemoDataBatchDTO> batches(User user) {
        String userGuid = user.getGuid().toString();
        Map<UUID, List<FinanceAccount>> accountsByBatch = new LinkedHashMap<>();
        for (FinanceAccount account : accountRepository.findAllByUserGuidAndGeneratedTrueOrderBySerialDateTimeDescNameAsc(userGuid)) {
            UUID batchId = account.getGeneratedBatchId();
            if (batchId != null) {
                accountsByBatch.computeIfAbsent(batchId, ignored -> new ArrayList<>()).add(account);
            }
        }

        List<FinanceDemoDataBatchDTO> batches = new ArrayList<>();
        for (Map.Entry<UUID, List<FinanceAccount>> entry : accountsByBatch.entrySet()) {
            UUID batchId = entry.getKey();
            List<FinanceAccount> accounts = entry.getValue();
            FinanceAccount firstAccount = accounts.get(0);
            FinanceDemoDataBatchDTO batch = new FinanceDemoDataBatchDTO();
            batch.setGeneratedBatchId(batchId);
            batch.setGeneratedLabel(firstAccount.getGeneratedLabel());
            batch.setCreatedAt(firstAccount.getSerialDateTime());
            batch.setAccountCount(accounts.size());
            batch.setTransactionCount(transactionRepository.countByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId));
            batch.setPayeeCount(payeeRepository.countByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId));
            batch.setSecurityCount(userSecurityRepository.countByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId));
            batch.setPriceCount(securityPriceRepository.countByGeneratedTrueAndGeneratedBatchId(batchId));
            batch.setLotCount(lotRepository.countByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId));
            batch.setAccountNames(accounts.stream().map(FinanceAccount::getName).toList());
            batches.add(batch);
        }
        return batches;
    }

    @Transactional(readOnly = true)
    public FinanceDemoDataResultDTO preview(User user, FinanceDemoDataRequestDTO request) {
        DemoPlan plan = buildPlan(user, request);
        FinanceDemoDataResultDTO result = new FinanceDemoDataResultDTO();
        result.setPreview(true);
        result.setGeneratedBatchId(plan.batchId());
        result.setGeneratedLabel(plan.label());
        result.setAccountCount(plan.accountPlans().size());
        result.setTransactionCount(plan.accountPlans().stream().mapToInt(AccountPlan::estimatedTransactions).sum());
        result.setPayeeCount(estimatedPayeeCount(plan));
        result.setSecurityCount(plan.includeInvestments() ? plannedSymbols(plan).size() : 0);
        result.setPriceCount(plan.includeInvestments() ? plannedSymbols(plan).size() : 0);
        result.setLotCount(plan.includeInvestments() ? plannedSymbols(plan).size() : 0);
        result.setAccountNames(plan.accountPlans().stream().map(AccountPlan::name).toList());
        result.setNotes(buildNotes(plan));
        return result;
    }

    public FinanceDemoDataDeleteResultDTO deleteBatch(User user, UUID batchId) {
        if (batchId == null) {
            throw new IllegalArgumentException("Generated batch id is required");
        }
        String userGuid = user.getGuid().toString();
        List<FinanceAccount> accounts = accountRepository.findAllByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId);
        if (accounts.isEmpty()) {
            throw new IllegalArgumentException("Generated batch not found");
        }

        FinanceDemoDataDeleteResultDTO result = new FinanceDemoDataDeleteResultDTO();
        result.setGeneratedBatchId(batchId);
        result.setGeneratedLabel(accounts.get(0).getGeneratedLabel());
        result.setAccountCount(accounts.size());
        result.setTransactionCount(transactionRepository.countByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId));
        result.setPayeeCount(payeeRepository.countByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId));
        result.setSecurityCount(userSecurityRepository.countByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId));
        result.setPriceCount(securityPriceRepository.countByGeneratedTrueAndGeneratedBatchId(batchId));
        result.setLotCount(lotRepository.countByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId));
        result.setInstitutionCount(institutionRepository.countByGeneratedTrueAndGeneratedBatchId(batchId));
        result.setSourceLinkCount(sourceLinkRepository.countByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId));

        sourceLinkRepository.deleteAllByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId);
        lotRepository.deleteAllByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId);
        transactionRepository.deleteAllByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId);
        accountRepository.deleteAllByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId);
        userSecurityRepository.deleteAllByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId);
        payeeRepository.deleteAllByUserGuidAndGeneratedTrueAndGeneratedBatchId(userGuid, batchId);
        institutionRepository.deleteAllByGeneratedTrueAndGeneratedBatchId(batchId);
        securityPriceRepository.deleteAllByGeneratedTrueAndGeneratedBatchId(batchId);
        return result;
    }

    public FinanceDemoDataResultDTO generate(User user, FinanceDemoDataRequestDTO request) {
        DemoPlan plan = buildPlan(user, request);
        categoryTemplateService.ensureDefaultCategoriesForUser(user);

        Map<String, FinanceCategory> categories = loadCategories(user);
        Map<String, FinancePayee> payees = new LinkedHashMap<>();
        FinanceInstitution institution = createInstitution(plan, "Commonwealth Bank", "landmark");
        List<FinanceAccount> accounts = new ArrayList<>();
        List<FinanceTransaction> transactions = new ArrayList<>();
        List<FinanceUserSecurity> userSecurities = new ArrayList<>();
        List<FinanceSecurityPrice> prices = new ArrayList<>();
        List<FinanceLot> lots = new ArrayList<>();

        List<AccountInstance> accountInstances = new ArrayList<>();
        for (AccountPlan accountPlan : plan.accountPlans()) {
            FinanceAccount account = createAccount(user, plan, accountPlan, institution);
            accounts.add(account);
            accountInstances.add(new AccountInstance(account, accountPlan));
        }

        FinanceAccount bank = firstAccount(accountInstances, "bank");

        for (AccountInstance instance : accountInstances) {
            FinanceAccount account = instance.account();
            AccountPlan accountPlan = instance.plan();
            switch (accountPlan.type()) {
                case "bank" -> transactions.addAll(createBankTransactions(user, plan, accountPlan, account, categories, payees));
                case "credit" -> {
                    transactions.addAll(createCreditTransactions(user, plan, accountPlan, account, categories, payees));
                    if (bank != null) {
                        transactions.addAll(createCreditCardRepayments(user, plan, accountPlan, bank, account, payees));
                    }
                }
                case "loan" -> {
                    transactions.addAll(createLoanTransactions(user, plan, account, categories, payees));
                    if (bank != null) {
                        transactions.addAll(createRepayments(user, plan, bank, account, "Westpac Home Loan", categories, payees));
                    }
                }
                case "investment" -> {
                    if (plan.includeInvestments()) {
                        InvestmentResult investmentResult = createInvestmentData(user, plan, accountPlan, account, categories, payees);
                        transactions.addAll(investmentResult.transactions());
                        userSecurities.addAll(investmentResult.userSecurities());
                        prices.addAll(investmentResult.prices());
                        lots.addAll(investmentResult.lots());
                    }
                }
                default -> {
                }
            }
        }
        if (bank != null) {
            transactions.addAll(createInvestmentFundingTransfers(user, plan, bank, accountInstances, transactions, payees));
            transactions.addAll(createSavingsSweeps(user, plan, bank, accountInstances, transactions, payees));
        }
        transactions.addAll(ensureBankAccountsStayPositive(user, plan, accountInstances, transactions, payees));

        payeeRepository.saveAll(payees.values());
        transactionRepository.saveAll(transactions);
        lotRepository.saveAll(lots);

        FinanceDemoDataResultDTO result = new FinanceDemoDataResultDTO();
        result.setGeneratedBatchId(plan.batchId());
        result.setGeneratedLabel(plan.label());
        result.setAccountCount(accounts.size());
        result.setTransactionCount(transactions.size());
        result.setPayeeCount(payees.size());
        result.setSecurityCount(userSecurities.size());
        result.setPriceCount(prices.size());
        result.setLotCount(lots.size());
        result.setAccountNames(accounts.stream().map(FinanceAccount::getName).toList());
        result.setNotes(buildNotes(plan));
        return result;
    }

    private DemoPlan buildPlan(User user, FinanceDemoDataRequestDTO request) {
        String persona = normalizePersona(request == null ? null : request.getPersona());
        String currencyCode = trimToNull(request == null ? null : request.getCurrencyCode());
        if (currencyCode == null) {
            currencyCode = trimToNull(user.getLocalCurrency());
        }
        if (currencyCode == null) {
            currencyCode = "AUD";
        }

        boolean noInvestments = bool(request == null ? null : request.getNoInvestments());
        boolean domesticStocks = !noInvestments && bool(request == null ? null : request.getDomesticStocks(), true);
        boolean internationalStocks = !noInvestments && bool(request == null ? null : request.getInternationalStocks());
        boolean realEstateInvestor = bool(request == null ? null : request.getRealEstateInvestor());
        boolean homeOwner = bool(request == null ? null : request.getHomeOwner()) || realEstateInvestor;
        boolean renter = bool(request == null ? null : request.getRenter()) && !homeOwner;
        boolean privateSchool = bool(request == null ? null : request.getPrivateSchool());
        int income = switch (persona) {
            case "Couple" -> 14500;
            case "Family" -> 16500;
            case "Retiree" -> 7200;
            default -> 8800;
        };
        if (privateSchool) {
            income += 3000;
        }
        if (realEstateInvestor) {
            income += 2500;
        }
        AiDemoParameters aiParameters = resolveAiDemoParameters(user, request, persona);
        income = applyAiIncomeAdjustment(income, aiParameters);
        int spendingRatePercent = clamp(request == null ? null : request.getSpendingRatePercent(), 30, 80, 55);

        UUID batchId = UUID.randomUUID();
        String label = "Sample Household - " + persona + " - " + LocalDate.now(DEMO_ZONE);
        List<AccountPlan> accountPlans = buildAccountPlans(
            request,
            persona,
            currencyCode,
            homeOwner,
            renter,
            realEstateInvestor,
            noInvestments
        );
        return new DemoPlan(
            batchId,
            label,
            persona,
            currencyCode,
            trimToNull(request == null ? null : request.getHints()),
            domesticStocks,
            internationalStocks,
            realEstateInvestor,
            homeOwner,
            renter,
            privateSchool,
            noInvestments,
            income,
            spendingRatePercent,
            aiParameters.note(),
            accountPlans
        );
    }

    private List<AccountPlan> buildAccountPlans(
        FinanceDemoDataRequestDTO request,
        String persona,
        String currencyCode,
        boolean homeOwner,
        boolean renter,
        boolean realEstateInvestor,
        boolean noInvestments
    ) {
        Map<String, FinanceDemoDataAccountSpecDTO> specs = new LinkedHashMap<>();
        if (request != null && request.getAccounts() != null) {
            for (FinanceDemoDataAccountSpecDTO spec : request.getAccounts()) {
                String type = normalizeAccountType(spec.getAccountType());
                if (type != null) {
                    specs.put(type, spec);
                }
            }
        }

        List<AccountPlan> plans = new ArrayList<>();
        addAccountPlan(plans, specs, "bank", "Everyday Banking", currencyCode, FinanceAccountType.BANKING.getValue(), bd(3500), true, 3);
        addAccountPlan(
            plans,
            specs,
            "credit",
            "Rewards Credit Card",
            currencyCode,
            FinanceAccountType.CREDIT_CARD.getValue(),
            bd(0),
            true,
            3
        );
        addAccountPlan(
            plans,
            specs,
            "loan",
            homeOwner ? "Home Mortgage" : "Personal Loan",
            currencyCode,
            FinanceAccountType.LOAN.getValue(),
            homeOwner ? bd(-620000) : bd(-28000),
            homeOwner || realEstateInvestor,
            5
        );
        addAccountPlan(
            plans,
            specs,
            "asset",
            realEstateInvestor ? "Investment Property" : (homeOwner ? "Family Home" : "Vehicle"),
            currencyCode,
            FinanceAccountType.ASSET.getValue(),
            realEstateInvestor ? bd(860000) : (homeOwner ? bd(950000) : bd(26000)),
            homeOwner || realEstateInvestor || "Family".equals(persona),
            1
        );
        addAccountPlan(
            plans,
            specs,
            "investment",
            "Share Portfolio",
            currencyCode,
            FinanceAccountType.INVESTMENT.getValue(),
            "Retiree".equals(persona) ? bd(420000) : bd(55000),
            !noInvestments,
            5
        );
        return plans;
    }

    private void addAccountPlan(
        List<AccountPlan> plans,
        Map<String, FinanceDemoDataAccountSpecDTO> specs,
        String type,
        String defaultName,
        String currencyCode,
        int financeType,
        BigDecimal defaultStartingBalance,
        boolean defaultInclude,
        int defaultYears
    ) {
        FinanceDemoDataAccountSpecDTO spec = specs.get(type);
        boolean include = spec == null ? defaultInclude : bool(spec.getInclude(), defaultInclude);
        if (!include) {
            return;
        }
        int years = clamp(spec == null ? defaultYears : spec.getYears(), 1, 10, defaultYears);
        BigDecimal startingValue = spec != null && spec.getStartingValue() != null ? spec.getStartingValue() : defaultStartingBalance;
        int stockCount = "investment".equals(type) ? clamp(spec == null ? null : spec.getStockCount(), 1, 12, 4) : 0;
        String tradingProfile = "investment".equals(type)
            ? normalizeTradingProfile(spec == null ? null : spec.getTradingProfile())
            : "buy_hold";
        int accountCount = clamp(spec == null ? null : spec.getAccountCount(), 1, 5, 1);
        for (int sequence = 1; sequence <= accountCount; sequence++) {
            String name = accountName(type, defaultName, realCompanyName(defaultName), sequence, accountCount);
            BigDecimal sequenceStartingValue = sequenceStartingValue(startingValue, type, sequence);
            plans.add(
                new AccountPlan(
                    type,
                    name,
                    currencyCode,
                    financeType,
                    sequenceStartingValue,
                    years,
                    density(spec),
                    stockCount,
                    sequence,
                    tradingProfile,
                    trimToNull(spec == null ? null : spec.getHints())
                )
            );
        }
    }

    private FinanceInstitution createInstitution(DemoPlan plan, String name, String icon) {
        FinanceInstitution institution = new FinanceInstitution();
        institution.setName(name);
        institution.setIcon(icon);
        institution.setComment("Primary banking relationship");
        institution.setSerialDateTime(ZonedDateTime.now(DEMO_ZONE));
        markGenerated(institution, plan);
        return institutionRepository.save(institution);
    }

    private FinanceAccount createAccount(User user, DemoPlan plan, AccountPlan accountPlan, FinanceInstitution institution) {
        FinanceAccount account = new FinanceAccount();
        account.setUserGuid(user.getGuid().toString());
        account.setName(accountPlan.name());
        account.setType(accountPlan.financeType());
        account.setCurrencyCode(accountPlan.currencyCode());
        account.setStartingBalance(accountPlan.startingBalance());
        account.setClosed(false);
        account.setFavourite("bank".equals(accountPlan.type()) || "investment".equals(accountPlan.type()));
        account.setDateOpened(LocalDate.now(DEMO_ZONE).minusYears(accountPlan.years()).withDayOfMonth(1));
        account.setSerialDateTime(ZonedDateTime.now(DEMO_ZONE));
        account.setInstitution(institution);
        if ("credit".equals(accountPlan.type())) {
            account.setCredit(true);
            account.setAmountLimit(bd(12000));
        }
        if ("bank".equals(accountPlan.type()) && accountPlan.sequence() > 1) {
            account.setInterestRate(savingsInterestRate(accountPlan));
            account.setDateInterestRateChanged(account.getDateOpened());
        }
        if ("loan".equals(accountPlan.type())) {
            account.setInterestRate(plan.homeOwner() ? 6.1 : 8.9);
            account.setMaxPayments(plan.homeOwner() ? 360 : 60);
        }
        markGenerated(account, plan);
        return accountRepository.save(account);
    }

    private List<FinanceTransaction> createBankTransactions(
        User user,
        DemoPlan plan,
        AccountPlan accountPlan,
        FinanceAccount account,
        Map<String, FinanceCategory> categories,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceTransaction> transactions = new ArrayList<>();
        int months = months(account);
        SpendingProfile spending = spendingProfile(plan);
        for (int index = 0; index < months; index++) {
            LocalDate month = month(account, index);
            if (accountPlan.sequence() != 1) {
                transactions.addAll(createSecondaryBankTransactions(user, plan, account, month, index, categories, payees));
                continue;
            }
            transactions.addAll(createIncomeTransactions(user, plan, account, month, months, index, categories, payees));
            if (plan.realEstateInvestor()) {
                transactions.add(
                    transaction(
                        user,
                        plan,
                        account,
                        month.withDayOfMonth(3),
                        bd(3100),
                        "REA Group Property Management",
                        category(categories, "Other Income", "Rent"),
                        payees,
                        "Rental property income"
                    )
                );
            }
            transactions.add(
                transaction(
                    user,
                    plan,
                    account,
                    month.withDayOfMonth(4),
                    monthlyAmount(bd(220), plan, index, "electricity").negate(),
                    "AGL Energy",
                    category(categories, "Electricity", "Bills"),
                    payees,
                    "Electricity account"
                )
            );
            transactions.add(
                transaction(
                    user,
                    plan,
                    account,
                    month.withDayOfMonth(7),
                    monthlyAmount(bd(140), plan, index, "mobile").negate(),
                    "Telstra",
                    category(categories, "Mobile & Cellular", "Bills"),
                    payees,
                    "Mobile plan"
                )
            );
            transactions.add(
                transaction(
                    user,
                    plan,
                    account,
                    month.withDayOfMonth(10),
                    monthlyAmount(bd(95), plan, index, "internet").negate(),
                    "Optus",
                    category(categories, "Internet", "Bills"),
                    payees,
                    "Home internet"
                )
            );
            transactions.add(
                transaction(
                    user,
                    plan,
                    account,
                    month.withDayOfMonth(8),
                    monthlyAmount(spending.insurance(), plan, index, "insurance").negate(),
                    "AAMI Insurance",
                    category(categories, "Insurance", "Bills"),
                    payees,
                    "Insurance premium"
                )
            );
            transactions.addAll(
                splitSpend(
                    user,
                    plan,
                    account,
                    month,
                    monthlyAmount(spending.transport(), plan, index, "transport"),
                    category(categories, "Public Transportation", "Auto & Transport"),
                    payees,
                    List.of("Translink", "Uber", "Wilson Parking", "Secure Parking"),
                    countFor(accountPlan, plan, index, "transport", 1, 3),
                    2,
                    24,
                    "Transport"
                )
            );
            if (plan.renter()) {
                transactions.add(
                    transaction(
                        user,
                        plan,
                        account,
                        month.withDayOfMonth(1),
                        monthlyAmount(spending.housing(), plan, index, "housing").negate(),
                        "Ray White",
                        category(categories, "Rent", "Bills"),
                        payees,
                        "Residential rent"
                    )
                );
            }
            if (plan.privateSchool()) {
                transactions.add(
                    transaction(
                        user,
                        plan,
                        account,
                        month.withDayOfMonth(12),
                        monthlyAmount(bd(1850), plan, index, "school-fees").negate(),
                        "Brisbane Grammar School",
                        category(categories, "Kids/Children", "Kids/Children"),
                        payees,
                        "Tuition fees"
                    )
                );
            }
            if ("Family".equals(plan.persona())) {
                transactions.add(
                    transaction(
                        user,
                        plan,
                        account,
                        month.withDayOfMonth(14),
                        monthlyAmount(bd(620), plan, index, "childcare").negate(),
                        "Goodstart Early Learning",
                        category(categories, "Kids/Children", "Kids/Children"),
                        payees,
                        "Childcare"
                    )
                );
            }
            if (plan.homeOwner()) {
                transactions.add(
                    transaction(
                        user,
                        plan,
                        account,
                        month.withDayOfMonth(18),
                        monthlyAmount(bd(520), plan, index, "council-rates").negate(),
                        "Brisbane City Council",
                        category(categories, "Council Rates", "Bills"),
                        payees,
                        "Council rates"
                    )
                );
                transactions.addAll(
                    splitSpend(
                        user,
                        plan,
                        account,
                        month,
                        monthlyAmount(bd(280), plan, index, "home-maintenance"),
                        category(categories, "Home Improvement", "Household"),
                        payees,
                        List.of("Bunnings Warehouse", "Mitre 10", "Reece Plumbing", "Jim's Mowing"),
                        countFor(accountPlan, plan, index, "home-maintenance", 1, 3),
                        12,
                        26,
                        "Home maintenance"
                    )
                );
            }
            if (plan.realEstateInvestor()) {
                transactions.add(
                    transaction(
                        user,
                        plan,
                        account,
                        month.withDayOfMonth(16),
                        monthlyAmount(bd(420), plan, index, "property-management").negate(),
                        "JLL Property Management",
                        category(categories, "Property Tax", "Investment Expenses"),
                        payees,
                        "Property management"
                    )
                );
            }
            if ("high".equals(accountPlan.density())) {
                transactions.add(
                    transaction(
                        user,
                        plan,
                        account,
                        month.withDayOfMonth(21),
                        monthlyAmount(bd(160), plan, index, "medical").negate(),
                        "Sonic HealthPlus",
                        category(categories, "Healthcare", "Healthcare"),
                        payees,
                        "Medical appointment"
                    )
                );
                transactions.addAll(
                    splitSpend(
                        user,
                        plan,
                        account,
                        month,
                        monthlyAmount(spending.subscriptions(), plan, index, "subscriptions"),
                        category(categories, "Entertainment", "Entertainment"),
                        payees,
                        List.of("Netflix", "Disney+", "Apple", "Spotify"),
                        countFor(accountPlan, plan, index, "subscriptions", 1, 3),
                        4,
                        24,
                        "Subscription"
                    )
                );
            }
            if (isVacationMonth(index)) {
                transactions.addAll(createBankVacationTransactions(user, plan, account, month, index, categories, payees, spending));
            }
        }
        return transactions;
    }

    private List<FinanceTransaction> createCreditTransactions(
        User user,
        DemoPlan plan,
        AccountPlan accountPlan,
        FinanceAccount account,
        Map<String, FinanceCategory> categories,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceTransaction> transactions = new ArrayList<>();
        int months = months(account);
        SpendingProfile spending = spendingProfile(plan);
        for (int index = 0; index < months; index++) {
            LocalDate month = month(account, index);
            transactions.addAll(
                splitSpend(
                    user,
                    plan,
                    account,
                    month,
                    monthlyAmount(spending.groceries(), plan, index, "groceries"),
                    category(categories, "Groceries", "Food & Groceries"),
                    payees,
                    List.of("Woolworths", "Coles", "ALDI", "Harris Farm Markets", "IGA"),
                    countFor(accountPlan, plan, index, "groceries", 4, 7),
                    2,
                    27,
                    "Groceries"
                )
            );
            transactions.addAll(
                splitSpend(
                    user,
                    plan,
                    account,
                    month,
                    monthlyAmount(spending.fuel(), plan, index, "fuel"),
                    category(categories, "Petrol/Gas", "Car & Auto"),
                    payees,
                    List.of("Ampol", "BP", "Shell Coles Express", "Caltex", "7-Eleven Fuel"),
                    countFor(accountPlan, plan, index, "fuel", 2, 4),
                    3,
                    25,
                    "Fuel"
                )
            );
            transactions.addAll(
                splitSpend(
                    user,
                    plan,
                    account,
                    month,
                    monthlyAmount(spending.dining(), plan, index, "dining"),
                    category(categories, "Dining Out", "Food & Groceries"),
                    payees,
                    List.of("Howard Smith Wharves", "Grill'd", "Sushi Sushi", "Guzman y Gomez", "The Coffee Club", "Uber Eats"),
                    countFor(accountPlan, plan, index, "dining", 2, 6),
                    4,
                    26,
                    "Dining"
                )
            );
            transactions.addAll(
                splitSpend(
                    user,
                    plan,
                    account,
                    month,
                    monthlyAmount(spending.healthcare(), plan, index, "healthcare"),
                    category(categories, "Healthcare", "Healthcare"),
                    payees,
                    List.of("Chemist Warehouse", "Priceline Pharmacy", "Sonic HealthPlus", "Bupa Dental", "Ramsay Pharmacy"),
                    countFor(accountPlan, plan, index, "healthcare", plan.retiree() ? 2 : 1, plan.retiree() ? 4 : 2),
                    5,
                    24,
                    "Healthcare"
                )
            );
            if ("Family".equals(plan.persona())) {
                transactions.addAll(
                    splitSpend(
                        user,
                        plan,
                        account,
                        month,
                        monthlyAmount(bd(210), plan, index, "children-sport"),
                        category(categories, "Sport Teams & Lessons", "Sport"),
                        payees,
                        List.of("Little Athletics Queensland", "Rebel Sport", "Swim Australia", "Football Queensland"),
                        countFor(accountPlan, plan, index, "children-sport", 1, 3),
                        8,
                        24,
                        "Children sport"
                    )
                );
            }
            if ("Single".equals(plan.persona()) || "Couple".equals(plan.persona())) {
                transactions.addAll(
                    splitSpend(
                        user,
                        plan,
                        account,
                        month,
                        monthlyAmount(bd(150), plan, index, "entertainment"),
                        category(categories, "Entertainment", "Entertainment"),
                        payees,
                        List.of("Event Cinemas", "Ticketek", "Spotify", "Steam", "Booktopia"),
                        countFor(accountPlan, plan, index, "entertainment", 1, 3),
                        10,
                        26,
                        "Entertainment"
                    )
                );
            }
            if (!"low".equals(accountPlan.density())) {
                transactions.addAll(
                    splitSpend(
                        user,
                        plan,
                        account,
                        month,
                        monthlyAmount(spending.household(), plan, index, "household"),
                        category(categories, "Household", "Household"),
                        payees,
                        List.of("Kmart", "Big W", "Bunnings Warehouse", "Officeworks", "JB Hi-Fi"),
                        countFor(accountPlan, plan, index, "household", 1, 3),
                        7,
                        26,
                        "Household items"
                    )
                );
                transactions.addAll(
                    splitSpend(
                        user,
                        plan,
                        account,
                        month,
                        monthlyAmount(spending.clothing(), plan, index, "clothing"),
                        category(categories, "Clothing", "Shopping"),
                        payees,
                        List.of("Myer", "David Jones", "Uniqlo", "Cotton On", "The Iconic"),
                        countFor(accountPlan, plan, index, "clothing", 1, 3),
                        9,
                        27,
                        "Clothing"
                    )
                );
            }
            if (isVacationMonth(index)) {
                transactions.addAll(createCreditVacationTransactions(user, plan, account, month, index, categories, payees, spending));
            }
        }
        return transactions;
    }

    private List<FinanceTransaction> createLoanTransactions(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        Map<String, FinanceCategory> categories,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceTransaction> transactions = new ArrayList<>();
        int months = months(account);
        BigDecimal interest = plan.homeOwner() ? bd(-2100) : bd(-180);
        for (int index = 0; index < months; index++) {
            LocalDate month = month(account, index);
            transactions.add(
                transaction(
                    user,
                    plan,
                    account,
                    month.withDayOfMonth(20),
                    interest,
                    "Westpac",
                    category(categories, "Mortgage Charges", "Bank Charges"),
                    payees,
                    "Interest charge"
                )
            );
        }
        return transactions;
    }

    private List<FinanceTransaction> createRepayments(
        User user,
        DemoPlan plan,
        FinanceAccount from,
        FinanceAccount to,
        String payeeName,
        Map<String, FinanceCategory> categories,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceTransaction> transactions = new ArrayList<>();
        int months = Math.min(months(from), months(to));
        BigDecimal amount = to.getType().equals(FinanceAccountType.LOAN.getValue()) ? bd(3600) : bd(1250);
        for (int index = 0; index < months; index++) {
            LocalDate date = month(from, index).withDayOfMonth(26);
            transactions.add(transfer(user, plan, from, to, date, amount.negate(), payeeName, payees, "Repayment to " + to.getName()));
            transactions.add(transfer(user, plan, to, from, date, amount, payeeName, payees, "Repayment from " + from.getName()));
        }
        return transactions;
    }

    private List<FinanceTransaction> createCreditCardRepayments(
        User user,
        DemoPlan plan,
        AccountPlan creditPlan,
        FinanceAccount bank,
        FinanceAccount credit,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceTransaction> transactions = new ArrayList<>();
        int months = Math.min(months(bank), months(credit));
        for (int index = 0; index < months; index++) {
            LocalDate date = month(bank, index).withDayOfMonth(28);
            BigDecimal amount = estimatedCreditCardSpend(plan, creditPlan, index);
            transactions.add(
                transfer(user, plan, bank, credit, date, amount.negate(), "American Express", payees, "Monthly card repayment")
            );
            transactions.add(transfer(user, plan, credit, bank, date, amount, "Commonwealth Bank", payees, "Monthly card repayment"));
        }
        return transactions;
    }

    private InvestmentResult createInvestmentData(
        User user,
        DemoPlan plan,
        AccountPlan accountPlan,
        FinanceAccount account,
        Map<String, FinanceCategory> categories,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceTransaction> transactions = new ArrayList<>();
        List<FinanceUserSecurity> userSecurities = new ArrayList<>();
        List<FinanceSecurityPrice> prices = new ArrayList<>();
        List<FinanceLot> lots = new ArrayList<>();
        List<String> symbols = plannedSymbols(plan, accountPlan);
        int count = 0;
        for (String symbol : symbols) {
            FinanceSecurity known = securityRepository.findBySymbol(symbol).orElse(null);
            FinanceUserSecurity userSecurity = new FinanceUserSecurity();
            userSecurity.setUserGuid(user.getGuid().toString());
            userSecurity.setName(known != null ? known.getName() : symbol);
            userSecurity.setSymbol(symbol);
            userSecurity.setCurrencyCode(known != null ? known.getCurrencyCode() : plan.currencyCode());
            userSecurity.setSecurity(known);
            userSecurity.setType(known != null ? known.getType() : 1);
            userSecurity.setSerialDateTime(ZonedDateTime.now(DEMO_ZONE));
            userSecurity.setMasterGuid(UUID.randomUUID().toString());
            markGenerated(userSecurity, plan);
            userSecurity = userSecurityRepository.save(userSecurity);
            userSecurities.add(userSecurity);

            BigDecimal price = priceForSymbol(symbol);
            Optional<FinanceSecurityPrice> existingPrice = securityPriceRepository.findBySymbolAndDate(
                symbol,
                LocalDate.now(DEMO_ZONE).atStartOfDay(DEMO_ZONE)
            );
            if (existingPrice.isEmpty()) {
                FinanceSecurityPrice securityPrice = new FinanceSecurityPrice();
                securityPrice.setSymbol(symbol);
                securityPrice.setDate(LocalDate.now(DEMO_ZONE).atStartOfDay(DEMO_ZONE));
                securityPrice.setSerialDateTime(ZonedDateTime.now(DEMO_ZONE));
                securityPrice.setPrice(price);
                securityPrice.setOpen(price.multiply(bd("0.99")));
                securityPrice.setClose(price);
                securityPrice.setHigh(price.multiply(bd("1.02")));
                securityPrice.setLow(price.multiply(bd("0.98")));
                securityPrice.setComment("Closing price");
                markGenerated(securityPrice, plan);
                prices.add(securityPriceRepository.save(securityPrice));
            }

            LocalDate buyDate = account
                .getDateOpened()
                .plusMonths(count * 3L)
                .withDayOfMonth(8);
            double quantity = 25 + (count * 12);
            BigDecimal amount = price.multiply(bd(quantity)).setScale(2, RoundingMode.HALF_UP).negate();
            FinanceTransaction buy = investmentTransaction(
                user,
                plan,
                account,
                buyDate,
                amount,
                userSecurity,
                FinanceInvestmentActivityType.BUY,
                quantity,
                price.doubleValue(),
                payees,
                "Market buy order"
            );
            buy = transactionRepository.save(buy);
            transactions.add(buy);
            lots.add(lot(user, plan, account, userSecurity, buy, quantity, buyDate));

            double heldQuantity = quantity;
            heldQuantity = createProfileTrades(
                user,
                plan,
                accountPlan,
                account,
                userSecurity,
                price,
                buyDate,
                heldQuantity,
                transactions,
                lots,
                payees
            );
            transactions.addAll(createDividendTransactions(user, plan, account, userSecurity, price, buyDate, heldQuantity, payees));
            count++;
        }
        return new InvestmentResult(transactions, userSecurities, prices, lots);
    }

    private double createProfileTrades(
        User user,
        DemoPlan plan,
        AccountPlan accountPlan,
        FinanceAccount account,
        FinanceUserSecurity userSecurity,
        BigDecimal basePrice,
        LocalDate firstBuyDate,
        double heldQuantity,
        List<FinanceTransaction> transactions,
        List<FinanceLot> lots,
        Map<String, FinancePayee> payees
    ) {
        TradeProfile profile = tradeProfile(accountPlan.tradingProfile());
        int months = Math.max(1, (int) java.time.temporal.ChronoUnit.MONTHS.between(firstBuyDate, LocalDate.now(DEMO_ZONE)));
        Random random = randomFor(plan, months, userSecurity.getSymbol() + accountPlan.tradingProfile());
        for (int monthIndex = profile.firstTradeMonth(); monthIndex <= months; monthIndex += profile.intervalMonths()) {
            LocalDate tradeDate = firstBuyDate.plusMonths(monthIndex).withDayOfMonth(Math.min(24, 7 + random.nextInt(15)));
            if (tradeDate.isAfter(LocalDate.now(DEMO_ZONE))) {
                break;
            }
            boolean sell = random.nextInt(100) < profile.sellProbabilityPercent() && heldQuantity > 5;
            double quantity = profile.quantity(random, heldQuantity);
            BigDecimal tradePrice = variedTradePrice(basePrice, plan, monthIndex, userSecurity.getSymbol());
            FinanceInvestmentActivityType activity = sell ? FinanceInvestmentActivityType.SELL : FinanceInvestmentActivityType.BUY;
            BigDecimal amount = tradePrice.multiply(bd(quantity)).setScale(2, RoundingMode.HALF_UP);
            if (!sell) {
                amount = amount.negate();
            }
            FinanceTransaction trade = investmentTransaction(
                user,
                plan,
                account,
                tradeDate,
                amount,
                userSecurity,
                activity,
                quantity,
                tradePrice.doubleValue(),
                payees,
                profile.memo()
            );
            trade = transactionRepository.save(trade);
            transactions.add(trade);
            if (sell) {
                heldQuantity = Math.max(0, heldQuantity - quantity);
            } else {
                heldQuantity += quantity;
                lots.add(lot(user, plan, account, userSecurity, trade, quantity, tradeDate));
            }
        }
        return heldQuantity;
    }

    private List<FinanceTransaction> createDividendTransactions(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        FinanceUserSecurity userSecurity,
        BigDecimal price,
        LocalDate firstBuyDate,
        double heldQuantity,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceTransaction> transactions = new ArrayList<>();
        int frequencyMonths = dividendFrequencyMonths(userSecurity.getSymbol());
        BigDecimal annualYield = dividendAnnualYield(userSecurity.getSymbol());
        if (annualYield.signum() <= 0) {
            return transactions;
        }
        for (
            LocalDate dividendDate = firstBuyDate.plusMonths(frequencyMonths);
            !dividendDate.isAfter(LocalDate.now(DEMO_ZONE));
            dividendDate = dividendDate.plusMonths(frequencyMonths)
        ) {
            BigDecimal dividend = bd(heldQuantity)
                .multiply(price)
                .multiply(annualYield)
                .multiply(bd(frequencyMonths))
                .divide(bd(12), 2, RoundingMode.HALF_UP);
            if (dividend.compareTo(bd("1.00")) >= 0) {
                transactions.add(
                    investmentTransaction(
                        user,
                        plan,
                        account,
                        dividendDate,
                        dividend,
                        userSecurity,
                        FinanceInvestmentActivityType.DIVIDEND,
                        0,
                        0,
                        payees,
                        "Dividend payment"
                    )
                );
            }
        }
        return transactions;
    }

    private FinanceTransaction transaction(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        LocalDate date,
        BigDecimal amount,
        String payeeName,
        FinanceCategory category,
        Map<String, FinancePayee> payees,
        String memo
    ) {
        FinancePayee payee = payees.computeIfAbsent(payeeName.toLowerCase(Locale.ROOT), ignored -> createPayee(user, plan, payeeName));
        FinanceTransaction transaction = new FinanceTransaction();
        transaction.setUserGuid(user.getGuid().toString());
        transaction.setAccountId(account.getId().toString());
        transaction.setDate(date);
        transaction.setAmount(amount);
        transaction.setPrincipalAmount(amount);
        transaction.setCategory(category);
        transaction.setPayeeId(payee.getId() == null ? null : payee.getId().toString());
        transaction.setPayeeName(payeeName);
        transaction.setMemo(appendHints(plan, memo));
        transaction.setCleared(true);
        transaction.setReconciled(false);
        transaction.setRecurring(false);
        transaction.setVoided(false);
        transaction.setSplitParent(false);
        transaction.setSplitChild(false);
        transaction.setInvestment(false);
        transaction.setCurrencyCode(account.getCurrencyCode());
        transaction.setSerialDateTime(ZonedDateTime.now(DEMO_ZONE));
        transaction.setMasterGuid(UUID.randomUUID().toString());
        markGenerated(transaction, plan);
        return transaction;
    }

    private FinanceTransaction transfer(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        FinanceAccount transferredAccount,
        LocalDate date,
        BigDecimal amount,
        String payeeName,
        Map<String, FinancePayee> payees,
        String memo
    ) {
        FinanceTransaction transaction = transaction(user, plan, account, date, amount, payeeName, null, payees, memo);
        transaction.setTransfer(true);
        transaction.setTransferTo(amount.signum() > 0);
        transaction.setTransferredAccountId(transferredAccount.getId().toString());
        return transaction;
    }

    private FinanceTransaction investmentTransaction(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        LocalDate date,
        BigDecimal amount,
        FinanceUserSecurity userSecurity,
        FinanceInvestmentActivityType activityType,
        double quantity,
        double price,
        Map<String, FinancePayee> payees,
        String memo
    ) {
        FinanceTransaction transaction = transaction(user, plan, account, date, amount, userSecurity.getSymbol(), null, payees, memo);
        transaction.setInvestment(true);
        transaction.setSecurityId(userSecurity.getId().toString());
        transaction.setInvestmentActivityType(activityType);
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        transaction.setCurrencyCode(userSecurity.getCurrencyCode());
        if (!plan.currencyCode().equals(userSecurity.getCurrencyCode())) {
            transaction.setRateToBase(1.5);
            transaction.setAmountBase(amount.multiply(bd("1.5")).setScale(2, RoundingMode.HALF_UP));
        }
        return transaction;
    }

    private FinanceLot lot(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        FinanceUserSecurity userSecurity,
        FinanceTransaction buy,
        double quantity,
        LocalDate buyDate
    ) {
        FinanceLot lot = new FinanceLot();
        lot.setUserGuid(user.getGuid().toString());
        lot.setAccountId(account.getId().toString());
        lot.setSecurityId(userSecurity.getId().toString());
        lot.setBuyTransactionId(buy.getId());
        lot.setOpenTransactionId(buy.getId());
        lot.setQuantity(quantity);
        lot.setBuyDate(buyDate);
        lot.setOpenDate(buyDate);
        lot.setLotType(0);
        lot.setMasterGuid(UUID.randomUUID().toString());
        lot.setSerialDateTime(ZonedDateTime.now(DEMO_ZONE));
        markGenerated(lot, plan);
        return lot;
    }

    private FinancePayee createPayee(User user, DemoPlan plan, String name) {
        FinancePayee payee = new FinancePayee();
        payee.setUserGuid(user.getGuid().toString());
        payee.setName(name);
        payee.setHidden(false);
        payee.setSerialDateTime(ZonedDateTime.now(DEMO_ZONE));
        payee.setMasterGuid(UUID.randomUUID().toString());
        markGenerated(payee, plan);
        return payeeRepository.save(payee);
    }

    private Map<String, FinanceCategory> loadCategories(User user) {
        Map<String, FinanceCategory> categories = new LinkedHashMap<>();
        for (FinanceCategory category : categoryRepository.findAllByUserGuidOrSystem(user.getGuid().toString())) {
            categories.putIfAbsent(category.getName().toLowerCase(Locale.ROOT), category);
        }
        return categories;
    }

    private FinanceCategory category(Map<String, FinanceCategory> categories, String preferred, String fallback) {
        FinanceCategory category = categories.get(preferred.toLowerCase(Locale.ROOT));
        if (category == null && fallback != null) {
            category = categories.get(fallback.toLowerCase(Locale.ROOT));
        }
        return category;
    }

    private List<String> plannedSymbols(DemoPlan plan) {
        AccountPlan accountPlan = plan
            .accountPlans()
            .stream()
            .filter(account -> "investment".equals(account.type()))
            .findFirst()
            .orElse(null);
        return plannedSymbols(plan, accountPlan);
    }

    private List<String> plannedSymbols(DemoPlan plan, AccountPlan accountPlan) {
        List<String> symbols = new ArrayList<>(List.of("VAS", "ANZ", "BHP", "WOW", "CBA", "CSL", "WES", "TLS", "MQG"));
        if (plan.internationalStocks()) {
            symbols.addAll(List.of("IBM", "AAPL", "MSFT"));
        }
        int stockCount = accountPlan == null ? investmentStockCount(plan) : accountPlan.stockCount();
        return symbols.subList(0, Math.min(stockCount, symbols.size()));
    }

    private int investmentStockCount(DemoPlan plan) {
        return plan
            .accountPlans()
            .stream()
            .filter(account -> "investment".equals(account.type()))
            .findFirst()
            .map(AccountPlan::stockCount)
            .orElse(4);
    }

    private String normalizeTradingProfile(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return "buy_hold";
        }
        value = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_').replace("/", "_");
        return switch (value) {
            case "buy_hold", "core_satellite", "limited_trading", "active_trading", "day_trader" -> value;
            default -> "buy_hold";
        };
    }

    private TradeProfile tradeProfile(String profile) {
        return switch (profile) {
            case "core_satellite" -> new TradeProfile(6, 6, 30, 4, 14, "Satellite rebalance");
            case "limited_trading" -> new TradeProfile(4, 4, 35, 5, 18, "Portfolio adjustment");
            case "active_trading" -> new TradeProfile(2, 2, 45, 8, 28, "Active trade");
            case "day_trader" -> new TradeProfile(1, 1, 52, 12, 45, "Short-term trade");
            default -> new TradeProfile(18, 12, 0, 2, 6, "Regular investment");
        };
    }

    private BigDecimal priceForSymbol(String symbol) {
        return bd(
            switch (symbol) {
                case "ANZ" -> 29;
                case "BHP" -> 44;
                case "WOW" -> 36;
                case "VAS" -> 102;
                case "CBA" -> 128;
                case "CSL" -> 285;
                case "WES" -> 66;
                case "TLS" -> 4;
                case "MQG" -> 205;
                case "IBM" -> 185;
                case "AAPL" -> 215;
                case "MSFT" -> 430;
                default -> 50;
            }
        );
    }

    private BigDecimal variedTradePrice(BigDecimal price, DemoPlan plan, int monthIndex, String symbol) {
        Random random = randomFor(plan, monthIndex, symbol + "-price");
        double multiplier = 0.88 + (random.nextDouble() * 0.24);
        return price.multiply(bd(multiplier)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal dividendAnnualYield(String symbol) {
        return bd(
            switch (symbol) {
                case "ANZ" -> 0.060;
                case "BHP" -> 0.052;
                case "WOW" -> 0.030;
                case "VAS" -> 0.038;
                case "CBA" -> 0.042;
                case "CSL" -> 0.012;
                case "WES" -> 0.041;
                case "TLS" -> 0.045;
                case "MQG" -> 0.035;
                case "IBM" -> 0.036;
                case "AAPL" -> 0.005;
                case "MSFT" -> 0.007;
                default -> 0.020;
            }
        );
    }

    private int dividendFrequencyMonths(String symbol) {
        return switch (symbol) {
            case "AAPL", "MSFT", "IBM" -> 3;
            default -> 6;
        };
    }

    private FinanceAccount firstAccount(List<AccountInstance> accountInstances, String type) {
        return accountInstances
            .stream()
            .filter(instance -> type.equals(instance.plan().type()))
            .map(AccountInstance::account)
            .findFirst()
            .orElse(null);
    }

    private BigDecimal sequenceStartingValue(BigDecimal startingValue, String type, int sequence) {
        if (sequence == 1) {
            return startingValue;
        }
        BigDecimal multiplier = switch (type) {
            case "bank" -> bd("0.35");
            case "credit" -> bd("0.00");
            case "loan" -> bd("0.55");
            case "asset" -> bd("0.65");
            case "investment" -> bd("0.45");
            default -> bd("1.00");
        };
        return startingValue.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private String accountName(String type, String defaultName, String companyName, int sequence, int total) {
        if (total <= 1) {
            return baseAccountName(type, defaultName, companyName);
        }
        return switch (type) {
            case "bank" -> List.of(
                "Commonwealth Bank Smart Access",
                "ING Savings Maximiser",
                "Macquarie Transaction Account",
                "NAB Reward Saver",
                "Westpac Choice"
            ).get(sequence - 1);
            case "credit" -> List.of(
                "American Express Platinum Edge",
                "Qantas Premier Platinum",
                "Westpac Altitude Black",
                "NAB Rewards Signature",
                "ANZ Rewards Black"
            ).get(sequence - 1);
            case "loan" -> List.of(
                "Westpac Flexi First Option Home Loan",
                "NAB Personal Loan",
                "Commonwealth Bank Investment Loan",
                "Macquarie Car Loan",
                "ANZ Variable Home Loan"
            ).get(sequence - 1);
            case "asset" -> List.of(
                "CoreLogic Family Home",
                "REA Group Investment Property",
                "Toyota RAV4",
                "Tesla Model Y",
                "Domain Coastal Apartment"
            ).get(sequence - 1);
            case "investment" -> List.of(
                "Vanguard Personal Investor",
                "CommSec Trading Account",
                "Selfwealth Share Portfolio",
                "nabtrade Portfolio",
                "Betashares Direct"
            ).get(sequence - 1);
            default -> companyName + " " + sequence;
        };
    }

    private String baseAccountName(String type, String defaultName, String companyName) {
        return switch (type) {
            case "bank" -> "Commonwealth Bank Smart Access";
            case "credit" -> "American Express Platinum Edge";
            case "loan" -> defaultName.toLowerCase(Locale.ROOT).contains("mortgage")
                ? "Westpac Flexi First Option Home Loan"
                : "NAB Personal Loan";
            case "asset" -> defaultName.toLowerCase(Locale.ROOT).contains("property")
                ? "REA Group Investment Property"
                : (defaultName.toLowerCase(Locale.ROOT).contains("home") ? "CoreLogic Family Home" : "Toyota RAV4");
            case "investment" -> "Vanguard Personal Investor";
            default -> companyName;
        };
    }

    private String realCompanyName(String defaultName) {
        if (defaultName.toLowerCase(Locale.ROOT).contains("banking")) {
            return "Commonwealth Bank";
        }
        if (defaultName.toLowerCase(Locale.ROOT).contains("credit")) {
            return "American Express";
        }
        if (defaultName.toLowerCase(Locale.ROOT).contains("loan") || defaultName.toLowerCase(Locale.ROOT).contains("mortgage")) {
            return "Westpac";
        }
        if (defaultName.toLowerCase(Locale.ROOT).contains("portfolio")) {
            return "Vanguard";
        }
        return "CoreLogic";
    }

    private String employerPayee(DemoPlan plan) {
        return switch (plan.persona()) {
            case "Couple" -> "Qantas Payroll";
            case "Family" -> "Atlassian Australia Payroll";
            case "Retiree" -> "AustralianSuper";
            default -> "Woolworths Group Payroll";
        };
    }

    private String secondaryEmployerPayee(DemoPlan plan) {
        return switch (plan.persona()) {
            case "Couple" -> "Suncorp Payroll";
            case "Family" -> "Queensland Health Payroll";
            default -> "Coles Group Payroll";
        };
    }

    private List<String> buildNotes(DemoPlan plan) {
        List<String> notes = new ArrayList<>();
        notes.add("Add-only generation; existing data is not modified.");
        notes.add("Every created record is flagged generated with batch " + plan.batchId());
        if (plan.hints() != null) {
            notes.add("Hints included: " + plan.hints());
        }
        if (plan.aiNote() != null) {
            notes.add("AI parameter note: " + plan.aiNote());
        }
        if (plan.noInvestments()) {
            notes.add("Investment data disabled by variant.");
        }
        return notes;
    }

    private int estimatedPayeeCount(DemoPlan plan) {
        int count = 28;
        if (plan.privateSchool()) count++;
        if (plan.realEstateInvestor()) count++;
        if (plan.includeInvestments()) count += plannedSymbols(plan).size();
        return count;
    }

    private int months(FinanceAccount account) {
        return Math.max(1, (int) java.time.temporal.ChronoUnit.MONTHS.between(account.getDateOpened(), LocalDate.now(DEMO_ZONE)) + 1);
    }

    private LocalDate month(FinanceAccount account, int index) {
        return account.getDateOpened().plusMonths(index);
    }

    private BigDecimal monthlyIncome(DemoPlan plan, int months, int index) {
        if (plan.retiree()) {
            return bd(plan.monthlyIncome());
        }
        int remainingYears = Math.max(0, (months - index - 1) / 12);
        double income = plan.monthlyIncome() / Math.pow(1 + wageGrowthRate(plan), remainingYears);
        return bd(income);
    }

    private List<FinanceTransaction> createIncomeTransactions(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        LocalDate month,
        int months,
        int index,
        Map<String, FinanceCategory> categories,
        Map<String, FinancePayee> payees
    ) {
        BigDecimal income = monthlyIncome(plan, months, index);
        if (plan.retiree()) {
            return List.of(
                transaction(
                    user,
                    plan,
                    account,
                    month.withDayOfMonth(5),
                    income,
                    "AustralianSuper",
                    category(categories, "Super/IRA Distributions", "Retirement Income"),
                    payees,
                    "Account based pension"
                )
            );
        }
        if ("Family".equals(plan.persona()) || "Couple".equals(plan.persona())) {
            BigDecimal primary = income.multiply(bd("0.58")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal secondary = income.subtract(primary).setScale(2, RoundingMode.HALF_UP);
            return List.of(
                transaction(
                    user,
                    plan,
                    account,
                    month.withDayOfMonth(15),
                    primary,
                    employerPayee(plan),
                    category(categories, "Net Pay", "Wages & Salary"),
                    payees,
                    "Primary salary"
                ),
                transaction(
                    user,
                    plan,
                    account,
                    month.withDayOfMonth(16),
                    secondary,
                    secondaryEmployerPayee(plan),
                    category(categories, "Net Pay", "Wages & Salary"),
                    payees,
                    "Secondary salary"
                )
            );
        }
        return List.of(
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(15),
                income,
                employerPayee(plan),
                category(categories, "Net Pay", "Wages & Salary"),
                payees,
                "Monthly salary"
            )
        );
    }

    private List<FinanceTransaction> createSecondaryBankTransactions(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        LocalDate month,
        int index,
        Map<String, FinanceCategory> categories,
        Map<String, FinancePayee> payees
    ) {
        BigDecimal interest = savingsMonthlyInterest(account, plan, index);
        BigDecimal fee = monthlyAmount(bd(5 + (account.getName().length() % 6)), plan, index, "account-fee");
        return List.of(
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(1),
                interest,
                account.getName(),
                category(categories, "Interest Income", "Other Income"),
                payees,
                "Savings interest"
            ),
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(28),
                fee.negate(),
                account.getName(),
                category(categories, "Bank Charges", "Bank Charges"),
                payees,
                "Account fee"
            )
        );
    }

    private List<FinanceTransaction> ensureBankAccountsStayPositive(
        User user,
        DemoPlan plan,
        List<AccountInstance> accountInstances,
        List<FinanceTransaction> transactions,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceTransaction> balancingTransactions = new ArrayList<>();
        List<FinanceAccount> bankAccounts = accountInstances
            .stream()
            .filter(instance -> "bank".equals(instance.plan().type()))
            .map(AccountInstance::account)
            .toList();
        for (FinanceAccount account : bankAccounts) {
            BigDecimal minimumBalance = minimumRunningBalance(account, transactions, balancingTransactions);
            if (minimumBalance.signum() >= 0) {
                continue;
            }
            BigDecimal required = minimumBalance.abs().add(bd(500)).setScale(2, RoundingMode.HALF_UP);
            FinanceAccount source = bankAccounts
                .stream()
                .filter(candidate -> !candidate.getId().equals(account.getId()))
                .filter(
                    candidate -> minimumRunningBalance(candidate, transactions, balancingTransactions).compareTo(required.add(bd(500))) > 0
                )
                .findFirst()
                .orElse(null);
            LocalDate supportDate = account.getDateOpened().plusDays(2);
            if (source != null) {
                balancingTransactions.add(
                    transfer(
                        user,
                        plan,
                        source,
                        account,
                        supportDate,
                        required.negate(),
                        "Commonwealth Bank",
                        payees,
                        "Cash buffer transfer"
                    )
                );
                balancingTransactions.add(
                    transfer(user, plan, account, source, supportDate, required, "Commonwealth Bank", payees, "Cash buffer transfer")
                );
            } else {
                account.setStartingBalance(account.getStartingBalance().add(required).setScale(2, RoundingMode.HALF_UP));
            }
        }
        return balancingTransactions;
    }

    private List<FinanceTransaction> createInvestmentFundingTransfers(
        User user,
        DemoPlan plan,
        FinanceAccount bank,
        List<AccountInstance> accountInstances,
        List<FinanceTransaction> transactions,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceTransaction> fundingTransactions = new ArrayList<>();
        List<FinanceAccount> investmentAccounts = accountInstances
            .stream()
            .filter(instance -> "investment".equals(instance.plan().type()))
            .map(AccountInstance::account)
            .toList();
        for (FinanceAccount investmentAccount : investmentAccounts) {
            Map<LocalDate, BigDecimal> fundingByDate = new LinkedHashMap<>();
            for (FinanceTransaction transaction : transactions) {
                if (
                    investmentAccount.getId().toString().equals(transaction.getAccountId()) &&
                    transaction.isInvestment() &&
                    FinanceInvestmentActivityType.BUY.equals(transaction.getInvestmentActivityType()) &&
                    transaction.getAmount().signum() < 0
                ) {
                    fundingByDate.merge(transaction.getDate(), transaction.getAmount().abs(), BigDecimal::add);
                }
            }
            for (Map.Entry<LocalDate, BigDecimal> entry : fundingByDate.entrySet()) {
                BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
                fundingTransactions.add(
                    transfer(
                        user,
                        plan,
                        bank,
                        investmentAccount,
                        entry.getKey(),
                        amount.negate(),
                        "Vanguard Personal Investor",
                        payees,
                        "Investment contribution"
                    )
                );
                fundingTransactions.add(
                    transfer(
                        user,
                        plan,
                        investmentAccount,
                        bank,
                        entry.getKey(),
                        amount,
                        "Commonwealth Bank",
                        payees,
                        "Investment contribution"
                    )
                );
            }
        }
        return fundingTransactions;
    }

    private List<FinanceTransaction> createSavingsSweeps(
        User user,
        DemoPlan plan,
        FinanceAccount primaryBank,
        List<AccountInstance> accountInstances,
        List<FinanceTransaction> transactions,
        Map<String, FinancePayee> payees
    ) {
        List<FinanceAccount> savingsAccounts = accountInstances
            .stream()
            .filter(instance -> "bank".equals(instance.plan().type()) && instance.plan().sequence() > 1)
            .map(AccountInstance::account)
            .toList();
        if (savingsAccounts.isEmpty()) {
            return List.of();
        }

        List<FinanceTransaction> sweepTransactions = new ArrayList<>();
        int months = months(primaryBank);
        BigDecimal reserve = bd(Math.max(5000, Math.round(plan.monthlyIncome() * 0.35)));
        for (int index = 0; index < months; index++) {
            LocalDate sweepDate = month(primaryBank, index);
            sweepDate = sweepDate.withDayOfMonth(sweepDate.lengthOfMonth());
            BigDecimal balance = projectedBalanceOn(primaryBank, transactions, sweepTransactions, sweepDate);
            BigDecimal sweepable = balance.subtract(reserve);
            if (sweepable.compareTo(bd(1000)) < 0) {
                continue;
            }
            BigDecimal amount = sweepable.multiply(bd("0.65")).setScale(2, RoundingMode.HALF_UP);
            FinanceAccount savingsAccount = savingsAccounts.get(index % savingsAccounts.size());
            sweepTransactions.add(
                transfer(
                    user,
                    plan,
                    primaryBank,
                    savingsAccount,
                    sweepDate,
                    amount.negate(),
                    savingsAccount.getName(),
                    payees,
                    "Savings transfer"
                )
            );
            sweepTransactions.add(
                transfer(user, plan, savingsAccount, primaryBank, sweepDate, amount, primaryBank.getName(), payees, "Savings transfer")
            );
        }
        return sweepTransactions;
    }

    private BigDecimal minimumRunningBalance(
        FinanceAccount account,
        List<FinanceTransaction> transactions,
        List<FinanceTransaction> balancingTransactions
    ) {
        BigDecimal balance = account.getStartingBalance();
        BigDecimal minimum = balance;
        for (FinanceTransaction transaction : accountTransactions(account, transactions, balancingTransactions)) {
            balance = balance.add(transaction.getAmount());
            minimum = minimum.min(balance);
        }
        return minimum;
    }

    private BigDecimal projectedBalanceOn(
        FinanceAccount account,
        List<FinanceTransaction> transactions,
        List<FinanceTransaction> balancingTransactions,
        LocalDate date
    ) {
        BigDecimal balance = account.getStartingBalance();
        for (FinanceTransaction transaction : accountTransactions(account, transactions, balancingTransactions)) {
            if (!transaction.getDate().isAfter(date)) {
                balance = balance.add(transaction.getAmount());
            }
        }
        return balance;
    }

    private List<FinanceTransaction> accountTransactions(
        FinanceAccount account,
        List<FinanceTransaction> transactions,
        List<FinanceTransaction> balancingTransactions
    ) {
        List<FinanceTransaction> accountTransactions = new ArrayList<>();
        accountTransactions.addAll(
            transactions
                .stream()
                .filter(transaction -> account.getId().toString().equals(transaction.getAccountId()))
                .toList()
        );
        accountTransactions.addAll(
            balancingTransactions
                .stream()
                .filter(transaction -> account.getId().toString().equals(transaction.getAccountId()))
                .toList()
        );
        accountTransactions.sort(
            Comparator.comparing(FinanceTransaction::getDate).thenComparing(
                FinanceTransaction::getId,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
        );
        return accountTransactions;
    }

    private Double savingsInterestRate(AccountPlan accountPlan) {
        return switch (accountPlan.sequence()) {
            case 2 -> 4.75;
            case 3 -> 4.35;
            case 4 -> 4.10;
            default -> 3.85;
        };
    }

    private BigDecimal savingsMonthlyInterest(FinanceAccount account, DemoPlan plan, int index) {
        double annualRate = account.getInterestRate() == null ? 0 : account.getInterestRate();
        if (annualRate <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal baseBalance = account.getStartingBalance().max(BigDecimal.ZERO);
        BigDecimal interest = baseBalance.multiply(bd(annualRate)).divide(bd(1200), 2, RoundingMode.HALF_UP);
        Random random = randomFor(plan, index, account.getName() + "-interest");
        double multiplier = 0.95 + (random.nextDouble() * 0.10);
        return interest.multiply(bd(multiplier)).setScale(2, RoundingMode.HALF_UP);
    }

    private double wageGrowthRate(DemoPlan plan) {
        return switch (plan.persona()) {
            case "Couple" -> 0.07;
            case "Family" -> 0.05;
            case "Retiree" -> 0.02;
            default -> 0.06;
        };
    }

    private SpendingProfile spendingProfile(DemoPlan plan) {
        return switch (plan.persona()) {
            case "Single" -> new SpendingProfile(
                bd(650),
                bd(220),
                bd(360),
                bd(120),
                bd(110),
                bd(95),
                bd(80),
                bd(160),
                bd(75),
                bd(1800),
                bd(2400)
            );
            case "Couple" -> new SpendingProfile(
                bd(1050),
                bd(360),
                bd(520),
                bd(180),
                bd(170),
                bd(150),
                bd(120),
                bd(230),
                bd(95),
                bd(2300),
                bd(3600)
            );
            case "Retiree" -> new SpendingProfile(
                bd(760),
                bd(170),
                bd(420),
                bd(260),
                bd(135),
                bd(95),
                bd(70),
                bd(260),
                bd(80),
                bd(1700),
                bd(4200)
            );
            default -> new SpendingProfile(
                bd(1450),
                bd(520),
                bd(650),
                bd(210),
                bd(260),
                bd(260),
                bd(160),
                bd(320),
                bd(110),
                bd(2800),
                bd(5200)
            );
        };
    }

    private boolean isVacationMonth(int index) {
        return index % 12 == 6;
    }

    private BigDecimal estimatedCreditCardSpend(DemoPlan plan, AccountPlan creditPlan, int index) {
        SpendingProfile spending = spendingProfile(plan);
        BigDecimal amount = monthlyAmount(spending.groceries(), plan, index, "groceries")
            .add(monthlyAmount(spending.fuel(), plan, index, "fuel"))
            .add(monthlyAmount(spending.dining(), plan, index, "dining"))
            .add(monthlyAmount(spending.healthcare(), plan, index, "healthcare"));
        if ("Family".equals(plan.persona())) {
            amount = amount.add(monthlyAmount(bd(210), plan, index, "children-sport"));
        }
        if ("Single".equals(plan.persona()) || "Couple".equals(plan.persona())) {
            amount = amount.add(monthlyAmount(bd(150), plan, index, "entertainment"));
        }
        if (!"low".equals(creditPlan.density())) {
            amount = amount
                .add(monthlyAmount(spending.household(), plan, index, "household"))
                .add(monthlyAmount(spending.clothing(), plan, index, "clothing"));
        }
        if (isVacationMonth(index)) {
            amount = amount.add(monthlyAmount(spending.vacationBudget(), plan, index, "vacation").multiply(bd("0.72")));
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private List<FinanceTransaction> splitSpend(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        LocalDate month,
        BigDecimal total,
        FinanceCategory category,
        Map<String, FinancePayee> payees,
        List<String> payeeNames,
        int count,
        int firstDay,
        int lastDay,
        String memo
    ) {
        List<FinanceTransaction> transactions = new ArrayList<>();
        Random random = randomFor(plan, month.getMonthValue() + (month.getYear() * 12), memo);
        List<BigDecimal> amounts = splitAmount(total, count, random);
        for (int i = 0; i < amounts.size(); i++) {
            int day = firstDay + random.nextInt(Math.max(1, lastDay - firstDay + 1));
            String payeeName = payeeNames.get(random.nextInt(payeeNames.size()));
            transactions.add(
                transaction(user, plan, account, month.withDayOfMonth(day), amounts.get(i).negate(), payeeName, category, payees, memo)
            );
        }
        return transactions;
    }

    private List<BigDecimal> splitAmount(BigDecimal total, int count, Random random) {
        int resolvedCount = Math.max(1, count);
        List<Integer> weights = new ArrayList<>();
        int weightTotal = 0;
        for (int i = 0; i < resolvedCount; i++) {
            int weight = 60 + random.nextInt(90);
            weights.add(weight);
            weightTotal += weight;
        }
        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < resolvedCount; i++) {
            BigDecimal amount =
                i == resolvedCount - 1
                    ? total.subtract(allocated)
                    : total.multiply(BigDecimal.valueOf(weights.get(i))).divide(BigDecimal.valueOf(weightTotal), 2, RoundingMode.HALF_UP);
            allocated = allocated.add(amount);
            amounts.add(amount.max(bd("1.00")).setScale(2, RoundingMode.HALF_UP));
        }
        return amounts;
    }

    private BigDecimal monthlyAmount(BigDecimal base, DemoPlan plan, int monthIndex, String key) {
        Random random = randomFor(plan, monthIndex, key);
        double multiplier = 0.82 + (random.nextDouble() * 0.36);
        return base.multiply(bd(multiplier)).multiply(monthlySpendingScale(plan, monthIndex)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal monthlySpendingScale(DemoPlan plan, int monthIndex) {
        BigDecimal targetSpend = bd(plan.monthlyIncome())
            .multiply(bd(monthlySpendingRate(plan, monthIndex)))
            .divide(bd(100), 2, RoundingMode.HALF_UP);
        BigDecimal baseline = baselineMonthlySpend(plan).max(bd("1.00"));
        return targetSpend.divide(baseline, 4, RoundingMode.HALF_UP);
    }

    private int monthlySpendingRate(DemoPlan plan, int monthIndex) {
        Random random = randomFor(plan, monthIndex, "spending-rate");
        int rate = plan.spendingRatePercent() - 7 + random.nextInt(15);
        return Math.max(30, Math.min(80, rate));
    }

    private BigDecimal baselineMonthlySpend(DemoPlan plan) {
        SpendingProfile spending = spendingProfile(plan);
        BigDecimal baseline = spending
            .groceries()
            .add(spending.fuel())
            .add(spending.dining())
            .add(spending.healthcare())
            .add(spending.household())
            .add(spending.clothing())
            .add(spending.transport())
            .add(spending.insurance())
            .add(spending.subscriptions());
        if (plan.renter()) {
            baseline = baseline.add(spending.housing());
        }
        if (plan.privateSchool()) {
            baseline = baseline.add(bd(1850));
        }
        if ("Family".equals(plan.persona())) {
            baseline = baseline.add(bd(620)).add(bd(210));
        }
        if (plan.homeOwner()) {
            baseline = baseline.add(bd(520)).add(bd(280));
        }
        if (plan.realEstateInvestor()) {
            baseline = baseline.add(bd(420));
        }
        if ("Single".equals(plan.persona()) || "Couple".equals(plan.persona())) {
            baseline = baseline.add(bd(150));
        }
        return baseline;
    }

    private int countFor(AccountPlan accountPlan, DemoPlan plan, int monthIndex, String key, int min, int max) {
        int densityBoost = switch (accountPlan.density()) {
            case "high" -> 1;
            case "low" -> -1;
            default -> 0;
        };
        int resolvedMin = Math.max(1, min + densityBoost);
        int resolvedMax = Math.max(resolvedMin, max + densityBoost);
        Random random = randomFor(plan, monthIndex, key + "-count");
        return resolvedMin + random.nextInt(resolvedMax - resolvedMin + 1);
    }

    private Random randomFor(DemoPlan plan, int monthIndex, String key) {
        long seed = plan.batchId().getMostSignificantBits() ^ plan.batchId().getLeastSignificantBits();
        seed ^= (long) monthIndex * 1_000_003L;
        seed ^= key.hashCode();
        return new Random(seed);
    }

    private List<FinanceTransaction> createBankVacationTransactions(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        LocalDate month,
        int monthIndex,
        Map<String, FinanceCategory> categories,
        Map<String, FinancePayee> payees,
        SpendingProfile spending
    ) {
        BigDecimal budget = monthlyAmount(spending.vacationBudget(), plan, monthIndex, "vacation");
        return List.of(
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(2),
                budget.multiply(bd("-0.18")),
                "Cover-More Travel Insurance",
                category(categories, "Insurance", "Travel"),
                payees,
                "Travel insurance"
            ),
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(3),
                budget.multiply(bd("-0.10")),
                "Travelex",
                category(categories, "Travel", "Vacation"),
                payees,
                "Foreign currency"
            ),
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(4),
                budget.multiply(bd("-0.12")),
                "Avis Car Rental",
                category(categories, "Rental Car & Taxi", "Travel"),
                payees,
                "Car rental"
            )
        );
    }

    private List<FinanceTransaction> createCreditVacationTransactions(
        User user,
        DemoPlan plan,
        FinanceAccount account,
        LocalDate month,
        int monthIndex,
        Map<String, FinanceCategory> categories,
        Map<String, FinancePayee> payees,
        SpendingProfile spending
    ) {
        BigDecimal budget = monthlyAmount(spending.vacationBudget(), plan, monthIndex, "vacation");
        return List.of(
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(5),
                budget.multiply(bd("-0.28")),
                "Qantas",
                category(categories, "Air Travel", "Travel"),
                payees,
                "Flights"
            ),
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(6),
                budget.multiply(bd("-0.30")),
                "Marriott Hotels",
                category(categories, "Hotel", "Travel"),
                payees,
                "Accommodation"
            ),
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(12),
                budget.multiply(bd("-0.08")),
                "Uber",
                category(categories, "Rental Car & Taxi", "Travel"),
                payees,
                "Airport transfers"
            ),
            transaction(
                user,
                plan,
                account,
                month.withDayOfMonth(14),
                budget.multiply(bd("-0.06")),
                "GetYourGuide",
                category(categories, "Entertainment", "Vacation"),
                payees,
                "Tours and activities"
            )
        );
    }

    private String density(FinanceDemoDataAccountSpecDTO spec) {
        String density = trimToNull(spec == null ? null : spec.getDensity());
        if ("low".equalsIgnoreCase(density) || "medium".equalsIgnoreCase(density) || "high".equalsIgnoreCase(density)) {
            return density.toLowerCase(Locale.ROOT);
        }
        return "medium";
    }

    private String appendHints(DemoPlan plan, String memo) {
        if (plan.hints() == null) {
            return memo;
        }
        return memo + " | Planning notes: " + plan.hints();
    }

    private String normalizePersona(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return "Family";
        }
        for (String persona : PERSONAS) {
            if (persona.equalsIgnoreCase(value)) {
                return persona;
            }
        }
        return "Family";
    }

    private String normalizeAccountType(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        value = value.toLowerCase(Locale.ROOT);
        return ACCOUNT_TYPES.contains(value) ? value : null;
    }

    private boolean bool(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private boolean bool(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : Boolean.TRUE.equals(value);
    }

    private int clamp(Integer value, int min, int max, int defaultValue) {
        int resolved = value == null ? defaultValue : value;
        return Math.max(min, Math.min(max, resolved));
    }

    private AiDemoParameters resolveAiDemoParameters(User user, FinanceDemoDataRequestDTO request, String persona) {
        String hints = trimToNull(request == null ? null : request.getHints());
        if (hints == null || request == null || !Boolean.TRUE.equals(request.getUseAiHints())) {
            return AiDemoParameters.empty();
        }
        String prompt = """
            Convert these demo finance data hints into bounded generator parameters.
            Return strict JSON only with:
            {
              "monthlyIncomeAdjustmentPercent": integer from -20 to 20,
              "note": short string under 120 chars
            }
            Persona: %s
            Hints: %s
            """.formatted(persona, hints);
        try {
            return aiService
                .generateText(user, AiServiceCallType.DEFAULT, prompt)
                .map(response -> parseAiDemoParameters(response.text()))
                .orElse(AiDemoParameters.empty());
        } catch (RuntimeException e) {
            return AiDemoParameters.empty();
        }
    }

    private AiDemoParameters parseAiDemoParameters(String text) {
        String json = trimToNull(text);
        if (json == null) {
            return AiDemoParameters.empty();
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            int adjustment = Math.max(-20, Math.min(20, node.path("monthlyIncomeAdjustmentPercent").asInt(0)));
            String note = trimToNull(node.path("note").asText(null));
            if (note != null && note.length() > 120) {
                note = note.substring(0, 120);
            }
            return new AiDemoParameters(adjustment, note);
        } catch (Exception e) {
            return AiDemoParameters.empty();
        }
    }

    private int applyAiIncomeAdjustment(int income, AiDemoParameters parameters) {
        if (parameters.monthlyIncomeAdjustmentPercent() == 0) {
            return income;
        }
        return Math.round((income * (100 + parameters.monthlyIncomeAdjustmentPercent())) / 100.0f);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean includeInvestments(DemoPlan plan) {
        return plan.includeInvestments();
    }

    private void markGenerated(FinanceAccount account, DemoPlan plan) {
        account.setGenerated(true);
        account.setGeneratedBatchId(plan.batchId());
        account.setGeneratedLabel(plan.label());
    }

    private void markGenerated(FinanceTransaction transaction, DemoPlan plan) {
        transaction.setGenerated(true);
        transaction.setGeneratedBatchId(plan.batchId());
        transaction.setGeneratedLabel(plan.label());
    }

    private void markGenerated(FinancePayee payee, DemoPlan plan) {
        payee.setGenerated(true);
        payee.setGeneratedBatchId(plan.batchId());
        payee.setGeneratedLabel(plan.label());
    }

    private void markGenerated(FinanceInstitution institution, DemoPlan plan) {
        institution.setGenerated(true);
        institution.setGeneratedBatchId(plan.batchId());
        institution.setGeneratedLabel(plan.label());
    }

    private void markGenerated(FinanceUserSecurity security, DemoPlan plan) {
        security.setGenerated(true);
        security.setGeneratedBatchId(plan.batchId());
        security.setGeneratedLabel(plan.label());
    }

    private void markGenerated(FinanceSecurityPrice price, DemoPlan plan) {
        price.setGenerated(true);
        price.setGeneratedBatchId(plan.batchId());
        price.setGeneratedLabel(plan.label());
    }

    private void markGenerated(FinanceLot lot, DemoPlan plan) {
        lot.setGenerated(true);
        lot.setGeneratedBatchId(plan.batchId());
        lot.setGeneratedLabel(plan.label());
    }

    private record AccountPlan(
        String type,
        String name,
        String currencyCode,
        int financeType,
        BigDecimal startingBalance,
        int years,
        String density,
        int stockCount,
        int sequence,
        String tradingProfile,
        String hints
    ) {
        int estimatedTransactions() {
            int monthly = switch (type) {
                case "bank" -> 12;
                case "credit" -> 8;
                case "loan" -> 2;
                case "investment" -> Math.max(2, stockCount * 2);
                default -> 0;
            };
            return monthly * years * 12;
        }
    }

    private record DemoPlan(
        UUID batchId,
        String label,
        String persona,
        String currencyCode,
        String hints,
        boolean domesticStocks,
        boolean internationalStocks,
        boolean realEstateInvestor,
        boolean homeOwner,
        boolean renter,
        boolean privateSchool,
        boolean noInvestments,
        int monthlyIncome,
        int spendingRatePercent,
        String aiNote,
        List<AccountPlan> accountPlans
    ) {
        boolean retiree() {
            return "Retiree".equals(persona);
        }

        boolean includeInvestments() {
            return !noInvestments && accountPlans.stream().anyMatch(account -> "investment".equals(account.type()));
        }
    }

    private record InvestmentResult(
        List<FinanceTransaction> transactions,
        List<FinanceUserSecurity> userSecurities,
        List<FinanceSecurityPrice> prices,
        List<FinanceLot> lots
    ) {}

    private record AccountInstance(FinanceAccount account, AccountPlan plan) {}

    private record TradeProfile(
        int firstTradeMonth,
        int intervalMonths,
        int sellProbabilityPercent,
        int minQuantity,
        int maxQuantity,
        String memo
    ) {
        double quantity(Random random, double heldQuantity) {
            int quantity = minQuantity + random.nextInt(Math.max(1, maxQuantity - minQuantity + 1));
            return Math.max(1, Math.min(quantity, Math.max(1, heldQuantity * 0.45)));
        }
    }

    private record AiDemoParameters(int monthlyIncomeAdjustmentPercent, String note) {
        static AiDemoParameters empty() {
            return new AiDemoParameters(0, null);
        }
    }

    private record SpendingProfile(
        BigDecimal groceries,
        BigDecimal fuel,
        BigDecimal dining,
        BigDecimal healthcare,
        BigDecimal household,
        BigDecimal clothing,
        BigDecimal transport,
        BigDecimal insurance,
        BigDecimal subscriptions,
        BigDecimal housing,
        BigDecimal vacationBudget
    ) {}
}
