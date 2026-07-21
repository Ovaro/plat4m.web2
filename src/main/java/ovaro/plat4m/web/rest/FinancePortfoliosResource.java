package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.FinanceAccountService;
import ovaro.plat4m.service.FinanceCustomPortfolioService;
import ovaro.plat4m.service.FinancePortfolioTradeService;
import ovaro.plat4m.service.FinanceSecurityService;
import ovaro.plat4m.service.UserService;
import ovaro.plat4m.service.dto.FinanceCustomPortfolioDTO;
import ovaro.plat4m.service.dto.FinanceCustomPortfolioOptionsDTO;
import ovaro.plat4m.service.dto.FinanceIndicatorDTO;
import ovaro.plat4m.service.dto.FinanceIndicatorsDTO;
import ovaro.plat4m.service.dto.FinanceInvestmentPortfolioSummaryDTO;
import ovaro.plat4m.service.dto.FinanceInvestmentSnapshotDetails;
import ovaro.plat4m.service.dto.FinancePortfolioTradeDTO;
import ovaro.plat4m.service.dto.FinanceSecurityHoldingDTO;
import ovaro.plat4m.service.dto.FinanceSnapshotWithComparison;
import ovaro.plat4m.service.dto.FinanceSnapshotsPerResourceDTO;

@RestController
@RequestMapping("/api")
public class FinancePortfoliosResource {

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final Logger log = LoggerFactory.getLogger(FinanceSecurityResource.class);
    private FinanceSecurityService financeSecurityService;
    private FinanceAccountService financeAccountService;
    private FinancePortfolioTradeService financePortfolioTradeService;
    private FinanceCustomPortfolioService financeCustomPortfolioService;
    private final UserService userService;

    public FinancePortfoliosResource(
        UserService userService,
        FinanceSecurityService financeSecurityService,
        FinanceAccountService financeAccountService,
        FinancePortfolioTradeService financePortfolioTradeService,
        FinanceCustomPortfolioService financeCustomPortfolioService
    ) {
        this.financeSecurityService = financeSecurityService;
        this.userService = userService;
        this.financeAccountService = financeAccountService;
        this.financePortfolioTradeService = financePortfolioTradeService;
        this.financeCustomPortfolioService = financeCustomPortfolioService;
    }

    @GetMapping("/custom-portfolios")
    public List<FinanceCustomPortfolioDTO> customPortfolios() {
        return financeCustomPortfolioService.findAll(currentUser());
    }

    @GetMapping("/custom-portfolios/{id}")
    public FinanceCustomPortfolioDTO customPortfolio(@PathVariable UUID id) {
        return financeCustomPortfolioService.findOne(currentUser(), id);
    }

