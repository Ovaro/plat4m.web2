package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
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
import ovaro.plat4m.domain.FinanceSecurity;
import ovaro.plat4m.domain.FinanceSecurityInvestmentSummary;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceSecurityPriceRepository;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.FinanceSecurityIntegrationService;
import ovaro.plat4m.service.FinanceSecurityPriceRefreshService;
import ovaro.plat4m.service.FinanceSecurityService;
import ovaro.plat4m.service.FinanceTransactionService;
import ovaro.plat4m.service.UserService;
import ovaro.plat4m.service.dto.FinanceInvestmentSnapshotDetails;
import ovaro.plat4m.service.dto.FinanceInvestmentSummaryDTO;
import ovaro.plat4m.service.dto.FinanceInvestmentTransactionDTO;
import ovaro.plat4m.service.dto.FinanceSecurityPriceRefreshRequestDTO;
import ovaro.plat4m.service.dto.FinanceSecurityPriceRefreshResultDTO;
import ovaro.plat4m.service.dto.FinanceSecurityPriceUpdateDTO;
import ovaro.plat4m.service.dto.FinanceSecurityPriceViewDTO;
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
    private final FinanceSecurityPriceRefreshService financeSecurityPriceRefreshService;
    private final FinanceSecurityPriceRepository financeSecurityPriceRepository;
    private final UserService userService;

    public FinanceSecurityResource(
        UserService userService,
        FinanceSecurityService financeSecurityService,
        FinanceTransactionService financeTransactionService,
        FinanceSecurityIntegrationService financeSecurityIntegrationService,
        FinanceSecurityPriceRefreshService financeSecurityPriceRefreshService,
        FinanceSecurityPriceRepository financeSecurityPriceRepository
    ) {
        this.financeSecurityService = financeSecurityService;
        this.financeSecurityIntegrationService = financeSecurityIntegrationService;
        this.userService = userService;
        this.financeTransactionService = financeTransactionService;
        this.financeSecurityPriceRefreshService = financeSecurityPriceRefreshService;
        this.financeSecurityPriceRepository = financeSecurityPriceRepository;
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
        try {
            FinanceSecurity security = this.financeSecurityIntegrationService.fetchSecurityInformation(
                symbol,
                getCurrentUser().getLocalCurrency(),
                null
            );
            log.info("Security identified:" + security);
        } catch (MultipleResultsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @PostMapping("/security-prices/refresh")
    public ResponseEntity<FinanceSecurityPriceRefreshResultDTO> refreshSecurityPrices(
        @RequestBody(required = false) FinanceSecurityPriceRefreshRequestDTO request
    ) throws IOException {
        try {
            FinanceSecurityPriceRefreshResultDTO result = this.financeSecurityPriceRefreshService.refreshQuotes(getCurrentUser(), request);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
        }
    }

    @PostMapping("/security-prices/refresh/start")
    public ResponseEntity<FinanceSecurityPriceRefreshResultDTO> startRefreshSecurityPrices(
        @RequestBody(required = false) FinanceSecurityPriceRefreshRequestDTO request
    ) {
        try {
            FinanceSecurityPriceRefreshResultDTO result = this.financeSecurityPriceRefreshService.startRefreshQuotesAsync(
                getCurrentUser(),
                request
            );
            return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
        }
    }

    @GetMapping("/security-prices/refresh/{jobId}")
    public ResponseEntity<FinanceSecurityPriceRefreshResultDTO> getRefreshSecurityPricesStatus(@PathVariable String jobId) {
        try {
            FinanceSecurityPriceRefreshResultDTO result = this.financeSecurityPriceRefreshService.getRefreshJobStatus(
                getCurrentUser(),
                jobId
            );
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/security-prices/refresh/{jobId}/items/{userSecurityId}/selection")
    public ResponseEntity<FinanceSecurityPriceRefreshResultDTO> updateRefreshSecurityPriceSelection(
        @PathVariable String jobId,
        @PathVariable String userSecurityId,
        @RequestParam boolean selected
    ) {
        try {
            FinanceSecurityPriceRefreshResultDTO result = this.financeSecurityPriceRefreshService.updateRefreshJobSelection(
                getCurrentUser(),
                jobId,
                userSecurityId,
                selected
            );
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/security-prices/refresh/{jobId}/apply")
    public ResponseEntity<FinanceSecurityPriceRefreshResultDTO> applyRefreshSecurityPrices(@PathVariable String jobId) {
        try {
            FinanceSecurityPriceRefreshResultDTO result = this.financeSecurityPriceRefreshService.applyRefreshJobResults(
                getCurrentUser(),
                jobId
            );
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @GetMapping("/investment/{id}/prices")
    public ResponseEntity<List<FinanceSecurityPriceViewDTO>> listInvestmentPrices(@PathVariable String id) {
        User user = getCurrentUser();
        String symbol = resolveStoredSymbol(user, id);
        List<FinanceSecurityPriceViewDTO> prices = financeSecurityPriceRepository
            .findBySymbolOrderByDateDesc(symbol)
            .stream()
            .map(this::toPriceView)
            .collect(Collectors.toList());
        return new ResponseEntity<>(prices, HttpStatus.OK);
    }

    @PostMapping("/investment/{id}/prices")
    public ResponseEntity<FinanceSecurityPriceViewDTO> createInvestmentPrice(
        @PathVariable String id,
        @RequestBody FinanceSecurityPriceUpdateDTO update
    ) {
        User user = getCurrentUser();
        String symbol = resolveStoredSymbol(user, id);
        FinanceSecurityPrice price = new FinanceSecurityPrice();
        applyPriceUpdate(price, symbol, update);
        return new ResponseEntity<>(toPriceView(financeSecurityPriceRepository.save(price)), HttpStatus.CREATED);
    }

    @PutMapping("/investment/{id}/prices/{priceId}")
    public ResponseEntity<FinanceSecurityPriceViewDTO> updateInvestmentPrice(
        @PathVariable String id,
        @PathVariable UUID priceId,
        @RequestBody FinanceSecurityPriceUpdateDTO update
    ) {
        User user = getCurrentUser();
        String symbol = resolveStoredSymbol(user, id);
        FinanceSecurityPrice price = financeSecurityPriceRepository
            .findById(priceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock price not found"));
        if (!symbol.equals(price.getSymbol())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock price does not belong to the selected holding");
        }
        applyPriceUpdate(price, symbol, update);
        return new ResponseEntity<>(toPriceView(financeSecurityPriceRepository.save(price)), HttpStatus.OK);
    }

    @DeleteMapping("/investment/{id}/prices/{priceId}")
    public ResponseEntity<Void> deleteInvestmentPrice(@PathVariable String id, @PathVariable UUID priceId) {
        User user = getCurrentUser();
        String symbol = resolveStoredSymbol(user, id);
        FinanceSecurityPrice price = financeSecurityPriceRepository
            .findById(priceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock price not found"));
        if (!symbol.equals(price.getSymbol())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock price does not belong to the selected holding");
        }
        financeSecurityPriceRepository.delete(price);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
        List<FinanceInvestmentTransactionDTO> dtos = this.financeTransactionService.findBySecurityId(getCurrentUser(), id);
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
        StopWatch sw = new StopWatch();
        sw.setKeepTaskList(true);
        sw.start("investmentSummary");

        User user = getCurrentUser();
        Optional<FinanceUserSecurity> ou = this.financeSecurityService.getUserSecurity(user, id);
        if (ou.isPresent()) {
            FinanceUserSecurity usec = ou.get();
            if (!usec.isEventsValid()) {
                financeSecurityService.identifyAndSaveInvestmentEvents(user, id);
                usec.setEventsValid(true);
                this.financeSecurityService.save(user, usec);
            }

            FinanceInvestmentSnapshotDetails dto = this.financeSecurityService.processSummary(user, usec, includeClosed, LocalDate.now());
            sw.stop();
            log.info(sw.prettyPrint());
            return dto;
        }

        return null;
    }

    private User getCurrentUser() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));
        return userService.getUserWithAuthoritiesByLogin(userLogin).orElseThrow(() -> new SecurityException("Current user not found"));
    }

    private FinanceSecurityPriceViewDTO toPriceView(FinanceSecurityPrice price) {
        FinanceSecurityPriceViewDTO dto = new FinanceSecurityPriceViewDTO();
        dto.setId(price.getId());
        dto.setSymbol(price.getSymbol());
        dto.setDate(price.getDate() != null ? price.getDate().toLocalDate() : null);
        dto.setPrice(price.getPrice());
        dto.setComment(price.getComment());
        return dto;
    }

    private void applyPriceUpdate(FinanceSecurityPrice price, String symbol, FinanceSecurityPriceUpdateDTO update) {
        if (update == null || update.getDate() == null || update.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date and price are required");
        }
        if (update.getPrice().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price cannot be negative");
        }
        ZonedDateTime priceDateTime = update.getDate().atStartOfDay(ZoneOffset.UTC);
        price.setSymbol(symbol);
        price.setDate(priceDateTime);
        price.setSerialDateTime(ZonedDateTime.now(ZoneOffset.UTC));
        price.setPrice(update.getPrice());
        price.setOpen(update.getPrice());
        price.setClose(update.getPrice());
        price.setHigh(update.getPrice());
        price.setLow(update.getPrice());
        price.setComment(update.getComment() != null && !update.getComment().isBlank() ? update.getComment().trim() : null);
    }

    private String resolveStoredSymbol(User user, String userSecurityId) {
        FinanceUserSecurity userSecurity = financeSecurityService
            .getUserSecurity(user, userSecurityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investment not found"));

        String securitySymbol = userSecurity.getSecurity() != null ? normalize(userSecurity.getSecurity().getSymbol()) : null;
        if (securitySymbol != null && !isSyntheticSymbol(securitySymbol)) {
            return normalizeMarketSymbol(securitySymbol);
        }

        List<FinanceSecurityInvestmentSummary> summaries = financeTransactionService.getFinanceSecurityInvestmentTransactions(
            user,
            false,
            null
        );
        for (FinanceSecurityInvestmentSummary summary : summaries) {
            if (summary.getSecurityId() != null && summary.getSecurityId().equals(userSecurityId)) {
                String summarySymbol = normalizeMarketSymbol(summary.getSymbol());
                if (summarySymbol != null) {
                    return summarySymbol;
                }
                String userSymbol = normalizeMarketSymbol(summary.getUserSymbol());
                if (userSymbol != null) {
                    return userSymbol;
                }
            }
        }

        String directSymbol = normalizeMarketSymbol(userSecurity.getSymbol());
        if (directSymbol != null) {
            return directSymbol;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No stored stock symbol could be resolved for this holding");
    }

    private String normalizeMarketSymbol(String symbol) {
        String normalized = normalize(symbol);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > 3 && normalized.charAt(2) == ':') {
            normalized = normalized.substring(3);
        }
        int dotIndex = normalized.indexOf('.');
        if (dotIndex > 0) {
            normalized = normalized.substring(0, dotIndex);
        }
        return normalize(normalized);
    }

    private boolean isSyntheticSymbol(String symbol) {
        String normalized = normalize(symbol);
        return normalized != null && normalized.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:.+");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
