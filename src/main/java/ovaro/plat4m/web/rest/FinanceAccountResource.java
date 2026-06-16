package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ovaro.plat4m.domain.FinanceAccountType;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.FinanceAccountService;
import ovaro.plat4m.service.FinanceTransactionService;
import ovaro.plat4m.service.UserService;
import ovaro.plat4m.service.dto.FinanceAccountDTO;
import ovaro.plat4m.service.dto.FinanceCategoryCreateDTO;
import ovaro.plat4m.service.dto.FinanceIndicatorDTO;
import ovaro.plat4m.service.dto.FinanceIndicatorsDTO;
import ovaro.plat4m.service.dto.FinanceManageCategoryDTO;
import ovaro.plat4m.service.dto.FinanceManageCategoryUpdateDTO;
import ovaro.plat4m.service.dto.FinanceManagePayeeDTO;
import ovaro.plat4m.service.dto.FinanceManagePayeeUpdateDTO;
import ovaro.plat4m.service.dto.FinanceResourceDTO;
import ovaro.plat4m.service.dto.FinanceSnapshotsPerResourceDTO;
import ovaro.plat4m.service.dto.FinanceTransactionEditorOptionsDTO;
import ovaro.plat4m.service.dto.FinanceTransactionRowDTO;
import ovaro.plat4m.service.dto.FinanceTransactionUpdateDTO;
import ovaro.plat4m.service.dto.FinanceTreeNodeDTO;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class FinanceAccountResource {

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final Logger log = LoggerFactory.getLogger(FinanceAccountResource.class);
    private FinanceAccountService financeAccountService;
    private FinanceTransactionService financeTransactionService;
    private final UserService userService;

    public FinanceAccountResource(
        FinanceAccountService financeAccountService,
        FinanceTransactionService financeTransactionService,
        UserService userService
    ) {
        this.financeAccountService = financeAccountService;
        this.financeTransactionService = financeTransactionService;
        this.userService = userService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<FinanceAccountDTO>> getAccounts(@RequestParam(required = false) Integer type) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        List<FinanceAccountDTO> accounts = financeAccountService.getAccounts(u.get(), type, null);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @GetMapping("/accounts/balancesOLD")
    public ResponseEntity<List<FinanceAccountDTO>> getAccountsOptimsedOLD() throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        List<FinanceAccountDTO> accounts = financeAccountService.getAccountsOptimisedOld(u.get());
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @GetMapping("/accounts/balances")
    public ResponseEntity<List<FinanceAccountDTO>> getAccountsOptimsed() throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        List<FinanceAccountDTO> accounts = financeAccountService.getAccountsOptimised(u.get(), true, null, false);

        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @GetMapping("/account/{accountId}/transactions")
    public ResponseEntity<List<FinanceTransaction>> getAccountTransactions(
        @PathVariable(name = "accountId") String accountId,
        Pageable pageable
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        log.info("getAccountTransactions: " + accountId + " - " + pageable);
        final Page<FinanceTransaction> page = financeAccountService.getTransactions(accountId, pageable);
        log.info(
            "getAccountTransactions result: " +
                accountId +
                " - #" +
                page.getNumberOfElements() +
                ". Page: " +
                page.getNumber() +
                ", Size: " +
                page.getSize() +
                ", Total Pages:  " +
                page.getTotalPages()
        );

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/account/{accountId}/transactions-non-page")
    public ResponseEntity<List<FinanceTransaction>> getAccountTransactionsNonPage(@PathVariable(name = "accountId") String accountId)
        throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        log.info("getAccountTransactionsNonPage: " + accountId);
        final List<FinanceTransaction> page = financeAccountService.getTransactionsNonPage(u.get(), accountId);
        //log.info("getAccountTransactionsNonPage result: " + accountId + " - #" + page.getNumberOfElements() + ". Page: " + page.getNumber() + ", Size: " + page.getSize() + ", Total Pages:  "+ page.getTotalPages());

        //HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @GetMapping("/account/{accountId}/transactions-paging")
    public ResponseEntity<List<FinanceTransactionRowDTO>> getAccountTransactionsPaging(
        @PathVariable(name = "accountId") String accountId,
        @RequestParam(name = "filters", required = false) String filters,
        Pageable pageable
    ) throws IOException {
        log.info("getAccountTransactionsPaging: " + accountId);
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        final Page<FinanceTransactionRowDTO> dtoPage = financeAccountService.getTransactionsPaging(u.get(), accountId, pageable, filters);
        log.info(
            "getAccountTransactionsPaging result: " +
                accountId +
                " - #" +
                dtoPage.getNumberOfElements() +
                ". Page: " +
                dtoPage.getNumber() +
                ", Size: " +
                dtoPage.getSize() +
                ", Total Pages:  " +
                dtoPage.getTotalPages()
        );

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), dtoPage);
        return new ResponseEntity<>(dtoPage.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/transactions/editor-options")
    public ResponseEntity<FinanceTransactionEditorOptionsDTO> getTransactionEditorOptions() throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        return new ResponseEntity<>(financeTransactionService.getEditorOptions(u.get()), HttpStatus.OK);
    }

    @GetMapping("/transactions/editor-options/categories")
    public ResponseEntity<List<FinanceResourceDTO>> getTransactionCategoryOptions(
        @RequestParam(name = "query", required = false) String query,
        Pageable pageable
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        final Page<FinanceResourceDTO> page = financeTransactionService.getCategoryOptions(u.get(), query, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @PostMapping("/transactions/editor-options/categories")
    public ResponseEntity<FinanceResourceDTO> createTransactionCategory(@RequestBody FinanceCategoryCreateDTO request) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        try {
            FinanceResourceDTO category = financeTransactionService.createCategory(
                u.get(),
                request.getName(),
                request.getType(),
                request.getParentCategoryId()
            );
            return new ResponseEntity<>(category, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @GetMapping("/transactions/editor-options/categories-tree")
    public ResponseEntity<List<FinanceTreeNodeDTO>> getTransactionCategoryTreeOptions() throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        return new ResponseEntity<>(financeTransactionService.getCategoryTreeOptions(u.get()), HttpStatus.OK);
    }

    @GetMapping("/transactions/editor-options/who")
    public ResponseEntity<List<FinanceResourceDTO>> getTransactionWhoOptions(
        @RequestParam(name = "query", required = false) String query,
        Pageable pageable
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        final Page<FinanceResourceDTO> page = financeTransactionService.getWhoOptions(u.get(), query, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/transactions/editor-options/who-tree")
    public ResponseEntity<List<FinanceTreeNodeDTO>> getTransactionWhoTreeOptions() throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        return new ResponseEntity<>(financeTransactionService.getWhoTreeOptions(u.get()), HttpStatus.OK);
    }

    @GetMapping("/transactions/editor-options/payees")
    public ResponseEntity<List<FinanceResourceDTO>> getTransactionPayeeOptions(
        @RequestParam(name = "query", required = false) String query,
        Pageable pageable
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        final Page<FinanceResourceDTO> page = financeTransactionService.getPayeeOptions(u.get(), query, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/transactions/editor-options/tags")
    public ResponseEntity<List<FinanceResourceDTO>> getTransactionTagOptions(
        @RequestParam(name = "query", required = false) String query,
        Pageable pageable
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        final Page<FinanceResourceDTO> page = financeTransactionService.getTagOptions(u.get(), query, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<FinanceManageCategoryDTO>> getCategories() throws IOException {
        Optional<User> u = getCurrentUser();
        return new ResponseEntity<>(financeTransactionService.getManagedCategories(u.get()), HttpStatus.OK);
    }

    @PostMapping("/categories")
    public ResponseEntity<FinanceManageCategoryDTO> createCategory(@RequestBody FinanceManageCategoryUpdateDTO update) throws IOException {
        Optional<User> u = getCurrentUser();
        try {
            return new ResponseEntity<>(financeTransactionService.createManagedCategory(u.get(), update), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<FinanceManageCategoryDTO> updateCategory(
        @PathVariable(name = "categoryId") UUID categoryId,
        @RequestBody FinanceManageCategoryUpdateDTO update
    ) throws IOException {
        Optional<User> u = getCurrentUser();
        try {
            return new ResponseEntity<>(financeTransactionService.updateManagedCategory(u.get(), categoryId, update), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable(name = "categoryId") UUID categoryId) throws IOException {
        Optional<User> u = getCurrentUser();
        try {
            financeTransactionService.deleteManagedCategory(u.get(), categoryId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @GetMapping("/categories/{categoryId}/transactions")
    public ResponseEntity<List<FinanceTransactionRowDTO>> getCategoryTransactions(@PathVariable(name = "categoryId") UUID categoryId)
        throws IOException {
        Optional<User> u = getCurrentUser();
        try {
            return new ResponseEntity<>(financeTransactionService.getManagedCategoryTransactions(u.get(), categoryId), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/payees")
    public ResponseEntity<List<FinanceManagePayeeDTO>> getPayees(
        @RequestParam(name = "includeHidden", defaultValue = "false") boolean includeHidden
    ) throws IOException {
        Optional<User> u = getCurrentUser();
        return new ResponseEntity<>(financeTransactionService.getManagedPayees(u.get(), includeHidden), HttpStatus.OK);
    }

    @GetMapping("/payees/{payeeId}/transactions")
    public ResponseEntity<List<FinanceTransactionRowDTO>> getPayeeTransactions(@PathVariable(name = "payeeId") UUID payeeId)
        throws IOException {
        Optional<User> u = getCurrentUser();
        try {
            return new ResponseEntity<>(financeTransactionService.getManagedPayeeTransactions(u.get(), payeeId), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/payees")
    public ResponseEntity<FinanceManagePayeeDTO> createPayee(@RequestBody FinanceManagePayeeUpdateDTO update) throws IOException {
        Optional<User> u = getCurrentUser();
        try {
            return new ResponseEntity<>(financeTransactionService.createManagedPayee(u.get(), update), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/payees/{payeeId}")
    public ResponseEntity<FinanceManagePayeeDTO> updatePayee(
        @PathVariable(name = "payeeId") UUID payeeId,
        @RequestBody FinanceManagePayeeUpdateDTO update
    ) throws IOException {
        Optional<User> u = getCurrentUser();
        try {
            return new ResponseEntity<>(financeTransactionService.updateManagedPayee(u.get(), payeeId, update), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @DeleteMapping("/payees/{payeeId}")
    public ResponseEntity<Void> deletePayee(@PathVariable(name = "payeeId") UUID payeeId) throws IOException {
        Optional<User> u = getCurrentUser();
        try {
            financeTransactionService.deleteManagedPayee(u.get(), payeeId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/account/{accountId}/transactions/{transactionId}")
    public ResponseEntity<FinanceTransactionRowDTO> updateTransaction(
        @PathVariable(name = "accountId") String accountId,
        @PathVariable(name = "transactionId") UUID transactionId,
        @RequestBody FinanceTransactionUpdateDTO update
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        try {
            FinanceTransactionRowDTO saved = financeTransactionService.updateTransaction(u.get(), accountId, transactionId, update);
            return new ResponseEntity<>(saved, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @DeleteMapping("/account/{accountId}/transactions/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
        @PathVariable(name = "accountId") String accountId,
        @PathVariable(name = "transactionId") UUID transactionId
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        try {
            financeTransactionService.deleteTransaction(u.get(), accountId, transactionId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    private Optional<User> getCurrentUser() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));
        return userService.getUserWithAuthoritiesByLogin(userLogin);
    }

    @GetMapping("/account/{accountId}/transactions/{transactionId}/splits")
    public ResponseEntity<List<FinanceTransactionUpdateDTO.SplitLineDTO>> getTransactionSplits(
        @PathVariable(name = "accountId") String accountId,
        @PathVariable(name = "transactionId") UUID transactionId
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        try {
            List<FinanceTransactionUpdateDTO.SplitLineDTO> splits = financeTransactionService.getSplitTransactions(
                u.get(),
                accountId,
                transactionId
            );
            return new ResponseEntity<>(splits, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/account/{accountId}/transactions")
    public ResponseEntity<FinanceTransactionRowDTO> createTransaction(
        @PathVariable(name = "accountId") String accountId,
        @RequestBody FinanceTransactionUpdateDTO update
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        try {
            FinanceTransactionRowDTO saved = financeTransactionService.createTransaction(u.get(), accountId, update);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Indicators
    //

    @GetMapping("/accounts/indicators")
    public FinanceIndicatorsDTO accountIndicators(@RequestParam(name = "periodAgo", defaultValue = "") String periodAgo) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        LocalDate d = null;
        if (periodAgo != null && !"".equals(periodAgo)) {
            d = WebUIUtils.getDateFromPeriod(periodAgo);
        }

        FinanceIndicatorsDTO dto = new FinanceIndicatorsDTO();
        List<FinanceIndicatorDTO> indicators = new ArrayList<FinanceIndicatorDTO>();
        List<FinanceAccountDTO> accountsNow = financeAccountService.getAccountsOptimised(u.get(), true, null, false);
        removeAccountsWithRelatedBankAccounts(accountsNow);
        List<FinanceAccountDTO> accountsOnDate = null;

        if (d != null) {
            accountsOnDate = financeAccountService.getAccountsOptimised(u.get(), true, d, true);
            removeAccountsWithRelatedBankAccounts(accountsOnDate);
        }

        dto.setIndicators(indicators);

        this.financeAccountService.processBasicAccountValues(accountsNow, indicators);
        this.financeAccountService.processAccountValuesByType(accountsNow, indicators);
        if (accountsOnDate != null) {
            this.financeAccountService.updateBasicAccountValuesWithDelta(accountsOnDate, d, indicators);
            this.financeAccountService.updateAccountValuesByTypeWithDelta(accountsOnDate, d, indicators);
        }

        return dto;
        //` throw new RuntimeException("Unhandled Type");
    }

    @GetMapping("/incexp/indicators")
    public FinanceIndicatorsDTO incexpIndicators(@RequestParam(name = "periodAgo", defaultValue = "") String periodAgo) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        LocalDate d = null;
        if (periodAgo != null && !"".equals(periodAgo)) {
            d = WebUIUtils.getDateFromPeriod(periodAgo);
        }

        FinanceIndicatorsDTO dto = new FinanceIndicatorsDTO();
        List<FinanceIndicatorDTO> indicators = new ArrayList<FinanceIndicatorDTO>();

        this.financeAccountService.getCategorisedIncomeAndExpenses(u.get(), d, LocalDate.now());

        return dto;
    }

    private void removeAccountsWithRelatedBankAccounts(List<FinanceAccountDTO> accounts) {
        List<FinanceAccountDTO> toRemove = new ArrayList<FinanceAccountDTO>();
        for (FinanceAccountDTO a : accounts) {
            if (
                a.getRelatedToAccountId() != null &&
                a.getType() == FinanceAccountType.BANKING.getValue() &&
                !a.getName().endsWith(" (Cash)")
            ) {
                toRemove.add(a);
            }
        }

        accounts.removeAll(toRemove);
    }

    // @GetMapping("/snapshot-processor")
    // public FinanceSnapshotWithDelta snapshotProcessor(@RequestParam(required = true) String type, @RequestParam(defaultValue = "false") String classId, @RequestParam(defaultValue = "false") String resourceId, @RequestParam(name = "periodAgo", defaultValue = "") String periodAgo){
    //     String userLogin = SecurityUtils
    //     .getCurrentUserLogin()
    //     .orElseThrow(() -> new SecurityException("Current user login not found"));

    //     Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

    //     LocalDate d = null;
    //     if(periodAgo != null && !"".equals(periodAgo)) {
    //         d = FinanceSecurityResource.getDateFromPeriod(periodAgo);
    //     }

    //     if("sumAccountType".equals(type)) {
    //         //int accountType = FinanceAccountType.fromValueString(classId);
    //         FinanceSnapshotWithDelta snapshot = new FinanceSnapshotWithDelta(null);
    //         List<FinanceAccountDTO> accountsNow = financeAccountService.getAccountsOptimised(u.get(), true, null, false);
    //         List<FinanceAccountDTO> accountsOnDate = financeAccountService.getAccountsOptimised(u.get(), true, d, false);
    //         snapshot.setValue(sumAccountValues(accountsNow, classId));
    //         snapshot.setComparisonValue(sumAccountValues(accountsOnDate, classId));
    //         snapshot.setComparisonDate(d);
    //         return snapshot;
    //     }
    //     throw new RuntimeException("Unhandled Type");
    // }

    // Historical Indicator Views

    @GetMapping("/accounts/history")
    public Collection<FinanceSnapshotsPerResourceDTO> getHistoricalAccountsValues(
        @RequestParam(name = "periodAgo", defaultValue = "") String periodAgo
    ) throws IOException {
        log.info("getHistoricalAccountsValues");
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        LocalDate d = null;
        if (periodAgo != null && !"".equals(periodAgo)) {
            d = WebUIUtils.getDateFromPeriod(periodAgo);
        }

        Collection<FinanceSnapshotsPerResourceDTO> summaries = this.financeAccountService.getAllAccountsMonthlyRunningBalance(
            u.get(),
            d,
            LocalDate.now()
        );

        return summaries;
    }
}
