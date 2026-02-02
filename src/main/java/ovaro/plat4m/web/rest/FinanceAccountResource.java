package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ovaro.plat4m.domain.FinanceAccountType;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.IFinanceTransactionWithRunningBalance;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.FinanceAccountService;
import ovaro.plat4m.service.UserService;
import ovaro.plat4m.service.dto.FinanceAccountDTO;
import ovaro.plat4m.service.dto.FinanceIndicatorDTO;
import ovaro.plat4m.service.dto.FinanceIndicatorsDTO;
import ovaro.plat4m.service.dto.FinanceSnapshotsPerResourceDTO;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class FinanceAccountResource {

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final Logger log = LoggerFactory.getLogger(FinanceAccountResource.class);
    private FinanceAccountService financeAccountService;
    private final UserService userService;

    public FinanceAccountResource(FinanceAccountService financeAccountService, UserService userService) {
        this.financeAccountService = financeAccountService;
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
    public ResponseEntity<List<IFinanceTransactionWithRunningBalance>> getAccountTransactionsPaging(
        @PathVariable(name = "accountId") String accountId,
        Pageable pageable
    ) throws IOException {
        log.info("getAccountTransactionsPaging: " + accountId);
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        final Page<IFinanceTransactionWithRunningBalance> page = financeAccountService.getTransactionsPaging(u.get(), accountId, pageable);
        log.info(
            "getAccountTransactionsPaging result: " +
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

        Collection<FinanceSnapshotsPerResourceDTO> summaries =
            this.financeAccountService.getAllAccountsMonthlyRunningBalance(u.get(), d, LocalDate.now());

        return summaries;
    }
}
