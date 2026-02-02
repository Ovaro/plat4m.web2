package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ovaro.plat4m.domain.FinanceSecurity;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.FinanceSecurityIntegrationService;
import ovaro.plat4m.service.FinanceSecurityService;
import ovaro.plat4m.service.FinanceTransactionService;
import ovaro.plat4m.service.UserService;
import ovaro.plat4m.service.dto.FinanceInvestmentSnapshotDetails;
import ovaro.plat4m.service.dto.FinanceInvestmentSummaryDTO;
import ovaro.plat4m.service.dto.FinanceInvestmentTransactionDTO;
import ovaro.plat4m.service.idto.MultipleResultsException;

@RestController
@RequestMapping("/api")
public class FinanceSecurityResource {

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final Logger log = LoggerFactory.getLogger(FinanceSecurityResource.class);
    private FinanceSecurityService financeSecurityService;
    private FinanceTransactionService financeTransactionService;
    private FinanceSecurityIntegrationService financeSecurityIntegrationService;
    private final UserService userService;

    public FinanceSecurityResource(
        UserService userService,
        FinanceSecurityService financeSecurityService,
        FinanceTransactionService financeTransactionService,
        FinanceSecurityIntegrationService financeSecurityIntegrationService
    ) {
        this.financeSecurityService = financeSecurityService;
        this.financeSecurityIntegrationService = financeSecurityIntegrationService;
        this.userService = userService;
        this.financeTransactionService = financeTransactionService;
    }

    @GetMapping("/security-price")
    public ResponseEntity<FinanceSecurityPrice> getLatestBySymbol(@RequestParam String symbol) throws IOException {
        FinanceSecurityPrice sp;
        // if(symbol != null && symbol.contains("-")) {
        //     sp = this.financeSecurityService.getLatestBySecurityId(symbol);
        // } else {
        sp = this.financeSecurityService.getLatestBySymbol(symbol);
        //}

        return new ResponseEntity<>(sp, HttpStatus.OK);
    }

