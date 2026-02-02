package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ovaro.plat4m.domain.FinanceAccountType;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.domain.FinanceSecurityType;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceCategoryRepository;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.FinanceAccountService;
import ovaro.plat4m.service.FinanceSecurityService;
import ovaro.plat4m.service.UserService;
import ovaro.plat4m.service.dto.FinanceAccountDTO;
import ovaro.plat4m.service.dto.FinanceNestedResourceDTO;
import ovaro.plat4m.service.dto.FinanceSecurityHoldingDTO;

@RestController
@RequestMapping("/api")
public class FinanceGeneralResource {

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final Logger log = LoggerFactory.getLogger(FinanceGeneralResource.class);

    private FinanceSecurityService financeSecurityService;
    private FinanceAccountService financeAccountService;

    private FinanceCategoryRepository financeCategoryRepository;
    private final UserService userService;

    public static String GROUP_ACCOUNTS_BY_TYPE = "rg/AccountsByType";
    public static String GROUP_ACCOUNTS_BY_TYPE_STR = "Accounts By Type";

    public static String GROUP_ACCOUNTS_BY_CURRENCY = "rg/AccountsByCurrency";
    public static String GROUP_ACCOUNTS_BY_CURRENCY_STR = "Accounts By Currency";

    public static String SUB_GROUP_ACCOUNT_TYPES_PREFIX = "rg/AccountType-";
    public static String SUB_GROUP_ACCOUNT_CURRENCY_PREFIX = "rg/AccountCurrency-";
    public static String ACCOUNT_TYPE = "account";
    public static String ACCOUNT_TYPE_TYPE = "accountType";

    public static String ACCOUNT_CURRENCY_TYPE = "accountCurrency";

    public static String SOURCE_ACCOUNT = "account";
    public static String SOURCE_PORTFOLIO = "portfolio";

    public static String CATEGORY_TYPE_PREFIX = "rg/Category-";
    public static String CATEGORIES_TYPE = "IncomeExpense";

    public static String INCOME_TYPE_PREFIX = "rg/Income-";
    public static String EXPENSE_TYPE_PREFIX = "rg/Expense-";

    public FinanceGeneralResource(
        UserService userService,
        FinanceSecurityService financeSecurityService,
        FinanceAccountService financeAccountService,
        FinanceCategoryRepository financeCategoryRepository
    ) {
        this.financeSecurityService = financeSecurityService;
        this.userService = userService;
        this.financeAccountService = financeAccountService;
        this.financeCategoryRepository = financeCategoryRepository;
    }

    @GetMapping("/resources")
    public ResponseEntity<List<FinanceNestedResourceDTO>> listResources() throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        List<FinanceAccountDTO> accounts = financeAccountService.getAccounts(u.get(), (Integer) null, (Boolean) null);

        List<FinanceNestedResourceDTO> dtos = new ArrayList<FinanceNestedResourceDTO>();
        Map<String, FinanceNestedResourceDTO> allAccounts = new HashMap<String, FinanceNestedResourceDTO>();
        Map<String, FinanceNestedResourceDTO> childAccounts = new HashMap<String, FinanceNestedResourceDTO>();

        for (FinanceAccountDTO a : accounts) {
            FinanceNestedResourceDTO dto = new FinanceNestedResourceDTO(
                a.getId().toString(),
                a.getName(),
                null,
                a.getCurrencyCode(),
                a.getAccountType()
            );
            allAccounts.put(a.getId().toString(), dto);
            dto.setIndicatorSource(SOURCE_ACCOUNT);

            if (a.getType() != FinanceAccountType.INVESTMENT.getValue()) {
                dto.setHistorySource(SOURCE_ACCOUNT);
            } else {
                dto.setHistorySource(SOURCE_PORTFOLIO);
            }
            //if(a.getType() != FinanceAccountType.INVESTMENT.getValue()) {
            dtos.add(dto);
            //}

            // if(a.getRelatedToAccountId() != null) {

            //     dto.setResourceSymbol(a.getRelatedToAccountId());
            //     childAccounts.add(dto);
            // }
        }

        // Fix nested accounts / child account structure
        // for(FinanceNestedResourceDTO childDTO: childAccounts) {
        //     FinanceNestedResourceDTO a = allAccounts.get(childDTO.getResourceSymbol());
        //     if(a != null) {
        //         childDTO.setResourceSymbol(null); // Wipe out temporary storage location
        //         a.addChild(childDTO);
        //     } else {
        //         log.warn("Cannot find investment account: " + childDTO.getResourceSymbol());
        //     }
        // }