    @PostMapping("/custom-portfolios")
    public ResponseEntity<FinanceCustomPortfolioDTO> createCustomPortfolio(@RequestBody FinanceCustomPortfolioDTO dto) {
        try {
            return new ResponseEntity<>(financeCustomPortfolioService.create(currentUser(), dto), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/custom-portfolios/{id}")
    public FinanceCustomPortfolioDTO updateCustomPortfolio(@PathVariable UUID id, @RequestBody FinanceCustomPortfolioDTO dto) {
        try {
            return financeCustomPortfolioService.update(currentUser(), id, dto);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @DeleteMapping("/custom-portfolios/{id}")
    public ResponseEntity<Void> deleteCustomPortfolio(@PathVariable UUID id) {
        financeCustomPortfolioService.delete(currentUser(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/custom-portfolios/options")
    public FinanceCustomPortfolioOptionsDTO customPortfolioOptions() {
        return financeCustomPortfolioService.getOptions(currentUser());
    }

    @GetMapping("/portfolios")
    public ResponseEntity<Collection<FinanceSecurityHoldingDTO>> getInvestmentHoldings(
        @RequestParam(required = false) String accountId,
        @RequestParam(required = false) UUID customPortfolioId,
        @RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed,
        @RequestParam(name = "periodAgo", defaultValue = "") String periodAgo
    ) throws IOException {
        User user = currentUser();
        validatePortfolioFilter(accountId, customPortfolioId);

        log.debug("getInvestmentHoldings: " + accountId);

        LocalDate d = WebUIUtils.getDateFromPeriod(periodAgo);

        Collection<FinanceSecurityHoldingDTO> result =
            customPortfolioId != null
                ? financeCustomPortfolioService.getHoldings(user, customPortfolioId, includeClosed, d)
                : financeAccountService.investmentAccountHoldings(user, accountId, includeClosed, null);
        log.debug("getInvestmentHoldings result: " + accountId + " - #" + result.size());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/portfolios/trades")
    public ResponseEntity<List<FinancePortfolioTradeDTO>> getInvestmentTrades(
        @RequestParam(required = false) String accountId,
        @RequestParam(required = false) UUID customPortfolioId,
        @RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed
    ) {
        User user = currentUser();
        validatePortfolioFilter(accountId, customPortfolioId);
        Set<String> customSecurityIds =
            customPortfolioId != null ? financeCustomPortfolioService.getSecurityIdStrings(user, customPortfolioId, true) : null;
        List<FinancePortfolioTradeDTO> result = financePortfolioTradeService.getTrades(user, accountId, includeClosed, customSecurityIds);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/portfolios/summaries")
    public FinanceInvestmentPortfolioSummaryDTO investmentSummaries(
        @RequestParam(required = false) String accountId,
        @RequestParam(required = false) UUID customPortfolioId,
        @RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed,
        @RequestParam(name = "periodAgo", defaultValue = "") String periodAgo
    ) throws IOException {
        User user = currentUser();
        validatePortfolioFilter(accountId, customPortfolioId);
        StopWatch sw = new StopWatch();
        sw.setKeepTaskList(true);

        List<FinanceUserSecurity> userSecurities =
            customPortfolioId != null
                ? checkInvestmentEventsAreGenerated(
                      user,
                      financeCustomPortfolioService.getUserSecurities(user, customPortfolioId, true),
                      sw
                  )
                : checkInvestmentEventsAreGenerated(user, accountId, sw);

        LocalDate d = WebUIUtils.getDateFromPeriod(periodAgo);
        sw.start("investmentSummaries");
        FinanceInvestmentPortfolioSummaryDTO result = this.financeSecurityService.portfolioSummaries(
            user,
            userSecurities,
            includeClosed,
            d
        ); //.minusYears(5)
        if (customPortfolioId != null) {
            result = financeCustomPortfolioService.addCashToSummary(user, customPortfolioId, result, d);
        }

        sw.stop();
        log.info(sw.prettyPrint());
        return result;
    }

    //
    // Historical Data for graphs
    //
    @GetMapping("/portfolios/history")
    public Collection<FinanceSnapshotsPerResourceDTO> investmentHistory(
        @RequestParam(required = false) String accountId,
        @RequestParam(required = false) UUID customPortfolioId,
        @RequestParam(required = false) String userSecurityId,
        @RequestParam(name = "periodAgo") String periodAgo,
        @RequestParam(name = "numberOfPeriods", defaultValue = "30") String numberOfPeriods,
        @RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed
    ) throws IOException {
        log.warn(
            "investmentHistory request received. accountId={}, userSecurityId={}, periodAgo={}, numberOfPeriods={}, includeClosed={}",
            accountId,
            userSecurityId,
            periodAgo,
            numberOfPeriods,
            includeClosed
        );
        User user = currentUser();
        validatePortfolioFilter(accountId, customPortfolioId);
        StopWatch sw = new StopWatch();
        sw.setKeepTaskList(true);

        int numberOfPeriodsInt = Integer.parseInt(numberOfPeriods);

        List<FinanceUserSecurity> userSecurities = null;
        if (userSecurityId == null) {
            userSecurities =
                customPortfolioId != null
                    ? checkInvestmentEventsAreGenerated(
                          user,
                          financeCustomPortfolioService.getUserSecurities(user, customPortfolioId, false),
                          sw
                      )
                    : checkInvestmentEventsAreGenerated(user, accountId, sw);
            userSecurities = userSecurities
                .stream()
                .filter(usec -> !usec.isIgnoredForRollup())
                .toList();
        } else {
            Optional<FinanceUserSecurity> osec = this.financeSecurityService.getUserSecurity(user, userSecurityId);
            if (osec.isPresent()) {
                checkInvestmentEventsAreGeneratedForUserSecurity(user, osec.get());
                userSecurities = convertUserSecurityToList(osec);
            } else {
                log.warn("No user security found for history request. userSecurityId={}", userSecurityId);
                return List.of();
            }
        }

        log.warn(
            "investmentHistory resolved user securities. count={}, accountId={}, userSecurityId={}",
            userSecurities != null ? userSecurities.size() : null,
            accountId,
            userSecurityId
        );

        LocalDate d = null;

        if (periodAgo != null && !"".equals(periodAgo)) {
            d = WebUIUtils.getDateFromPeriod(periodAgo);
        }

        sw.start("historicalInvestmentValues");
        List<FinanceSnapshotsPerResourceDTO> history = new ArrayList<>(
            this.financeSecurityService.historicalInvestmentValues(
                user,
                d,
                LocalDate.now(),
                userSecurities,
                numberOfPeriodsInt,
                true,
                includeClosed
            )
        ); //.minusYears(5)
        if (customPortfolioId != null) {
            history.addAll(financeCustomPortfolioService.getCashHistory(user, customPortfolioId, d, LocalDate.now()));
        }

        sw.stop();
        log.info(sw.prettyPrint());
        log.warn(
            "investmentHistory response prepared. resourceCount={}, accountId={}, userSecurityId={}, periodAgo={}",
            history != null ? history.size() : null,
            accountId,
            userSecurityId,
            periodAgo
        );
        return history;
    }

    // XX Up to here.
    // // Need to process aggregates for investments. ATM it uses the account values that come back in the accounts call which is wrong for investments.
    // // Should probably stop them being returned by the account service as non-sensical. Eg:
    // // 'sumAccountType-investment' or investment type accounts

    // public List<FinanceSnapshotsPerResourceDTO> processAggregates(List<FinanceUserSecurity> userSecurities, Collection<FinanceSnapshotsPerResourceDTO> resourceSnapshots) {
    //     Map<String, FinanceSnapshotsPerResourceDTO> indexedResourceSnapshots  = this.indexResourceSnapshotsByUserSecurityId(resourceSnapshots);
    //     Map<String, FinanceSnapshotsPerResourceDTO> indexByType = new HashMap<String, FinanceSnapshotsPerResourceDTO>();
    //     for(FinanceAccountType type : FinanceAccountType.values()) {
    //         String typeStr = type.getValueString();
    //         FinanceSnapshotsPerResourceDTO typeResourceSnapshot = indexByType.get(typeStr);

    //         if(typeResourceSnapshot == null) {
    //             typeResourceSnapshot = new FinanceSnapshotsPerResourceDTO();
    //             typeResourceSnapshot.setResourceId("sumAccountType-" + typeStr);
    //             typeResourceSnapshot.setResourceName(typeStr);
    //             typeResourceSnapshot.setIndicatorType("sumAccountType");
    //             indexByType.put(typeStr, typeResourceSnapshot);
    //         }

    //         for(FinanceAccount account : accounts) {
    //             if(type.getValue() == account.getType() && (type.getValue() != FinanceAccountType.BANKING.getValue() || (type.getValue() == FinanceAccountType.BANKING.getValue() && account.getRelatedToAccountId() == null))){
    //                 FinanceSnapshotsPerResourceDTO rDTO = indexedResourceSnapshots.get(account.getId().toString());
    //                 if(rDTO == null) {
    //                     log.warn("Couldn't find rDTO for account: " + account.getName() + "[" + account.getId().toString() + "]");
    //                 } else {
    //                     if(type.getValue() == FinanceAccountType.BANKING.getValue()) {
    //                         FinanceSnapshot s = null;
    //                         if(rDTO.getSnapshots() != null && rDTO.getSnapshots().size() > 0) {
    //                             s = rDTO.getSnapshots().get(rDTO.getSnapshots().size()-1);
    //                         }
    //                         if(s != null) {
    //                             log.info("Adding account: " + account.getName() + "[" + account.getId().toString() + "] to sumAccountType-xxx: " + s.getValue() + " > " + s.getFxToLocal());
    //                         }
    //                     }
    //                     addToExistingSnapshots(typeResourceSnapshot, rDTO.getSnapshots());
    //                 }
    //             }
    //         }
    //     }
    //     Collection<FinanceSnapshotsPerResourceDTO> newResourceSnapshots = indexByType.values();
    //     List<FinanceSnapshotsPerResourceDTO> result = new ArrayList<FinanceSnapshotsPerResourceDTO>();
    //     for(FinanceSnapshotsPerResourceDTO a : resourceSnapshots) {
    //         result.add(a);
    //     }
    //     result.addAll(newResourceSnapshots);
    //     return result;
    // }

    public Map<String, FinanceSnapshotsPerResourceDTO> indexResourceSnapshotsByUserSecurityId(
        Collection<FinanceSnapshotsPerResourceDTO> resourceSnapshots
    ) {
        Map<String, FinanceSnapshotsPerResourceDTO> indexed = new HashMap<String, FinanceSnapshotsPerResourceDTO>(resourceSnapshots.size());
        for (FinanceSnapshotsPerResourceDTO dto : resourceSnapshots) {
            indexed.put(dto.getId(), dto);
        }
        return indexed;
    }

    private List<FinanceUserSecurity> checkInvestmentEventsAreGenerated(User u, String accountId, StopWatch sw) {
        sw.start("getUserSecurities");

        List<FinanceUserSecurity> userSecurities = this.financeSecurityService.getUserSecurities(u, accountId);

        sw.stop();
        sw.start("identifyAndSaveEvents");
        for (FinanceUserSecurity usec : userSecurities) {
            checkInvestmentEventsAreGeneratedForUserSecurity(u, usec);
        }
        sw.stop();
        return userSecurities;
    }

    private List<FinanceUserSecurity> checkInvestmentEventsAreGenerated(User u, List<FinanceUserSecurity> userSecurities, StopWatch sw) {
        sw.start("identifyAndSaveEvents");
        for (FinanceUserSecurity usec : userSecurities) {
            checkInvestmentEventsAreGeneratedForUserSecurity(u, usec);
        }
        sw.stop();
        return userSecurities;
    }

    private List<FinanceUserSecurity> convertUserSecurityToList(Optional<FinanceUserSecurity> ousec) {
        List<FinanceUserSecurity> userSecurities = new ArrayList<FinanceUserSecurity>();
        if (ousec.isPresent()) {
            userSecurities.add(ousec.get());
        }

        return userSecurities;
    }

    // private List<FinanceUserSecurity> retrieveUserSecurityId(User u, String userSecurityId, StopWatch sw) {
    //     List<FinanceUserSecurity> userSecurities = new ArrayList<FinanceUserSecurity>();
    //     Optional<FinanceUserSecurity> ousec = this.financeSecurityService.getUserSecurity(u, userSecurityId);
    //     if(ousec.isPresent()) {
    //         userSecurities.add(ousec.get());
    //     }

    //     return userSecurities;
    // }

    private void checkInvestmentEventsAreGeneratedForUserSecurity(User u, FinanceUserSecurity usec) {
        if (!usec.isEventsValid()) {
            financeSecurityService.identifyAndSaveInvestmentEvents(u, usec.getId().toString());
        }
    }

    @GetMapping("/portfolios/identity-investment-events")
    public String investmentEvents(@RequestParam(name = "includeClosed", defaultValue = "false") boolean includeClosed) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        StopWatch sw = new StopWatch();
        sw.setKeepTaskList(true);
        sw.start("identifyAndSaveInvestmentEvents-ALL");
        financeSecurityService.identifyAndSaveInvestmentEvents(u.get());
        sw.stop();
        log.info(sw.prettyPrint());
        return "OK";
    }

    @GetMapping("/portfolios/identity-investment-events/{id}")
    public String investmentEvents(@PathVariable String id) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        StopWatch sw = new StopWatch();
        sw.setKeepTaskList(true);
        sw.start("identifyAndSaveInvestmentEvents:" + id);
        financeSecurityService.identifyAndSaveInvestmentEvents(u.get(), id);

        Optional<FinanceUserSecurity> ou = this.financeSecurityService.getUserSecurity(u.get(), id);
        if (ou.isPresent()) {
            FinanceUserSecurity usec = ou.get();
            usec.setEventsValid(true);
            this.financeSecurityService.save(u.get(), usec);
        }
        sw.stop();
        log.info(sw.prettyPrint());
        return "OK";
    }

    @GetMapping("/portfolios/indicators")
    public FinanceIndicatorsDTO portfolioIndicators(@RequestParam(name = "periodAgo", defaultValue = "") String periodAgo) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);
        StopWatch sw = new StopWatch();
        sw.setKeepTaskList(true);

        LocalDate d = null;

        if (periodAgo != null && !"".equals(periodAgo)) {
            d = WebUIUtils.getDateFromPeriod(periodAgo);
        }

        FinanceIndicatorsDTO dto = new FinanceIndicatorsDTO();

        List<FinanceUserSecurity> userSecurities = checkInvestmentEventsAreGenerated(u.get(), (String) null, sw);
        Map<String, FinanceUserSecurity> indexedUS = indexUserSecurities(userSecurities);

        sw.start("investmentSummaries");
        FinanceInvestmentPortfolioSummaryDTO result = this.financeSecurityService.portfolioSummaries(
            u.get(),
            userSecurities,
            true,
            LocalDate.now()
        ); //.minusYears(5)

        List<FinanceIndicatorDTO> indicators = new ArrayList<FinanceIndicatorDTO>();
        this.processSummaryValues(result.getSummaries(), indicators, indexedUS);

        if (d != null) {
            FinanceInvestmentPortfolioSummaryDTO comparisonResult = this.financeSecurityService.portfolioSummaries(
                u.get(),
                userSecurities,
                true,
                d
            );
            this.updateBasicValuesWithDelta(comparisonResult.getSummaries(), d, indicators);
        }

        dto.setIndicators(indicators);

        sw.stop();
        log.info(sw.prettyPrint());
        return dto;
        //` throw new RuntimeException("Unhandled Type");
    }

    // Account Indicators
    public void processSummaryValues(
        List<FinanceInvestmentSnapshotDetails> investmentSnapshots,
        List<FinanceIndicatorDTO> indicators,
        Map<String, FinanceUserSecurity> indexedUS
    ) {
        for (FinanceInvestmentSnapshotDetails isnap : investmentSnapshots) {
            if (isnap != null) {
                FinanceIndicatorDTO iDTO = new FinanceIndicatorDTO();
                iDTO.setId(isnap.getUserSecurityId());
                //iDTO.setResourceName(isnap.getName());
                iDTO.setType("value");
                FinanceUserSecurity fus = indexedUS.get(isnap.getUserSecurityId());
                if (fus != null) {
                    iDTO.setName(fus.getName());
                    iDTO.setCurrencyCode(fus.getCurrencyCode());
                    iDTO.setSymbol(fus.getSymbol());
                }
                FinanceSnapshotWithComparison financeSnapshotWithDelta = new FinanceSnapshotWithComparison(null);

                financeSnapshotWithDelta.setValue(isnap.getPrice().multiply(BigDecimal.valueOf(isnap.getQuantity())));

                if (isnap.getFxToLocal() != null) {
                    financeSnapshotWithDelta.setFxToLocal(isnap.getFxToLocal());
                    // financeSnapshotWithDelta.setValue(account.getBalance().multiply(new BigDecimal(account.getFxRateToLocal())));
                    financeSnapshotWithDelta.setCurrencyIsoCode(isnap.getCurrencyIsoCode());
                    if (isnap.getFxDate() != null) {
                        financeSnapshotWithDelta.setFxDate(isnap.getFxDate());
                    }
                }

                iDTO.setSnapshot(financeSnapshotWithDelta);
                indicators.add(iDTO);
            }
        }
    }

    public void updateBasicValuesWithDelta(
        List<FinanceInvestmentSnapshotDetails> investmentSnapshots,
        LocalDate date,
        List<FinanceIndicatorDTO> indicators
    ) {
        for (FinanceInvestmentSnapshotDetails isnap : investmentSnapshots) {
            if (isnap != null) {
                for (FinanceIndicatorDTO iDTO : indicators) {
                    if (iDTO.getId().equals(isnap.getUserSecurityId())) {
                        if (iDTO.getSnapshot() != null) {
                            iDTO.getSnapshot().setComparisonDate(date);

                            if (isnap.getPrice() != null && isnap.getQuantity() != null) {
                                iDTO.getSnapshot().setComparisonValue(isnap.getPrice().multiply(BigDecimal.valueOf(isnap.getQuantity())));
                            } else {
                                iDTO.getSnapshot().setComparisonValue(BigDecimal.ZERO);
                            }

                            if (isnap.getFxToLocal() != null) {
                                iDTO.getSnapshot().setComparisonFxToLocal(isnap.getFxToLocal());
                                //iDTO.getSnapshot().setComparisonValue(iDTO.getSnapshot().getComparisonValue().multiply(new BigDecimal(account.getFxRateToLocal())));
                                if (isnap.getFxDate() != null) {
                                    iDTO.getSnapshot().setComparisonFxDate(isnap.getFxDate());
                                }
                            }
                        } else {
                            log.warn("No original snapshot for: " + isnap.getUserSecurityId() + " to add delta to.");
                        }
                        break;
                    }
                }
            }
        }
        // Make sure all zero
        for (FinanceIndicatorDTO iDTO : indicators) {
            if (iDTO.getSnapshot() != null && iDTO.getSnapshot().getComparisonValue() == null) {
                iDTO.getSnapshot().setComparisonValue(BigDecimal.ZERO);
                iDTO.getSnapshot().setComparisonDate(date);
            }
        }
    }

    public static Map<String, FinanceUserSecurity> indexUserSecurities(List<FinanceUserSecurity> userSecurities) {
        Map<String, FinanceUserSecurity> index = new HashMap<String, FinanceUserSecurity>();
        for (FinanceUserSecurity fus : userSecurities) {
            index.put(fus.getId().toString(), fus);
        }
        return index;
    }

    private User currentUser() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));
        return userService.getUserWithAuthoritiesByLogin(userLogin).orElseThrow(() -> new SecurityException("Current user not found"));
    }

    private void validatePortfolioFilter(String accountId, UUID customPortfolioId) {
        if (accountId != null && !accountId.isBlank() && customPortfolioId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use either accountId or customPortfolioId, not both");
        }
    }
}