    @GetMapping("/security/load-security")
    public void loadSecurity(@RequestParam String symbol) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        try {
            FinanceSecurity security =
                this.financeSecurityIntegrationService.fetchSecurityInformation(symbol, u.get().getLocalCurrency(), null);
            log.info("Security identified:" + security);
        } catch (MultipleResultsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Get the transactions related to an investment/security.
     * @param id Security ID of the investment.
     * @param includeClosed Whether to include closed positions or not.
     * @return List of Transactions (DTOs)
     * @throws IOException
     */
    @GetMapping("/investment/{id}/transactions")
    public List<FinanceInvestmentTransactionDTO> investmentTransactions(
        @PathVariable String id,
        @RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        List<FinanceInvestmentTransactionDTO> dtos = this.financeTransactionService.findBySecurityId(u.get(), id);
        if (!includeClosed) {
            this.financeSecurityService.removeClosedTransactions(dtos);
        }
        return dtos;
    }

    /**
     * Investment Summary.
     * @param id Security ID of the investment.
     * @param includeClosed Whether to include closed positions or not.
     * @return Summary DTO
     * @throws IOException
     */
    @GetMapping("/investment/{id}/summary")
    public FinanceInvestmentSnapshotDetails investmentSummary(
        @PathVariable String id,
        @RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        StopWatch sw = new StopWatch();
        sw.setKeepTaskList(true);
        sw.start("investmentSummary");

        Optional<FinanceUserSecurity> ou = this.financeSecurityService.getUserSecurity(u.get(), id);
        if (ou.isPresent()) {
            FinanceUserSecurity usec = ou.get();
            if (!usec.isEventsValid()) {
                financeSecurityService.identifyAndSaveInvestmentEvents(u.get(), id);
                usec.setEventsValid(true);
                this.financeSecurityService.save(u.get(), usec);
            }

            FinanceInvestmentSnapshotDetails dto =
                this.financeSecurityService.processSummary(u.get(), usec, includeClosed, LocalDate.now());
            sw.stop();
            log.info(sw.prettyPrint());
            return dto;
        }

        return null;
    }
    // @GetMapping("/security-prices")
    // public ResponseEntity<List<FinanceSecurityPrice>> getLatestByIds(@RequestParam(defaultValue = "") String ids) throws IOException {
    //     List<FinanceSecurityPrice> sp = null;
    //     if(ids != null && ids.contains("-")) {
    //         // Check if can be split
    //         if(ids.contains(",")) {
    //             String[] idsArray = ids.split(",");
    //             sp = this.financeSecurityService.getLatestBySecurityIds(idsArray);
    //         } else {
    //             String[] arr = new String[1];
    //             arr[0] = ids;
    //             sp = this.financeSecurityService.getLatestBySecurityIds(arr);
    //         }
    //     } else {
    //        sp = this.financeSecurityService.getLatestAll();

    //     }

    //     return new ResponseEntity<>(sp, HttpStatus.OK);
    // }

    // @GetMapping("/investment/transactions-plus")
    // public Map<String, FinanceInvestmentTransactionsAndSummaryDTO> investmentTransactionsPlusAll(@RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed) throws IOException {
    //     String userLogin = SecurityUtils
    //     .getCurrentUserLogin()
    //     .orElseThrow(() -> new SecurityException("Current user login not found"));

    //     Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

    //     StopWatch sw = new StopWatch();
    //     sw.setKeepTaskList(true);
    //     sw.start("processTransactions-ALL");

    //     List<FinanceInvestmentTransactionDTO> dtos =  this.financeTransactionService.findByUser(u.get());
    //     // Split into security ids
    //     Map<String, List<FinanceInvestmentTransactionDTO>> split = new HashMap<String, List<FinanceInvestmentTransactionDTO>>();
    //     for(FinanceInvestmentTransactionDTO fit : dtos) {
    //         if(fit.getTransaction().getInvestmentActivityType() != FinanceInvestmentActivityType.INVALID) {
    //             List<FinanceInvestmentTransactionDTO> l = split.get(fit.getTransaction().getSecurityId());
    //             if(l==null) {
    //                 l = new ArrayList<FinanceInvestmentTransactionDTO>();
    //                 split.put(fit.getTransaction().getSecurityId(), l);
    //             }
    //             l.add(fit);
    //         }
    //     }

    //     Map<String, FinanceInvestmentTransactionsAndSummaryDTO> result = new HashMap<String, FinanceInvestmentTransactionsAndSummaryDTO>();
    //     for(String securityId : split.keySet()) {
    //         FinanceInvestmentTransactionsAndSummaryDTO dto = this.financeSecurityService.processTransactions(u.get(), securityId, dtos, includeClosed);
    //         result.put(securityId, dto);
    //     }

    //     sw.stop();
    //     log.info(sw.prettyPrint());

    //     return result;
    // }

    // @GetMapping("/investment/{id}/transactions-plus")
    // public FinanceInvestmentTransactionsAndSummaryDTO investmentTransactionsPlus(@PathVariable String id, @RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed) throws IOException {
    //     String userLogin = SecurityUtils
    //     .getCurrentUserLogin()
    //     .orElseThrow(() -> new SecurityException("Current user login not found"));

    //     Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
    //     StopWatch sw = new StopWatch();
    //     sw.setKeepTaskList(true);
    //     sw.start("processTransactions");
    //     List<FinanceInvestmentTransactionDTO> dtos =  this.financeTransactionService.findBySecurityId(u.get(), id);
    //     FinanceInvestmentTransactionsAndSummaryDTO dto = this.financeSecurityService.processTransactions(u.get(), id, dtos, includeClosed);
    //     sw.stop();
    //     log.info(sw.prettyPrint());
    //     return dto;
    // }

    //
    // Historical Data for graphs
    //
    // @GetMapping("/investment/portfolioValueOverTime")
    // public FinanceInvestmentPortfolioSummaryDTO investmentPortfolioValueOverTime(@RequestParam(required = false) String accountId, @RequestParam(name = "includeClosed", defaultValue = "true") boolean includeClosed, @RequestParam(name = "periodAgo", defaultValue = "") String periodAgo, @RequestParam(name = "interval", defaultValue = "P6M") String isoPeriod) throws IOException {
    //     String userLogin = SecurityUtils
    //     .getCurrentUserLogin()
    //     .orElseThrow(() -> new SecurityException("Current user login not found"));

    //     Period interval = Period.parse(isoPeriod);

    //     Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
    //     StopWatch sw = new StopWatch();
    //     sw.setKeepTaskList(true);

    //     List<FinanceUserSecurity> userSecurities = checkInvestmentEventsAreGenerated(u.get(), accountId, sw);

    //     LocalDate d = getDateFromPeriod(periodAgo);
    //     sw.start("investmentPortfolioValueOverTime");

    //     // Select in-scope investment events
    //     //      Save as point annotations
    //     //      Save values into primary value series
    //     // Generate dates based on intervals (e.g. every six months)
    //     //      Select equity prices for interval dates (e.g. default to last know date - i.e. limit 1 < date)
    //     //      Create value for each security at each point
    //     // Roll into groupings (either on server or on client - if on client need all grouping data to be included)
    //     FinanceInvestmentPortfolioSummaryDTO result = this.financeSecurityService.portfolioSummaries(u.get(), userSecurities, includeClosed, d); //.minusYears(5)

    //     sw.stop();
    //     log.info(sw.prettyPrint());
    //     return result;

    // }

}