        List<FinanceUserSecurity> userSecurities = this.financeSecurityService.getUserSecurities(u.get(), null);
        for (FinanceUserSecurity us : userSecurities) {
            FinanceSecurityType t = FinanceSecurityType.toSecurityType(us.getType());
            FinanceNestedResourceDTO dto = new FinanceNestedResourceDTO(
                us.getId().toString(),
                us.getName(),
                us.getSymbol(),
                us.getCurrencyCode(),
                t.name()
            );
            dto.setIndicatorSource(SOURCE_PORTFOLIO);
            dto.setHistorySource(SOURCE_PORTFOLIO);

            allAccounts.put(us.getId().toString(), dto);
            if (us.getLinked() == null) {
                dtos.add(dto);
            } else {
                childAccounts.put(us.getLinked().getId().toString(), dto);
            }
        }

        Collection<FinanceSecurityHoldingDTO> inv = financeAccountService.investmentAccountHoldings(u.get(), null, true, LocalDate.now());
        for (FinanceSecurityHoldingDTO sec : inv) {
            FinanceNestedResourceDTO a = allAccounts.get(sec.getAccountId());
            if (a != null) {
                FinanceNestedResourceDTO dto = new FinanceNestedResourceDTO(
                    sec.getId().toString(),
                    sec.getName(),
                    sec.getSymbol(),
                    sec.getCurrencyCode(),
                    sec.getTypeName()
                );
                dto.setIndicatorSource(SOURCE_PORTFOLIO);
                dto.setHistorySource(SOURCE_PORTFOLIO);
                a.addChild(dto);
            } else {
                log.warn("Cannot find investment account: " + sec.getAccountId());
            }
        }

        //Fix nested accounts / child account structure
        for (String accountId : childAccounts.keySet()) {
            FinanceNestedResourceDTO child = childAccounts.get(accountId);
            FinanceNestedResourceDTO parent = allAccounts.get(accountId);
            if (parent != null) {
                parent.addChild(child);
            } else {
                log.warn("Cannot find parent account: " + accountId);
            }
        }

        // Group functions
        dtos.add(groupByAccountType(accounts, allAccounts));
        dtos.add(groupByAccountCurrency(accounts, allAccounts));

        dtos.add(getCategories());

        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    private FinanceNestedResourceDTO getCategories() {
        // Map<String, FinanceCategory> parentRegistry = new HashMap<String, FinanceCategory>();
        List<FinanceCategory> all = this.financeCategoryRepository.findAllByClassificationId(0);
        Map<String, FinanceNestedResourceDTO> indexedCategoryDTOs = new HashMap<String, FinanceNestedResourceDTO>();
        for (FinanceCategory category : all) {
            FinanceNestedResourceDTO dto = new FinanceNestedResourceDTO(category.getId().toString(), category.getName());
            indexedCategoryDTOs.put(dto.getId(), dto);
        }

        // Fix up the hierarchy
        List<FinanceNestedResourceDTO> parents = new ArrayList<FinanceNestedResourceDTO>();
        for (FinanceCategory category : all) {
            if (category.getParent() != null) {
                FinanceNestedResourceDTO childDTO = indexedCategoryDTOs.get(category.getId().toString());
                FinanceNestedResourceDTO parentDTO = indexedCategoryDTOs.get(category.getParent().getId().toString());
                parentDTO.addChild(childDTO);
            } else {
                FinanceNestedResourceDTO dto = indexedCategoryDTOs.get(category.getId().toString());
                if ("INCOME".equals(dto.getName())) {
                    dto.setId("Income");
                    dto.setName("Income");
                } else if ("EXPENSE".equals(dto.getName())) {
                    dto.setId("Expense");
                    dto.setName("Expense");
                }
                parents.add(dto);
            }
        }

        FinanceNestedResourceDTO dto = new FinanceNestedResourceDTO(CATEGORIES_TYPE, "Income & Expense");
        for (FinanceNestedResourceDTO parent : parents) {
            dto.addChild(parent);
        }

        return dto;
    }

    public FinanceNestedResourceDTO groupByAccountType(List<FinanceAccountDTO> accounts, Map<String, FinanceNestedResourceDTO> allAccounts)
        throws IOException {
        FinanceNestedResourceDTO grp = new FinanceNestedResourceDTO(GROUP_ACCOUNTS_BY_TYPE, GROUP_ACCOUNTS_BY_TYPE_STR);
        Map<String, FinanceNestedResourceDTO> subGroupIndex = new HashMap<String, FinanceNestedResourceDTO>();

        for (FinanceAccountDTO account : accounts) {
            FinanceNestedResourceDTO subGrp = subGroupIndex.get(account.getAccountType());
            if (subGrp == null) {
                subGrp = new FinanceNestedResourceDTO(SUB_GROUP_ACCOUNT_TYPES_PREFIX + account.getAccountType(), account.getAccountType());
                subGrp.setType(ACCOUNT_TYPE_TYPE);
                grp.addChild(subGrp);
                subGroupIndex.put(account.getAccountType(), subGrp);
            }

            FinanceNestedResourceDTO o = allAccounts.get(account.getId().toString());
            if (o == null) {
                o = new FinanceNestedResourceDTO(account.getId().toString(), account.getName());
                o.setType(ACCOUNT_TYPE);
                o.setCurrencyCode(account.getCurrencyCode());
            }

            subGrp.addChild(o);
        }
        return grp;
    }

    public FinanceNestedResourceDTO groupByAccountCurrency(
        List<FinanceAccountDTO> accounts,
        Map<String, FinanceNestedResourceDTO> allAccounts
    ) throws IOException {
        FinanceNestedResourceDTO grp = new FinanceNestedResourceDTO(GROUP_ACCOUNTS_BY_CURRENCY, GROUP_ACCOUNTS_BY_CURRENCY_STR);
        Map<String, FinanceNestedResourceDTO> subGroupIndex = new HashMap<String, FinanceNestedResourceDTO>();

        for (FinanceAccountDTO account : accounts) {
            FinanceNestedResourceDTO subGrp = subGroupIndex.get(account.getCurrencyCode());
            if (subGrp == null) {
                subGrp = new FinanceNestedResourceDTO(
                    SUB_GROUP_ACCOUNT_CURRENCY_PREFIX + account.getCurrencyCode(),
                    account.getCurrencyCode()
                );
                subGrp.setType(ACCOUNT_CURRENCY_TYPE);
                grp.addChild(subGrp);
                subGroupIndex.put(account.getCurrencyCode(), subGrp);
            }

            FinanceNestedResourceDTO o = allAccounts.get(account.getId().toString());
            if (o == null) {
                o = new FinanceNestedResourceDTO(account.getId().toString(), account.getName());
                o.setType(ACCOUNT_CURRENCY_TYPE);
                o.setCurrencyCode(account.getCurrencyCode());
            }

            subGrp.addChild(o);
        }
        return grp;
    }
    // public FinanceNestedResourceDTO groupByPortfolio(List<FinanceUserSecurity> userSecurities) throws IOException {
    //     FinanceNestedResourceDTO grp = new FinanceNestedResourceDTO(GROUP_ACCOUNTS_BY_CURRENCY, GROUP_ACCOUNTS_BY_CURRENCY_STR);
    //     Map<String, FinanceNestedResourceDTO> subGroupIndex = new HashMap<String, FinanceNestedResourceDTO>();

    //     for(FinanceAccountDTO account : accounts) {
    //         FinanceNestedResourceDTO subGrp = subGroupIndex.get(account.getCurrencyCode());
    //         if(subGrp == null) {
    //             subGrp = new FinanceNestedResourceDTO(SUB_GROUP_ACCOUNT_CURRENCY_PREFIX + account.getCurrencyCode(), account.getCurrencyCode());
    //             subGrp.setIndicatorType(ACCOUNT_CURRENCY_TYPE);
    //             grp.addChild(subGrp);
    //             subGroupIndex.put(account.getCurrencyCode(), subGrp);
    //         }

    //         FinanceNestedResourceDTO o = new FinanceNestedResourceDTO(account.getId().toString(), account.getName());
    //         o.setIndicatorType(ACCOUNT_CURRENCY_TYPE);
    //         o.setResourceCurrencyCode(account.getCurrencyCode());
    //         subGrp.addChild(o);
    //     }
    //     return grp;
    // }

}
