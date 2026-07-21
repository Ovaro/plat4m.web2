package ovaro.plat4m.service;

import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.domain.FinanceInvestmentActivityType;
import ovaro.plat4m.domain.FinanceInvestmentEvent;
import ovaro.plat4m.domain.FinanceSecurity;
import ovaro.plat4m.domain.FinanceSecurityInvestmentSummary;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.FinanceSecurityType;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.IFinanceSecurityPriceInPeriod;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceInvestmentEventRepository;
import ovaro.plat4m.repository.FinanceSecurityPriceRepository;
import ovaro.plat4m.repository.FinanceSecurityRepository;
import ovaro.plat4m.repository.FinanceUserSecurityRepository;
import ovaro.plat4m.repository.LockingFinanceUserSecurityRepository;
import ovaro.plat4m.service.dto.FinanceDateAnnotationDTO;
import ovaro.plat4m.service.dto.FinanceInvestmentPortfolioSummaryDTO;
import ovaro.plat4m.service.dto.FinanceInvestmentSnapshotDetails;
import ovaro.plat4m.service.dto.FinanceInvestmentTransactionDTO;
import ovaro.plat4m.service.dto.FinanceSnapshot;
import ovaro.plat4m.service.dto.FinanceSnapshotsPerResourceDTO;
import ovaro.plat4m.service.idto.MultipleResultsException;

@Service
public class FinanceSecurityService {

    private final Logger log = LoggerFactory.getLogger(FinanceSecurityService.class);

    private FinanceSecurityRepository securityRepository;
    private FinanceUserSecurityRepository userSecurityRepository;
    private LockingFinanceUserSecurityRepository lockingUserSecurityRepository;
    private FinanceSecurityPriceRepository securitySPRepository;
    private FinanceInvestmentEventRepository investmentEventRepository;
    private FinanceSecurityIntegrationService financeSecurityIntegrationService;
    private FinanceFXService fxService;
    private FinanceTransactionService financeTransactionService;

    public FinanceSecurityService(
        FinanceSecurityRepository securityRepository,
        FinanceSecurityPriceRepository securitySPRepository,
        FinanceSecurityIntegrationService financeSecurityIntegrationService,
        FinanceUserSecurityRepository userSecurityRepository,
        FinanceFXService fxService,
        FinanceInvestmentEventRepository investmentEventRepository,
        FinanceTransactionService financeTransactionService,
        LockingFinanceUserSecurityRepository lockingUserSecurityRepository
    ) {
        this.securityRepository = securityRepository;
        this.securitySPRepository = securitySPRepository;
        this.financeSecurityIntegrationService = financeSecurityIntegrationService;
        this.userSecurityRepository = userSecurityRepository;
        this.fxService = fxService;
        this.investmentEventRepository = investmentEventRepository;
        this.financeTransactionService = financeTransactionService;
        this.lockingUserSecurityRepository = lockingUserSecurityRepository;
    }

    public FinanceSecurityPrice getLatestBySymbol(String symbol) {
        StopWatch sw = new StopWatch();
        sw.start("getLatestBySymbol");
        FinanceSecurityPrice sp = this.securitySPRepository.findLatestBySymbol(symbol);
        sw.stop();
        log.info(sw.prettyPrint());
        return sp;
    }

    public List<FinanceSecurityPrice> getLatestBySecurityIds(String[] securityIds) {
        StopWatch sw = new StopWatch();
        sw.start("getLatestBySecurityIds");
        // FinanceSecurityPrice sp =
        // this.securitySPRepository.findLatestBySecurityId(securityId);
        List<UUID> uuids = new ArrayList<UUID>();
        for (String s : securityIds) {
            uuids.add(UUID.fromString(s));
        }
        Collections.singletonList(uuids);
        List<FinanceSecurityPrice> sp = this.securitySPRepository.findLatestBySecurityIdIn(uuids);
        sw.stop();
        log.info(sw.prettyPrint());
        return sp;
        // return sp;
    }

    public List<FinanceSecurityPrice> getLatestAll() {
        StopWatch sw = new StopWatch();
        sw.start("getLatestAll");
        List<FinanceSecurityPrice> sps = this.securitySPRepository.findLatestAll();
        sw.stop();
        log.info(sw.prettyPrint());
        return sps;
    }

    /**
     * Historical Investment Values for graphs typically.
     * @param user User.
     * @param start Start of period to return.
     * @param end End of period to return.
     * @param userSecurities
     * @param numberOfPeriods If > 0 then divide start/end by nuberOfPeriods, otherwise monthly (i.e. <= 0)
     * @param includePositionChangeEvents
     * @param includeClosed
     * @return Collection<FinanceSnapshotsPerResourceDTO>
     */
    public Collection<FinanceSnapshotsPerResourceDTO> historicalInvestmentValues(
        User user,
        LocalDate start,
        LocalDate end,
        List<FinanceUserSecurity> userSecurities,
        int numberOfPeriods,
        boolean includePositionChangeEvents,
        boolean includeClosed
    ) {
        // Get Events
        List<FinanceSnapshotsPerResourceDTO> results = new ArrayList<FinanceSnapshotsPerResourceDTO>();
        log.warn(
            "historicalInvestmentValues start. user={}, start={}, end={}, userSecurityCount={}, numberOfPeriods={}, includeClosed={}",
            user.getLogin(),
            start,
            end,
            userSecurities != null ? userSecurities.size() : null,
            numberOfPeriods,
            includeClosed
        );

        if (userSecurities == null || userSecurities.isEmpty()) {
            log.warn("historicalInvestmentValues exiting early because no user securities were supplied.");
            return results;
        }

        List<String> userSecurityIds = new ArrayList<String>(userSecurities.size());
        for (FinanceUserSecurity us : userSecurities) {
            userSecurityIds.add(us.getId().toString());
        }

        List<FinanceInvestmentEvent> allEvents = this.investmentEventRepository.findAllByUserSecurityIdsOrderByDate(
            userSecurityIds,
            end.plusDays(1)
        );
        log.warn(
            "historicalInvestmentValues loaded events. eventCount={}, firstSecurityId={}",
            allEvents != null ? allEvents.size() : null,
            userSecurityIds.isEmpty() ? null : userSecurityIds.get(0)
        );

        if (allEvents == null || allEvents.isEmpty()) {
            log.warn("historicalInvestmentValues exiting early because no investment events were found.");
            return results;
        }
        // Get period
        // If from date == null - work out the earliest event use it
        if (start == null && allEvents.size() > 0) {
            start = allEvents.get(0).getDate();
        } else if (start == null) {
            start = LocalDate.now().minusYears(1);
        }

        Period period = null;
        int days = 0;
        if (numberOfPeriods > 0) {
            long gap = ChronoUnit.DAYS.between(start, end);
            days = Math.max(1, (int) gap / numberOfPeriods);
            period = Period.ofDays(days);
            log.debug("Gap: " + gap + ", Period: " + period.toString() + ", Period Days: " + days);
        } else {
            // Null period and day=0 implies monthly
        }

        // Make user securities easily retrievable by ID
        Map<String, FinanceUserSecurity> usMap = mapUserSecurities(userSecurities);

        if (!includeClosed) {
            removeClosedEvents(user, allEvents, usMap);
        }

        // Partition by User Security / Filter out if not a user security being included.
        Map<String, List<FinanceInvestmentEvent>> allEventsByUserSecurity = partitionByUserSecurityAndFilter(user, allEvents, usMap);
        log.warn("historicalInvestmentValues partitioned events. groupedSecurityCount={}", allEventsByUserSecurity.size());

        // Generate annotation series
        Map<String, List<FinanceDateAnnotationDTO>> annotationsPerSecurity = generateAnnotationSeries(allEventsByUserSecurity, start, end);

        // Generate base series by period.
        // This uses the investment events (and the User Security map to provide additional contextual information), and places the known holding into period snapshots.
        // The price and FX rates are as per the last investment event NOT the correct price at those epochs.
        Map<String, List<FinanceSnapshot>> valuesPerSecurity = generateBaseSeriesByPeriod(
            user,
            allEventsByUserSecurity,
            usMap,
            start,
            end,
            period
        );

        // Update the period snapshots with the latest known security prices and FX values.
        updateWithLatestPrices(valuesPerSecurity, usMap, user.getLocalCurrency(), days);

        log.info(
            "Historical investment values prepared. userSecurities={}, events={}, groupedSeries={}",
            userSecurities.size(),
            allEvents.size(),
            valuesPerSecurity.size()
        );

        // Generate the DTO response.
        for (FinanceUserSecurity us : userSecurities) {
            String usId = us.getId().toString();
            List<FinanceSnapshot> snapshots = valuesPerSecurity.get(usId);
            if (snapshots == null || snapshots.isEmpty()) {
                log.info("Excluding history series for {} [{}] because no snapshots were generated.", us.getName(), usId);
                continue;
            }

            if (!hasRenderableSnapshots(snapshots)) {
                log.info(
                    "Excluding history series for {} [{}] because snapshots had no positive quantity or value. snapshotCount={}",
                    us.getName(),
                    usId,
                    snapshots.size()
                );
                continue;
            }

            FinanceSnapshotsPerResourceDTO dto = new FinanceSnapshotsPerResourceDTO();
            dto.setId(usId);
            dto.setName(us.getName());
            dto.setSymbol(us.getSymbol());
            dto.setCurrencyCode(us.getCurrencyCode());
            dto.setType("userSecurity");
            dto.setSnapshots(snapshots);
            dto.setAnnotations(annotationsPerSecurity.get(usId));
            results.add(dto);
            log.info(
                "Including history series for {} [{}]. snapshotCount={}, firstDate={}, lastDate={}",
                us.getName(),
                usId,
                snapshots.size(),
                snapshots.get(0).getDate(),
                snapshots.get(snapshots.size() - 1).getDate()
            );
        }

        log.warn("historicalInvestmentValues finished. resultCount={}", results.size());

        return results;
    }

    private boolean hasRenderableSnapshots(List<FinanceSnapshot> snapshots) {
        for (FinanceSnapshot snapshot : snapshots) {
            if (snapshot == null) {
                continue;
            }
            if (snapshot.getValue() != null && snapshot.getValue().doubleValue() > 0.0) {
                return true;
            }
            FinanceInvestmentSnapshotDetails inv = snapshot.checkAndGetInvestment();
            if (inv.getQuantity() != null && inv.getQuantity() > 0.0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Produce the series of investment event annotations (i.e. BUY/SELL events).
     */
    private Map<String, List<FinanceDateAnnotationDTO>> generateAnnotationSeries(
        Map<String, List<FinanceInvestmentEvent>> allEventsByUserSecurity,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        // Generate annotation series
        Map<String, List<FinanceDateAnnotationDTO>> annotationsPerSecurity = new HashMap<String, List<FinanceDateAnnotationDTO>>();
        for (String usId : allEventsByUserSecurity.keySet()) {
            for (FinanceInvestmentEvent fie : allEventsByUserSecurity.get(usId)) {
                List<FinanceDateAnnotationDTO> listAnnotations = annotationsPerSecurity.get(fie.getUserSecurityId());
                if (listAnnotations == null) {
                    listAnnotations = new ArrayList<FinanceDateAnnotationDTO>();
                    annotationsPerSecurity.put(fie.getUserSecurityId(), listAnnotations);
                }
                if (
                    fie.getQuantity() != null &&
                    (fie.getDate().isEqual(fromDate) || fie.getDate().isAfter(fromDate)) &&
                    (fie.getDate().isBefore(toDate) || fie.getDate().isEqual(toDate))
                ) {
                    FinanceDateAnnotationDTO newAnnotation = new FinanceDateAnnotationDTO(fie.getDate());
                    if (fie.getQuantity() > 0) {
                        if (fie.getIncome() != null && fie.getIncome() != null && fie.getIncome().doubleValue() > 0) {
                            newAnnotation.setAnnotation("Reinvest Divdend");
                            newAnnotation.setType("Reinvest");
                        } else {
                            newAnnotation.setAnnotation("Buy " + fie.getQuantity());
                            newAnnotation.setType("BUY");
                        }
                    } else if (fie.getQuantity() < 0) {
                        newAnnotation.setAnnotation("Sell");
                        newAnnotation.setType("SELL");
                    } else if (fie.getQuantity() < 0) {
                        newAnnotation.setAnnotation("Buy " + fie.getQuantity());
                        newAnnotation.setType("BUY");
                    }
                    listAnnotations.add(newAnnotation);
                }
            }
        }

        return annotationsPerSecurity;
    }

    /*
     * Generate base series by period.
     * This uses the investment events (and the User Security map to provide additional contextual information), and places the known holding into period snapshots.
     * The price and FX rates are as per the last investment event NOT the correct price at those epochs.
     */
    private Map<String, List<FinanceSnapshot>> generateBaseSeriesByPeriod(
        User user,
        Map<String, List<FinanceInvestmentEvent>> allEventsByUserSecurity,
        Map<String, FinanceUserSecurity> usMap,
        LocalDate fromDate,
        LocalDate toDate,
        Period period
    ) {
        Map<String, List<FinanceSnapshot>> valuesPerSecurity = new HashMap<String, List<FinanceSnapshot>>();
        for (String usId : allEventsByUserSecurity.keySet()) {
            FinanceUserSecurity fus = usMap.get(usId);
            log.info(
                "UserSecurity: " +
                    fus.getName() +
                    "[" +
                    usId +
                    "] being processed. Between: " +
                    fromDate +
                    " - " +
                    toDate +
                    ". Periods: " +
                    period
            );
            LocalDate currentPeriodEnd = fromDate.with(TemporalAdjusters.lastDayOfMonth());
            LocalDate toDateEndMonth = toDate.with(TemporalAdjusters.lastDayOfMonth());
            Double holdingBalance = Double.valueOf(0);
            BigDecimal lastPrice = BigDecimal.ZERO;
            Double lastFX = null; // We don't store the FX at that date
            LocalDate lastPriceDate = null;
            LocalDate lastFXDate = null;
            for (FinanceInvestmentEvent fie : allEventsByUserSecurity.get(usId)) {
                log.info(
                    "FIE: " +
                        fus.getName() +
                        " (" +
                        fie.getDate() +
                        ") [" +
                        fie.getHolding() +
                        " $" +
                        fie.getPrice() +
                        "] - periodEnd: " +
                        currentPeriodEnd
                );

                if (fie.getCapitalDelta() == null || fie.getHolding() == null) {
                    // Income only. Skip
                } else {
                    if (fie.getDate().isAfter(toDateEndMonth)) {
                        log.debug("[After toDate so skipping]");
                    } else if (fie.getDate().isBefore(fromDate)) {
                        log.debug("[Before the fromDate so skipping]");
                        // Skip - but save current values
                        holdingBalance = fie.getHolding();
                        if (fie.getPrice() != null) {
                            lastPrice = fie.getPrice();
                            lastPriceDate = fie.getDate();
                            lastFX = fie.getRateToBase();
                            lastFXDate = fie.getDate();
                        }
                    } else {
                        // We are in a future period. Roll foward until we find it
                        while (fie.getDate().isAfter(currentPeriodEnd)) {
                            log.info(
                                fie.getDate() +
                                    " is after or equal to the end of the period: " +
                                    currentPeriodEnd +
                                    " so storing current holding & last price and rolling forward to next period. "
                            );
                            // STORE [1]
                            this.storeSnapshotValue(
                                valuesPerSecurity,
                                usMap.get(usId),
                                currentPeriodEnd,
                                holdingBalance,
                                lastPrice,
                                true,
                                lastPriceDate,
                                lastFX,
                                lastFXDate,
                                user.getLocalCurrency()
                            );
                            currentPeriodEnd = addPeriodToDate(currentPeriodEnd, period);
                        }

                        if (
                            fie.getDate().isEqual(currentPeriodEnd) ||
                            (fie.getDate().isBefore(currentPeriodEnd) &&
                                (currentPeriodEnd.isBefore(toDateEndMonth) || currentPeriodEnd.isEqual(toDateEndMonth)))
                        ) {
                            // We are inside the period, just save it and move to the next FIE
                            log.info(
                                fie.getDate() +
                                    " is before the end of the period: " +
                                    currentPeriodEnd +
                                    " so storing investment event on the date: " +
                                    fie.getDate()
                            );

                            holdingBalance = fie.getHolding();

                            if (fie.getPrice() != null) {
                                lastPrice = fie.getPrice();
                                lastPriceDate = fie.getDate();
                                lastFX = fie.getRateToBase();
                                lastFXDate = fie.getDate();
                            }
                            this.storeSnapshotValue(
                                valuesPerSecurity,
                                usMap.get(usId),
                                fie.getDate(),
                                holdingBalance,
                                lastPrice,
                                fie.getPrice() == null,
                                lastPriceDate,
                                lastFX,
                                lastFXDate,
                                user.getLocalCurrency()
                            );
                        }
                    }
                }
            }

            // Save unclosed duration
            while (currentPeriodEnd.isBefore(toDateEndMonth) || currentPeriodEnd.isEqual(toDateEndMonth)) {
                log.info(
                    "Closing period: " +
                        currentPeriodEnd +
                        " for: " +
                        fus.getName() +
                        " (" +
                        (fus.getSecurity() != null ? fus.getSecurity().getSymbol() : fus.getSymbol()) +
                        "). Holding: " +
                        holdingBalance +
                        ", lastPrice: " +
                        lastPrice
                );
                // STORE [3]
                this.storeSnapshotValue(
                    valuesPerSecurity,
                    usMap.get(usId),
                    currentPeriodEnd,
                    holdingBalance,
                    lastPrice,
                    true,
                    lastPriceDate,
                    lastFX,
                    lastFXDate,
                    user.getLocalCurrency()
                );

                currentPeriodEnd = addPeriodToDate(currentPeriodEnd, period);
            }
        }
        return valuesPerSecurity;
    }

    private LocalDate addPeriodToDate(LocalDate date, Period period) {
        if (period != null) {
            return date.plus(period);
        } else {
            date = date.plus(Period.ofMonths(1));
            date = date.withDayOfMonth(date.lengthOfMonth());
            return date;
        }
    }

    /**
     * Update the period snapshots with the latest known security prices and FX values.
     */
    private void updateWithLatestPrices(
        Map<String, List<FinanceSnapshot>> valuesPerSecurity,
        Map<String, FinanceUserSecurity> usMap,
        String baseCurrency,
        int stdPeriodDays
    ) {
        // Processing needs to be by period

        List<String> symbols = new ArrayList<String>();
        List<String> currencies = new ArrayList<String>();
        List<LocalDate> dates = new ArrayList<LocalDate>();

        // Index the securities, by known symbol, into the periods found in the snapshots.
        Map<LocalDate, Map<String, FinanceSnapshot>> indexedPeriodsAndSecurities = indexKnownSymbolsAndSnapshotsByDate(
            valuesPerSecurity,
            usMap,
            baseCurrency,
            symbols,
            dates,
            currencies
        );

        // Index FX rates for the currencies represented
        Map<LocalDate, Map<String, FinanceFX>> fxMap = getClosestFXRatesForPeriods(baseCurrency, currencies, dates, stdPeriodDays);

        // Update Security Prices
        // TODO optimise to one query rather than multiple
        LocalDate fromDate = null;
        for (LocalDate d : dates) {
            //log.info("Processing period : " + d);
            if (fromDate == null) {
                fromDate = d;
            }

            List<IFinanceSecurityPriceInPeriod> prices = securitySPRepository.findLatestInPeriod(symbols, fromDate, d);
            if (prices == null || prices.size() == 0) {
                prices = securitySPRepository.findLatestInPeriod(symbols, fromDate.minusYears(5), fromDate);
            }
            if (prices == null || prices.isEmpty()) {
                continue;
            }
            // Update the prices
            Map<String, FinanceSnapshot> securitiesToSnapshot = indexedPeriodsAndSecurities.get(d);
            if (securitiesToSnapshot == null || securitiesToSnapshot.isEmpty()) {
                continue;
            }
            for (IFinanceSecurityPriceInPeriod price : prices) {
                LocalDate priceDate = price.getDate() != null ? price.getDate().toLocalDate() : null;
                log.info("Processing price : " + price.getSymbol() + " [" + priceDate + " $" + price.getPrice() + "] for period: " + d);
                FinanceSnapshot snapshot = securitiesToSnapshot.get(price.getSymbol());
                if (snapshot == null) {
                    log.info("Cannot find snapshot for : " + price.getSymbol() + " for period: " + d);
                    continue;
                }

                FinanceInvestmentSnapshotDetails inv = snapshot.checkAndGetInvestment();
                if (inv.getPriceDate() == null) {
                    log.warn("Snapshot date for : " + price.getSymbol() + " for period: " + d + " is null");
                } else if (priceDate != null && !inv.getPriceDate().isAfter(priceDate)) {
                    snapshot.setId(price.getSymbol());
                    if (snapshot.getCurrencyIsoCode() == null || snapshot.getCurrencyIsoCode().equals(baseCurrency)) {
                        //log.info("Updating snapshot for " + price.getSymbol() + " in period: " + d + " - FROM: " + inv.getPriceDate() + " " +  snapshot.getValue() +"($" + inv.getPrice() + ") to  " + price.getPrice().multiply(BigDecimal.valueOf(inv.getQuantity())) + " ($" + price.getPrice() + ") - Price Date: " + price.getDate());
                        updateSnapshotValue(snapshot, price.getPrice(), priceDate, null, null, baseCurrency);
                    } else {
                        // Need Currency Conversion update as well.
                        log.info(
                            "Updating (foreign FX snapshot) for " +
                                price.getSymbol() +
                                " in period: " +
                                d +
                                " - FROM: " +
                                inv.getPriceDate() +
                                " " +
                                snapshot.getValue() +
                                "($" +
                                inv.getPrice() +
                                ") to  " +
                                price.getPrice().multiply(BigDecimal.valueOf(inv.getQuantity())) +
                                " ($" +
                                price.getPrice() +
                                ") - Price Date: " +
                                priceDate
                        );
                        Map<String, FinanceFX> curFXMap = fxMap.get(d);
                        if (curFXMap != null) {
                            FinanceFX fx = curFXMap.get(snapshot.getCurrencyIsoCode());
                            if (fx == null) {
                                log.info(
                                    "Updating snapshot without FX because no FX match was found for currency {} on period {}.",
                                    snapshot.getCurrencyIsoCode(),
                                    d
                                );
                                updateSnapshotValue(snapshot, price.getPrice(), priceDate, null, null, baseCurrency);
                            } else if (snapshot.getFxDate() == null || fx.getDate().toLocalDate().isAfter(snapshot.getFxDate())) {
                                // Lets use it because it looks to be more recent
                                Double rate = fx.getRate();
                                if (fx.getFromIsoCode().equals(baseCurrency)) {
                                    rate = 1 / fx.getRate();
                                }
                                log.info("Updating snapshot using new FX values: " + fx.getDate().toLocalDate() + ", rate: " + rate);
                                updateSnapshotValue(snapshot, price.getPrice(), priceDate, rate, fx.getDate().toLocalDate(), baseCurrency);
                            } else {
                                log.info("Updating snapshot but using old FX value because we don't have a more recent one.");
                                updateSnapshotValue(snapshot, price.getPrice(), priceDate, null, null, baseCurrency);
                            }
                        } else {
                            log.info("Updating snapshot without FX since not registered.");
                            updateSnapshotValue(snapshot, price.getPrice(), priceDate, null, null, baseCurrency);
                        }
                    }
                    // snapshot.setPriceDate(price.getDate());
                    // snapshot.setValue(price.getPrice().multiply(BigDecimal.valueOf(snapshot.getQuantity())));
                } else {
                    log.info(
                        "Not updating snapshot for : " +
                            price.getSymbol() +
                            " for period: " +
                            d +
                            " since it is after price date: " +
                            priceDate +
                            " (compared to: " +
                            inv.getPriceDate() +
                            "). Value: " +
                            snapshot.getValue()
                    );
                }
            }
        }
    }

    private Map<LocalDate, Map<String, FinanceFX>> getClosestFXRatesForPeriods(
        String baseCurrency,
        List<String> currencies,
        List<LocalDate> dates,
        int days
    ) {
        Map<LocalDate, Map<String, FinanceFX>> result = new HashMap<LocalDate, Map<String, FinanceFX>>();
        if (dates == null || dates.size() == 0) {
            return result;
        }
        LocalDate fromDate = dates.get(0);
        fromDate = fromDate.minusYears(1);
        LocalDate toDate = dates.get(dates.size() - 1);
        if (days < 1) {
            days = 1;
        }

        for (String currency : currencies) {
            log.debug("[" + currency + "] being loaded.");
            List<FinanceFX> fxs = null;
            fxs = fxService.findFXSummaries(baseCurrency, currency, fromDate, toDate, days);

            // Expects order by date
            for (LocalDate periodDate : dates) {
                // Now walk the FXs to find nearest dates and index accordingly
                log.debug("[" + currency + "] [" + periodDate + "]");
                FinanceFX lastFX = null;
                for (FinanceFX fx : fxs) {
                    LocalDate fxDate = fx.getDate().toLocalDate();
                    log.debug("[" + currency + "] [" + periodDate + "] Checking FXDate: " + fxDate);
                    if (fxDate.isBefore(periodDate)) {
                        // Roll Forward
                        lastFX = fx;
                        log.debug("[" + currency + "] [" + periodDate + "]  FXDate: " + fxDate + " is before period date. Skipping.");
                    } else if (fxDate.isEqual(periodDate)) {
                        log.debug("[" + currency + "] [" + periodDate + "]  FXDate: " + fxDate + " is equal period date. Storing.");
                        store(result, periodDate, currency, fx);
                        lastFX = fx;
                        break;
                    } else {
                        // The FX date is now after the period date.
                        if (lastFX == null) {
                            // Skip, but this isn't good since no value for period is available
                            log.warn("Couldnt find FX value for: " + baseCurrency + " to " + currency + " for period date: " + periodDate);
                        } else {
                            log.debug(
                                "[" +
                                    currency +
                                    "] [" +
                                    periodDate +
                                    "]  LastFX: " +
                                    lastFX.getDate() +
                                    " was the last one before the end of the period. Storing."
                            );
                            store(result, periodDate, currency, lastFX);
                            lastFX = fx;
                            break;
                        }
                    }
                }

                if (lastFX != null && lastFX.getDate().toLocalDate().isBefore(periodDate)) {
                    // Unclosed period
                    log.debug("[" + currency + "] [" + periodDate + "] Closing period with LastFXDate: " + lastFX.getDate() + ". Storing.");
                    store(result, periodDate, currency, lastFX);
                }
            }
        }

        return result;
    }

    private void store(Map<LocalDate, Map<String, FinanceFX>> result, LocalDate periodDate, String currency, FinanceFX fx) {
        Map<String, FinanceFX> map = result.get(periodDate);
        if (map == null) {
            map = new HashMap<String, FinanceFX>();
            result.put(periodDate, map);
        }
        map.put(currency, fx);
        log.info("[" + currency + "] [" + periodDate + "] - Storing FX: " + fx.getDate().toLocalDate() + "");
    }

    /**
     * Index the securities, by known symbol, into the periods (ordered by date) found in the snapshots.
     */
    private Map<LocalDate, Map<String, FinanceSnapshot>> indexKnownSymbolsAndSnapshotsByDate(
        Map<String, List<FinanceSnapshot>> valuesPerSecurity,
        Map<String, FinanceUserSecurity> usMap,
        String baseCurrency,
        List<String> symbols,
        List<LocalDate> dates,
        List<String> currencies
    ) {
        Map<LocalDate, Map<String, FinanceSnapshot>> indexedPeriodsAndSecurities = new HashMap<LocalDate, Map<String, FinanceSnapshot>>();
        Map<String, String> currenciesMap = new HashMap<String, String>();
        for (String usId : valuesPerSecurity.keySet()) {
            FinanceUserSecurity fus = usMap.get(usId);
            if (fus == null) {
                continue;
            }
            if (fus.getSecurity() != null || fus.getSymbol() != null) {
                String symbol = fus.getSymbol();
                if (fus.getSecurity() != null) {
                    symbol = fus.getSecurity().getSymbol();
                }
                if (symbol == null || symbol.isBlank()) {
                    log.info("(No symbol for : " + fus.getName() + " so not processing)");
                    continue;
                }
                if (fus.getCurrencyCode() != null && !fus.getCurrencyCode().equals(baseCurrency)) {
                    currenciesMap.put(fus.getCurrencyCode(), fus.getCurrencyCode());
                }
                log.debug("Processing symbol: " + symbol + " [" + usId + "]");

                symbols.add(symbol);

                for (FinanceSnapshot snapshot : valuesPerSecurity.get(usId)) {
                    // Check if in array
                    if (!dateAlreadyInArray(dates, snapshot.getDate())) {
                        dates.add(snapshot.getDate());
                    } else {
                        // log.info("(period : " + snapshot.getDate() + " [" + fus.getSymbol() + "] was already in dates array.)");
                    }

                    // Index the security by period, and security to snapshot
                    Map<String, FinanceSnapshot> securityToSnapshot = indexedPeriodsAndSecurities.get(snapshot.getDate());
                    if (securityToSnapshot == null) {
                        securityToSnapshot = new HashMap<String, FinanceSnapshot>();
                        indexedPeriodsAndSecurities.put(snapshot.getDate(), securityToSnapshot);
                        log.info("Storing snapshot in *new* period : " + snapshot.getDate() + " [" + usId + "]");
                    } else {
                        log.info("Storing snapshot in existing period : " + snapshot.getDate() + " [" + usId + "]");
                    }
                    if (fus.getSecurity() != null) {
                        securityToSnapshot.put(fus.getSecurity().getSymbol(), snapshot);
                    } else {
                        securityToSnapshot.put(fus.getSymbol(), snapshot);
                    }
                }
            } else {
                log.info("(No symbol for : " + fus.getName() + " so not processing)");
            }
        }

        dates.sort(
            new Comparator<LocalDate>() {
                @Override
                public int compare(LocalDate d1, LocalDate d2) {
                    return d1.compareTo(d2);
                }
            }
        );

        // Fix currencies for return
        currencies.addAll(currenciesMap.keySet());

        return indexedPeriodsAndSecurities;
    }

    private boolean dateAlreadyInArray(List<LocalDate> dates, LocalDate date) {
        boolean result = false;

        for (LocalDate arrayDate : dates) {
            if (arrayDate.equals(date)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private void storeSnapshotValue(
        Map<String, List<FinanceSnapshot>> valuesPerSecurity,
        FinanceUserSecurity security,
        LocalDate date,
        Double holding,
        BigDecimal price,
        boolean estimatedPrice,
        LocalDate priceDate,
        Double rateToBase,
        LocalDate fxDate,
        String baseCurrency
    ) {
        String securityId = security.getId().toString();
        List<FinanceSnapshot> listValues = valuesPerSecurity.get(securityId);
        if (listValues == null) {
            listValues = new ArrayList<FinanceSnapshot>();
            valuesPerSecurity.put(securityId, listValues);
        }
        if (holding != null) {
            if (price != null && holding != null) {
                FinanceSnapshot newSnapshot = findSnapshotByDate(listValues, date);
                if (newSnapshot == null) {
                    newSnapshot = new FinanceSnapshot(date);
                    listValues.add(newSnapshot);
                }
                updateSnapshotValue(
                    newSnapshot,
                    holding,
                    price,
                    estimatedPrice,
                    priceDate,
                    security.getCurrencyCode(),
                    rateToBase,
                    fxDate,
                    baseCurrency
                );
            }
        }
    }

    private FinanceSnapshot findSnapshotByDate(List<FinanceSnapshot> snapshots, LocalDate date) {
        for (FinanceSnapshot snapshot : snapshots) {
            if (snapshot != null && snapshot.getDate() != null && snapshot.getDate().equals(date)) {
                return snapshot;
            }
        }
        return null;
    }

    private void updateSnapshotValue(
        FinanceSnapshot snapshot,
        Double holding,
        BigDecimal price,
        boolean estimatedPrice,
        LocalDate priceDate,
        String currencyIsoCode,
        Double rateToBase,
        LocalDate fxDate,
        String baseCurrency
    ) {
        FinanceInvestmentSnapshotDetails inv = snapshot.checkAndGetInvestment();
        inv.setQuantity(holding);
        inv.setEstimatedPrice(estimatedPrice);
        snapshot.setCurrencyIsoCode(currencyIsoCode);
        updateSnapshotValue(snapshot, price, priceDate, rateToBase, fxDate, baseCurrency);
    }

    private void updateSnapshotValue(
        FinanceSnapshot snapshot,
        BigDecimal price,
        LocalDate priceDate,
        Double rateToBase,
        LocalDate fxDate,
        String baseCurrency
    ) {
        FinanceInvestmentSnapshotDetails inv = snapshot.checkAndGetInvestment();
        log.info(
            "Updating " +
                snapshot.getId() +
                ". Price: " +
                price +
                ", PriceDate: " +
                priceDate +
                ", New Value: " +
                price.multiply(BigDecimal.valueOf(inv.getQuantity()))
        );
        inv.setPriceDate(priceDate);
        inv.setPrice(price);
        if (rateToBase != null) {
            snapshot.setFxToLocal(rateToBase);
            snapshot.setFxDate(fxDate);
        }

        BigDecimal value = price.multiply(BigDecimal.valueOf(inv.getQuantity()));
        if (snapshot.getCurrencyIsoCode() != null && !snapshot.getCurrencyIsoCode().equals(baseCurrency)) {
            Double effectiveRateToBase = rateToBase != null && rateToBase > 0.0 ? rateToBase : snapshot.getFxToLocal();
            if (effectiveRateToBase != null && effectiveRateToBase > 0.0) {
                value = value.multiply(BigDecimal.valueOf(effectiveRateToBase));
            } else {
                log.warn(
                    "No FX rate available for historical snapshot. currency={}, baseCurrency={}, date={}, price={}, quantity={}",
                    snapshot.getCurrencyIsoCode(),
                    baseCurrency,
                    snapshot.getDate(),
                    price,
                    inv.getQuantity()
                );
            }
        }
        snapshot.setValue(value);
    }

    public Map<String, FinanceUserSecurity> mapUserSecurities(List<FinanceUserSecurity> includedUserSecurities) {
        Map<String, FinanceUserSecurity> usMap = new HashMap<String, FinanceUserSecurity>();
        for (FinanceUserSecurity fus : includedUserSecurities) {
            usMap.put(fus.getId().toString(), fus);
        }
        return usMap;
    }

    public void removeClosedEvents(
        User user,
        List<FinanceInvestmentEvent> allEvents,
        Map<String, FinanceUserSecurity> includeUserSecuritiesMap
    ) {
        // Map<String, List<FinanceInvestmentEvent>> allEventsByUserSecurity = new HashMap<String, List<FinanceInvestmentEvent>>();
        // for (FinanceInvestmentEvent fie : allEvents) {
        //     if (includeUserSecuritiesMap.get(fie.getUserSecurityId()) != null) {
        //         List<FinanceInvestmentEvent> listEvents = allEventsByUserSecurity.get(fie.getUserSecurityId());
        //         if (listEvents == null) {
        //             listEvents = new ArrayList<FinanceInvestmentEvent>();
        //             allEventsByUserSecurity.put(fie.getUserSecurityId(), listEvents);
        //         }
        //         listEvents.add(fie);
        //     }
        // }
        List<FinanceInvestmentEvent> toRemove = new ArrayList<FinanceInvestmentEvent>();
        Map<String, String> usToRemove = new HashMap<String, String>();
        for (int i = allEvents.size() - 1; i >= 0; i--) {
            FinanceInvestmentEvent event = allEvents.get(i);
            boolean usInTheRemoveMap = usToRemove.get(event.getUserSecurityId()) != null ? true : false;
            log.info("Event.getHolding:  " + event.getHolding() + " - usID: " + event.getUserSecurityId());
            if (event.getHolding() != null && event.getHolding() == 0.0) {
                // Should be removed plus all others with same user security ID
                toRemove.add(event);
                usToRemove.put(event.getUserSecurityId(), event.getUserSecurityId());
            } else if (usInTheRemoveMap) {
                toRemove.add(event);
            }
        }

        allEvents.removeAll(toRemove);
    }

    public Map<String, List<FinanceInvestmentEvent>> partitionByUserSecurityAndFilter(
        User user,
        List<FinanceInvestmentEvent> allEvents,
        Map<String, FinanceUserSecurity> includeUserSecuritiesMap
    ) {
        Map<String, List<FinanceInvestmentEvent>> allEventsByUserSecurity = new HashMap<String, List<FinanceInvestmentEvent>>();
        for (FinanceInvestmentEvent fie : allEvents) {
            if (includeUserSecuritiesMap.get(fie.getUserSecurityId()) != null) {
                List<FinanceInvestmentEvent> listEvents = allEventsByUserSecurity.get(fie.getUserSecurityId());
                if (listEvents == null) {
                    listEvents = new ArrayList<FinanceInvestmentEvent>();
                    allEventsByUserSecurity.put(fie.getUserSecurityId(), listEvents);
                }
                listEvents.add(fie);
            }
        }
        return allEventsByUserSecurity;
    }

    public List<LocalDate> determineSnapshotDates(LocalDate fromDate, LocalDate toDate, int numberOfPeriods) {
        List<LocalDate> dates = new ArrayList<LocalDate>(numberOfPeriods);
        dates.add(fromDate);
        long days = ChronoUnit.DAYS.between(fromDate, toDate);
        long daysInPeriod = days / numberOfPeriods;
        for (int i = 1; i < numberOfPeriods - 1; i++) {
            dates.add(fromDate.plusDays(daysInPeriod));
        }
        dates.add(toDate);
        return dates;
    }

    public Map<LocalDate, Map<String, HoldingSnapshot>> historicalInvestmentValues(
        User user,
        List<FinanceInvestmentEvent> allEvents,
        List<LocalDate> snapshotDates
    ) {
        // Determine number of days between snapshot periods
        Map<LocalDate, Map<String, HoldingSnapshot>> result = new HashMap<LocalDate, Map<String, HoldingSnapshot>>();

        Map<String, HoldingSnapshot> openSnapshot = new HashMap<String, HoldingSnapshot>();
        // Tally
        int datePos = 0;

        for (FinanceInvestmentEvent fie : allEvents) {
            LocalDate date = snapshotDates.get(datePos);
            if (fie.getDate().isAfter(date)) {
                Map<String, HoldingSnapshot> closedSnapshot = finaliseSnapshot(openSnapshot);
                result.put(date, closedSnapshot);
                datePos++;
                date = snapshotDates.get(datePos);
            }

            HoldingSnapshot currentNonFinalSnapshot = openSnapshot.get(fie.getUserSecurityId());
            if (currentNonFinalSnapshot == null) {
                currentNonFinalSnapshot = new HoldingSnapshot();
            }

            currentNonFinalSnapshot.quantity = currentNonFinalSnapshot.quantity + fie.getQuantity();
        }

        return result;
    }

    private Map<String, HoldingSnapshot> finaliseSnapshot(Map<String, HoldingSnapshot> openSnapshot) {
        Map<String, HoldingSnapshot> closedSnapshot = new HashMap<String, HoldingSnapshot>();
        for (String id : openSnapshot.keySet()) {
            HoldingSnapshot snap = openSnapshot.get(id);
            // Strip out zero or negative holdings
            if (snap.quantity > 0) {
                HoldingSnapshot clone = SerializationUtils.clone(snap);
                closedSnapshot.put(id, clone);
            }
        }

        return closedSnapshot;
    }

    public FinanceSecurity handleUserSecurity(
        User user,
        FinanceUserSecurity userSecurity,
        String currencyCode,
        boolean fetchSecurityInformation
    ) {
        if (
            userSecurity == null ||
            (userSecurity.getType() != FinanceSecurityType.STOCK.value() && userSecurity.getType() != FinanceSecurityType.ETF.value()) ||
            userSecurity.getSymbol() == null
        ) {
            return null;
        }
        FinanceSecurity security = null;
        String symbol = userSecurity.getSymbol();
        Optional<FinanceSecurity> os = securityRepository.findBySymbol(symbol);
        if (os.isPresent()) {
            return os.get();
        }

        if (symbol != null && symbol.charAt(2) == ':') {
            // strip off coutry prefix
            symbol = userSecurity.getSymbol().substring(3);
            os = securityRepository.findBySymbol(symbol);
        } else if (symbol != null && symbol.contains(".")) {
            // strip off suffix
            symbol = userSecurity.getSymbol().substring(0, userSecurity.getSymbol().indexOf("."));
            os = securityRepository.findBySymbol(symbol);
        }

        if (os.isPresent()) {
            return os.get();
        }

        if (fetchSecurityInformation) {
            try {
                security = this.financeSecurityIntegrationService.fetchSecurityInformation(symbol, user.getLocalCurrency(), currencyCode);
                if (security != null) {
                    // Fix Currency

                    security = securityRepository.save(security);
                    return security;
                }
            } catch (MultipleResultsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return null;
    }

    @Transactional
    public void identifyAndSaveInvestmentEvents(User user) {
        List<FinanceInvestmentTransactionDTO> dtos = this.financeTransactionService.findByUser(user);
        // Split into security ids
        Map<String, List<FinanceInvestmentTransactionDTO>> split = new HashMap<String, List<FinanceInvestmentTransactionDTO>>();
        for (FinanceInvestmentTransactionDTO fit : dtos) {
            if (fit.getTransaction().getInvestmentActivityType() != FinanceInvestmentActivityType.INVALID) {
                List<FinanceInvestmentTransactionDTO> l = split.get(fit.getTransaction().getSecurityId());
                if (l == null) {
                    l = new ArrayList<FinanceInvestmentTransactionDTO>();
                    split.put(fit.getTransaction().getSecurityId(), l);
                }
                l.add(fit);
            }
        }

        // Map<String, FinanceInvestmentTransactionsAndSummaryDTO> result = new
        // HashMap<String, FinanceInvestmentTransactionsAndSummaryDTO>();
        for (String securityId : split.keySet()) {
            this.identifyAndSaveInvestmentEvents(user, securityId, split.get(securityId), true);
        }
    }

    @Transactional
    public void identifyAndSaveInvestmentEvents(User user, String userSecurityId) {
        List<FinanceInvestmentTransactionDTO> dtos = this.financeTransactionService.findBySecurityId(user, userSecurityId);
        this.identifyAndSaveInvestmentEvents(user, userSecurityId, dtos, true);
    }

    @Transactional
    public void identifyAndSaveInvestmentEvents(
        User user,
        String userSecurityId,
        List<FinanceInvestmentTransactionDTO> dtos,
        boolean includeClosed
    ) {
        // Check and lock the userSecurity to stop parrallel activities occuring (multi-threading)
        Optional<FinanceUserSecurity> osec = this.lockingUserSecurityRepository.findById(UUID.fromString(userSecurityId));
        FinanceUserSecurity usec = osec.get();
        if (usec.getUserGuid() == null || !usec.getUserGuid().equals(user.getGuid().toString())) {
            log.warn(
                "Realigning FinanceUserSecurity ownership during event generation. userSecurityId={}, existingUserGuid={}, currentUserGuid={}",
                userSecurityId,
                usec.getUserGuid(),
                user.getGuid()
            );
            usec.setUserGuid(user.getGuid().toString());
        }
        //Optional<FinanceUserSecurity> osec = this.getUserSecurity(user, userSecurityId);
        // if(osec.isPresent() && !osec.get().isEventsValid()) {
        //     osec.get().setEventsValid(true);
        //     this.save(user, osec.get());
        //     log.info("Generating events for: " + userSecurityId);
        // } else {
        //     // TODO: Enhance this to have a number generated.
        //     log.warn("Events were already generated for this security. Not generating: " + osec.get().getId().toString());
        //     return;
        // }

        double quantity = 0;
        BigDecimal totalCapital = BigDecimal.ZERO;
        totalCapital.setScale(9, RoundingMode.HALF_UP);
        BigDecimal totalBaseCapital = BigDecimal.ZERO;
        totalBaseCapital.setScale(9, RoundingMode.HALF_UP);

        // Could check to make faster
        Optional<FinanceUserSecurity> ousec = userSecurityRepository.findById(UUID.fromString(userSecurityId));
        String acctCurrency = user.getLocalCurrency();
        if (ousec.isPresent() && ousec.get().getCurrencyCode() != null) {
            acctCurrency = ousec.get().getCurrencyCode();
        }

        List<CapitalGainEvent> cges = new ArrayList<CapitalGainEvent>();
        List<TransactionTrancheItem> openTransactions = new ArrayList<TransactionTrancheItem>();

        // Remove old ones
        List<FinanceInvestmentEvent> old = investmentEventRepository.findAllByUserSecurityId(userSecurityId);
        investmentEventRepository.deleteAllInBatch(old);

        List<FinanceInvestmentEvent> events = new ArrayList<FinanceInvestmentEvent>();
        MathContext MC = FinanceInvestmentEvent.MC;

        for (int i = dtos.size() - 1; i >= 0; i--) {
            FinanceInvestmentTransactionDTO txn = dtos.get(i);

            // if(quantity == 0) {
            // positionOpenDate = txn.getDate();
            // }

            FinanceInvestmentEvent evt = null;

            BigDecimal shareComponent = BigDecimal.ZERO;
            if (txn.getQuantity() != null && txn.getPrice() != null) {
                shareComponent = new BigDecimal(txn.getPrice() * txn.getQuantity(), MC);
            }

            if (
                txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.BUY ||
                txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.REINVEST_DIVIDEND_COMBINED ||
                txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.ADD_SHARES
            ) {
                // evt = new FinanceInvestmentEvent();
                // evt.setUserSecurityId(userSecurityId);
                // evt.setCurrencyCode(txn.getTransaction().getCurrencyCode());
                // evt.setUserGuid(user.getGuid().toString());
                // evt.setDate(txn.getDate());
                evt = createFIE(user, userSecurityId, txn);
                evt.setPrice(new BigDecimal(txn.getPrice(), MC));

                if (txn.getTransaction().getInvestmentActivityType() != FinanceInvestmentActivityType.ADD_SHARES) {
                    if (txn.getTransaction().getCurrencyCode() != null && !txn.getTransaction().getCurrencyCode().equals(acctCurrency)) {
                        // Different txn currency than account currency
                        // Share prices could be user currency rather than the amount which seems to be
                        // in the account currency, so if you subtract and these are different then it
                        // will be wrong.
                        // So this just assumes no fee and uses the amounts which is safer.
                        evt.setFee(0);

                        totalCapital = totalCapital.add(txn.getAmount());
                        evt.setCapitalDelta(txn.getAmount().round(MC));
                        evt.setBaseCurrencyCapitalDelta(evt.getCapitalDelta()); // For multi currency FIEs to be consistent
                    } else {
                        // Should be in the account currency
                        evt.setFee(txn.getAmount().abs().subtract(shareComponent));

                        totalCapital = totalCapital.add(shareComponent);
                        evt.setCapitalDelta(shareComponent);

                        if (
                            txn.getTransaction().getAmountBase() != null &&
                            txn.getTransaction().getAmountBase().compareTo(BigDecimal.ZERO) != 0 &&
                            txn.getTransaction().getAmountBase().compareTo(txn.getAmount()) != 0
                        ) {
                            // baseAmount is different. Lets record for currency differnce calcs
                            // double feeInBase = evt.getFee().doubleValue() *
                            // txn.getTransaction().getRateToBase();

                            // BigDecimal localFee = new BigDecimal(txn.getTransaction().getRateToBase() *
                            // evt.getFee().doubleValue());
                            // BigDecimal baseCurrencyCaptial =
                            // txn.getTransaction().getAmountBase().subtract(localFee); // .subtract(new
                            // BigDecimal(feeInBase))
                            if (txn.getTransaction().getRateToBase() != null) {
                                BigDecimal baseCurrencyCaptial2 = shareComponent.multiply(
                                    new BigDecimal(txn.getTransaction().getRateToBase(), MC)
                                );
                                totalBaseCapital = totalBaseCapital.add(baseCurrencyCaptial2);
                                BigDecimal fx = FinanceInvestmentEvent.setScale(new BigDecimal(txn.getTransaction().getRateToBase(), MC));
                                evt.setRateToBase(fx.doubleValue());
                                evt.setBaseCurrencyCapitalDelta(baseCurrencyCaptial2);
                            } else {
                                evt.setBaseCurrencyCapitalDelta(txn.getTransaction().getAmountBase());
                                // Warning this is not really tested this branch.
                                log.warn("Using transaction base amount directly. Fee structure assumed to not matter but not confirmed.");
                            }
                        } else {
                            totalBaseCapital = totalBaseCapital.add(txn.getAmount()); // .subtract(evt.getFee())
                        }
                    }
                } else {
                    // ADD Shares came from split of something else or special div etc. No captial
                    // inflow
                    evt.setCapitalDelta(0);
                }

                if (txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.REINVEST_DIVIDEND_COMBINED) {
                    evt.setIncome(resolveIncomeAmount(txn));
                }

                openTransactions.add(new TransactionTrancheItem(txn.getTransaction()));

                BigDecimal d = FinanceInvestmentEvent.setScale(new BigDecimal(quantity + (txn.getQuantity()), MC));
                evt.setHolding(d.doubleValue());
                quantity = evt.getHolding();
                evt.setQuantity(txn.getQuantity());
            } else if (
                txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.REINVEST_DIVIDEND_COMBINED ||
                txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.DIVIDEND
            ) {
                if (evt == null) {
                    // evt = new FinanceInvestmentEvent();
                    // evt.setUserGuid(user.getGuid().toString());
                    // evt.setUserSecurityId(userSecurityId);
                    // evt.setDate(txn.getDate());
                    evt = createFIE(user, userSecurityId, txn);
                }
                evt.setIncome(resolveIncomeAmount(txn));
                // totalIncome = evt.getIncome();
            } else if (txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.SELL) {
                CapitalGainEvent cge = trackPositionAfterSell(openTransactions, txn.getQuantity(), txn.getTransaction());
                cges.add(cge);
                totalCapital = totalCapital.subtract(FinanceInvestmentEvent.setScale(new BigDecimal(cge.capitalUsed, MC)));

                // evt = new FinanceInvestmentEvent();
                // evt.setUserSecurityId(userSecurityId);
                // evt.setUserGuid(user.getGuid().toString());
                // evt.setDate(txn.getDate());
                evt = createFIE(user, userSecurityId, txn);
                BigDecimal d = FinanceInvestmentEvent.setScale(new BigDecimal(quantity - txn.getQuantity(), MC));
                evt.setHolding(d.doubleValue());
                if (evt.getHolding() < 0.001) {
                    evt.setHolding(0.0);
                }
                evt.setQuantity(txn.getQuantity() * -1);
                evt.setCapitalDelta(cge.capitalUsed * -1);

                evt.setFee(Math.abs(Math.abs(txn.getAmount().doubleValue()) - Math.abs(txn.getPrice() * txn.getQuantity())));

                // BigDecimal localFee = new
                // BigDecimal(txn.getTransaction().getRateToBase()).multiply(evt.getFee());
                // Too simple below since this doesn't take into account currency FX of the
                // tranches at that date
                // evt.setBaseCurrencyCapitalDelta(new
                // BigDecimal(cge.capitalUsed*cge.fxRateToBase*-1));

                BigDecimal baseCurrencyCapitalDelta = FinanceInvestmentEvent.setScale(
                    new BigDecimal(cge.baseCurrencyCapitalUsed.doubleValue() * -1, MC)
                );
                evt.setBaseCurrencyCapitalDelta(baseCurrencyCapitalDelta);
                evt.setBaseCurrencyRealisedGain(FinanceInvestmentEvent.setScale(new BigDecimal(cge.baseCurrencyGain, MC)));

                evt.setRealisedGain(cge.gain);
                evt.setFeeDelta(FinanceInvestmentEvent.setScale(new BigDecimal(cge.fee, MC)));
                if (txn.getTransaction().getRateToBase() != null) {
                    BigDecimal fx = FinanceInvestmentEvent.setScale(new BigDecimal(txn.getTransaction().getRateToBase(), MC));
                    evt.setRateToBase(fx.doubleValue());
                }
                if (cge.currencyGain != null) {
                    evt.setRealisedCurrencyGain(FinanceInvestmentEvent.setScale(cge.currencyGain).round(MC));
                }

                quantity -= txn.getQuantity();
            }

            if (evt != null) {
                events.add(evt);
                log.info("FIE: " + evt);
            }
        }

        if (events.size() > 0) {
            if (usec != null) {
                //&& !usec.isEventsValid()
                usec.setEventsValid(true);
                this.save(user, usec);
                log.info("Generating events for: " + userSecurityId);
                investmentEventRepository.saveAll(events);
            } else {
                // TODO: Enhance this to have a number generated.
                log.warn("Events were already generated for this security. Not generating: " + userSecurityId);
                return;
            }
        }
    }

    private BigDecimal resolveIncomeAmount(FinanceInvestmentTransactionDTO txn) {
        if (txn.getAmount() != null && txn.getRateToBase() != null && txn.getRateToBase() != 0) {
            return txn.getAmount().abs().multiply(new BigDecimal(txn.getRateToBase(), FinanceInvestmentEvent.MC));
        }
        if (txn.getAmountBase() != null && txn.getAmountBase().compareTo(BigDecimal.ZERO) != 0) {
            return txn.getAmountBase().abs();
        }
        return txn.getAmount() != null ? txn.getAmount().abs() : BigDecimal.ZERO;
    }

    private FinanceInvestmentEvent createFIE(User user, String userSecurityId, FinanceInvestmentTransactionDTO txn) {
        //MathContext MC = FinanceInvestmentEvent.MC;
        FinanceInvestmentEvent evt = new FinanceInvestmentEvent();
        evt.setUserSecurityId(userSecurityId);
        evt.setCurrencyCode(txn.getTransaction().getCurrencyCode());
        evt.setUserGuid(user.getGuid().toString());
        evt.setDate(txn.getDate());
        // evt.setPrice(new BigDecimal(txn.getPrice(), MC));

        return evt;
    }

    public List<FinanceInvestmentSnapshotDetails> processSummaries(
        User user,
        List<FinanceUserSecurity> userSecurities,
        boolean includeClosed,
        LocalDate atDate
    ) {
        List<FinanceInvestmentSnapshotDetails> summaries = new ArrayList<FinanceInvestmentSnapshotDetails>();
        for (FinanceUserSecurity us : userSecurities) {
            // if(us.getSecurity() != null) {
            FinanceInvestmentSnapshotDetails summary = processSummary(user, us, includeClosed, atDate);
            if (summary != null) {
                summaries.add(summary);
            }
            // }
        }
        return summaries;
    }

    public FinanceInvestmentPortfolioSummaryDTO portfolioSummaries(
        User user,
        List<FinanceUserSecurity> userSecurities,
        boolean includeClosed,
        LocalDate atDate
    ) {
        List<FinanceInvestmentSnapshotDetails> summaries = processSummaries(user, userSecurities, includeClosed, atDate);
        List<FinanceInvestmentSnapshotDetails> rollupSummaries = summaries
            .stream()
            .filter(summary -> !summary.isIgnoredForRollup())
            .toList();

        FinanceInvestmentPortfolioSummaryDTO result = new FinanceInvestmentPortfolioSummaryDTO();
        result.setDate(atDate);
        result.setSummaries(summaries);
        result.setCurrencyIsoCode(user.getLocalCurrency());

        // Amalgamate summary
        BigDecimal totalCapitalInvested = BigDecimal.ZERO;
        BigDecimal totalCapitalGain = BigDecimal.ZERO;
        BigDecimal totalCurrencyGain = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalReturn = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        Double totalAyi = 0.0;
        for (FinanceInvestmentSnapshotDetails summary : rollupSummaries) {
            if (summary != null) {
                totalValue = totalValue.add(summary.getPrice().multiply(BigDecimal.valueOf(summary.getQuantity())));
                totalCurrencyGain = totalCurrencyGain.add(summary.getTotalCurrencyGain());
                // if (totalAyi <= summary.getAyi()) {
                //     totalAyi = summary.getAyi();
                // }

                //if (user.getLocalCurrency().equals(summary.getCurrencyIsoCode()) || summary.getFxToLocal() == null) {
                totalCapitalInvested = totalCapitalInvested.add(summary.getTotalCapitalInvested());
                totalCapitalGain = totalCapitalGain.add(summary.getTotalCapitalGain());
                totalIncome = totalIncome.add(summary.getTotalIncome());
                totalReturn = totalReturn.add(summary.getTotalReturn());
                // } else {
                //     totalCapitalInvested = totalCapitalInvested.add(new BigDecimal(
                //             summary.getTotalCapitalInvested().doubleValue() * summary.getFxToLocal()));
                //     totalCapitalGain = totalCapitalGain.add(
                //             new BigDecimal(summary.getTotalCapitalGain().doubleValue() * summary.getFxToLocal()));
                //     totalIncome = totalIncome
                //             .add(new BigDecimal(summary.getTotalIncome().doubleValue() * summary.getFxToLocal()));
                //     totalReturn = totalReturn
                //             .add(new BigDecimal(summary.getTotalReturn().doubleValue() * summary.getFxToLocal()));
                // }
                // log.info("" + summary.getUserSecurityId() + ", has capital of: "
                // +summary.getTotalCapitalInvested() );
            }
        }

        result.setTotalValue(totalValue);
        result.setTotalCapitalGain(totalCapitalGain);
        result.setTotalCapitalGain(totalCapitalGain);
        result.setTotalCapitalInvested(totalCapitalInvested);
        result.setTotalCurrencyGain(totalCurrencyGain);
        result.setTotalIncome(totalIncome);
        result.setTotalReturn(totalReturn);
        result.setTotalAyi(totalAyi);

        // Get Events
        List<String> rollupUserSecurityIds = rollupSummaries.stream().map(FinanceInvestmentSnapshotDetails::getUserSecurityId).toList();
        List<FinanceInvestmentEvent> allEvents = rollupUserSecurityIds.isEmpty()
            ? List.of()
            : this.investmentEventRepository.findAllByUserSecurityIdsOrderByDate(rollupUserSecurityIds, atDate);
        LocalDate now = LocalDate.now();
        double ayi = calcAYIFromEvents(allEvents, totalCapitalInvested.doubleValue(), now, includeClosed);

        log.info("AYI: " + ayi + ", total: " + result.getTotalAyi());

        setPercentages(result, rollupSummaries, ayi);

        //
        // double md = processDataWithModifiedDietz(allEvents, result.getTotalValue(),
        // now);
        // FinanceInvestmentEvent event1 = allEvents.get(0);
        // LocalDate b = event1.getDate();
        // long days = ChronoUnit.DAYS.between(b, now);
        // double years = ((double)days/365);
        // log.info("Modified Dietz: " + formatPC(md) + ", Days Invested: " + days + "
        // (" + years + ") - MDpa:" + formatPC(md/years) );

        return result;
    }

    public void setPercentages(FinanceInvestmentPortfolioSummaryDTO result, List<FinanceInvestmentSnapshotDetails> summaries, double ayi) {
        double totalCapitalGainCAGR = calcCAGR(
            result.getTotalCapitalGain().doubleValue(),
            result.getTotalCapitalInvested().doubleValue(),
            ayi
        );
        result.setTotalCapitalGainCAGR(totalCapitalGainCAGR);
        result.setTotalCapitalGainPC(
            calcReturn(result.getTotalCapitalGain().doubleValue(), result.getTotalCapitalInvested().doubleValue(), ayi)
        );

        double totalCurrencyGainCAGR = calcCAGR(
            result.getTotalCurrencyGain().doubleValue(),
            result.getTotalCapitalInvested().doubleValue(),
            ayi
        );
        result.setTotalCurrencyGainCAGR(totalCurrencyGainCAGR);
        result.setTotalCurrencyGainPC(
            calcReturn(result.getTotalCurrencyGain().doubleValue(), result.getTotalCapitalInvested().doubleValue(), ayi)
        );

        double totalIncomeCagr = calcCAGR(result.getTotalIncome().doubleValue(), result.getTotalCapitalInvested().doubleValue(), ayi); // MPL diff +1232.70
        result.setTotalIncomeCAGR(totalIncomeCagr);
        result.setTotalIncomePC(calcReturn(result.getTotalIncome().doubleValue(), result.getTotalCapitalInvested().doubleValue(), ayi));

        double totalReturnCagr = calcCAGR(result.getTotalReturn().doubleValue(), result.getTotalCapitalInvested().doubleValue(), ayi);
        result.setTotalReturnCAGR(totalReturnCagr);
        result.setTotalReturnPC(calcReturn(result.getTotalReturn().doubleValue(), result.getTotalCapitalInvested().doubleValue(), ayi));
        log.info(
            "PORTFOLIO: TR(CAGR): " +
                result.getTotalReturnCAGR() +
                ", TR(s): " +
                result.getTotalReturnPC() +
                ", TCap(CAGR): " +
                result.getTotalCapitalGainCAGR() +
                ", TCap(s): " +
                result.getTotalCapitalGainPC() +
                ", TIn(CAGR): " +
                result.getTotalIncomeCAGR() +
                ", TIn(s): " +
                result.getTotalIncomePC() +
                ", TCur(CAGR): " +
                result.getTotalCurrencyGainCAGR() +
                ", TCur(s): " +
                result.getTotalCurrencyGainPC()
        );
    }

    public FinanceInvestmentSnapshotDetails processSummary(
        User user,
        FinanceUserSecurity userSecurity,
        boolean includeClosed,
        LocalDate atDate
    ) {
        // Get Events
        List<FinanceInvestmentEvent> events = this.investmentEventRepository.findAllByUserSecurityIdOrderByDate(
            userSecurity.getId().toString(),
            atDate
        );

        // Set up sums
        double quantity = 0;
        BigDecimal totalCapital = BigDecimal.ZERO;
        BigDecimal totalForeignCapital = BigDecimal.ZERO; // For currency gain calcs
        BigDecimal totalCapitalInBaseCurrency = BigDecimal.ZERO;
        BigDecimal totalForeignCapitalInBaseCurrency = BigDecimal.ZERO; // For currency gain calcs
        BigDecimal totalClosedCapital = BigDecimal.ZERO;
        BigDecimal totalClosedCapitalInBaseCurrency = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalRealisedCurrencyGain = BigDecimal.ZERO;
        BigDecimal totalRealisedCapitalGain = BigDecimal.ZERO;
        BigDecimal totalRealisedCapitalGainInBaseCurrency = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal totalRealisedFees = BigDecimal.ZERO;

        LocalDate positionOpenDate = null;

        for (FinanceInvestmentEvent fie : events) {
            if (positionOpenDate == null) {
                positionOpenDate = fie.getDate();
            }
            if (fie.getQuantity() != null) {
                quantity = quantity + fie.getQuantity();
            }
            if (fie.getCapitalDelta() != null) {
                totalCapital = totalCapital.add(fie.getCapitalDelta());
                if (fie.getCapitalDelta().signum() < 0) {
                    totalClosedCapital = totalClosedCapital.add(fie.getCapitalDelta().abs());
                }
                if (fie.getCurrencyCode() != null && !fie.getCurrencyCode().equals(user.getLocalCurrency())) {
                    totalForeignCapital = totalForeignCapital.add(fie.getCapitalDelta());
                }
                // log.info("Total Captial: " + totalCapital + "(" + fie.getCapitalDelta()+")");
            }
            if (fie.getBaseCurrencyCapitalDelta() != null) {
                totalCapitalInBaseCurrency = totalCapitalInBaseCurrency.add(fie.getBaseCurrencyCapitalDelta());
                if (fie.getBaseCurrencyCapitalDelta().signum() < 0) {
                    totalClosedCapitalInBaseCurrency = totalClosedCapitalInBaseCurrency.add(fie.getBaseCurrencyCapitalDelta().abs());
                }
                if (fie.getCurrencyCode() != null && !fie.getCurrencyCode().equals(user.getLocalCurrency())) {
                    totalForeignCapitalInBaseCurrency = totalForeignCapitalInBaseCurrency.add(fie.getBaseCurrencyCapitalDelta());
                }
            }
            if (fie.getRealisedGain() != null) {
                totalRealisedCapitalGain = totalRealisedCapitalGain.add(fie.getRealisedGain()); // .subtract(fie.getFee())
            }
            if (fie.getBaseCurrencyRealisedGain() != null) {
                totalRealisedCapitalGainInBaseCurrency = totalRealisedCapitalGainInBaseCurrency.add(fie.getBaseCurrencyRealisedGain()); // .subtract(fie.getFee())
            }
            if (fie.getIncome() != null) {
                totalIncome = totalIncome.add(fie.getIncome());
            }
            if (fie.getRealisedCurrencyGain() != null) {
                totalRealisedCurrencyGain = totalRealisedCurrencyGain.add(fie.getRealisedCurrencyGain());
            }

            if (fie.getFee() != null) {
                totalFees = totalFees.add(fie.getFee());
            }

            if (fie.getFeeDelta() != null) {
                totalRealisedFees = totalRealisedFees.add(fie.getFeeDelta());
            }

            if (quantity <= 0 && !includeClosed) {
                totalCapital = BigDecimal.ZERO;
                totalCapitalInBaseCurrency = BigDecimal.ZERO;
                totalForeignCapital = BigDecimal.ZERO;
                totalForeignCapitalInBaseCurrency = BigDecimal.ZERO;
                totalClosedCapital = BigDecimal.ZERO;
                totalClosedCapitalInBaseCurrency = BigDecimal.ZERO;
                totalIncome = BigDecimal.ZERO;
                totalRealisedCurrencyGain = BigDecimal.ZERO;
                totalRealisedCapitalGain = BigDecimal.ZERO;
                totalRealisedCapitalGainInBaseCurrency = BigDecimal.ZERO;
                // totalFees = BigDecimal.ZERO;
                positionOpenDate = null;
            }
        }

        // Find latest price and details
        List<String> symbols = new ArrayList<String>();
        // TODO: FIX HERE FOR NON SECURITIES!!!

        BigDecimal currentValue = BigDecimal.ZERO;
        BigDecimal currentPrice = BigDecimal.ZERO;
        ZonedDateTime currentPriceDateTime = null;
        LocalDate currentPriceDate = null;

        if (userSecurity.getSecurity() != null) {
            symbols.add(userSecurity.getSecurity().getSymbol());
            List<FinanceSecurityPrice> oprice = this.securitySPRepository.findLatestBySymbolsAndBeforeDate(symbols, atDate.plusDays(1));
            if (oprice == null || oprice.size() != 1) {
                Optional<FinanceInvestmentEvent> latestPricedEvent = findLatestPricedEvent(events, atDate);
                if (latestPricedEvent.isEmpty()) {
                    log.error("Couldn't find SP: " + userSecurity.getSecurity().getSymbol());
                    return null;
                }
                FinanceInvestmentEvent priceEvent = latestPricedEvent.get();
                currentPrice = priceEvent.getPrice();
                currentPriceDate = priceEvent.getDate();
                log.warn(
                    "Couldn't find SP: {} before {}, using latest investment event price {} from {}",
                    userSecurity.getSecurity().getSymbol(),
                    atDate,
                    currentPrice,
                    currentPriceDate
                );
            } else {
                // Get calculate the latest gain....
                currentPrice = oprice.get(0).getPrice();
                currentPriceDateTime = oprice.get(0).getDate();
                currentPriceDate = currentPriceDateTime.toLocalDate();
            }
            currentValue = currentPrice.multiply(new BigDecimal(quantity));
        } else {
            // Not set
        }

        FinanceInvestmentSnapshotDetails dto = new FinanceInvestmentSnapshotDetails();
        dto.setUserSecurityId(userSecurity.getId().toString());
        dto.setIgnoredForRollup(userSecurity.isIgnoredForRollup());
        dto.setIgnoredForRollupReason(userSecurity.getIgnoredForRollupReason());
        dto.setPrice(currentPrice);
        dto.setQuantity(quantity);
        if (currentPriceDate != null) {
            dto.setPriceDate(currentPriceDate);
        }

        dto.setTotalCapitalGain(BigDecimal.ZERO);
        dto.setTotalIncome(BigDecimal.ZERO);
        dto.setTotalCurrencyGain(BigDecimal.ZERO);
        dto.setTotalReturn(BigDecimal.ZERO);

        // Fees only tallied for closed so include them
        dto.setTotalFees(totalFees);

        dto.setTotalCapitalGain(currentValue.subtract(totalCapital)); // .subtract(dto.getTotalFees())
        dto.setCurrencyIsoCode(userSecurity.getCurrencyCode());

        BigDecimal capitalInvestedForReturn = includeClosed ? totalCapital.add(totalClosedCapital) : totalCapital;
        BigDecimal capitalInvestedInBaseForReturn = includeClosed
            ? totalCapitalInBaseCurrency.add(totalClosedCapitalInBaseCurrency)
            : totalCapitalInBaseCurrency;

        if (userSecurity.getCurrencyCode() != null && !userSecurity.getCurrencyCode().equals(user.getLocalCurrency())) {
            dto.setTotalCapitalInvested(capitalInvestedInBaseForReturn);
            FinanceFX fx = fxService.getLatestFX(userSecurity.getCurrencyCode(), user.getLocalCurrency(), atDate.plusDays(1));
            if (fx == null) {
                // Just get the last known
                fx = fxService.getLatestFX(userSecurity.getCurrencyCode(), user.getLocalCurrency(), LocalDate.now().plusDays(1));
            }
            BigDecimal totalForeignCapitalTodayInBaseCurrency = totalForeignCapital.multiply(new BigDecimal(fx.getRate()));
            // *CALC*
            // Currency Gain is the value of the capital invested using todays FX value MINUS the actual value (in base currency)
            dto.setTotalCurrencyGain(totalForeignCapitalTodayInBaseCurrency.subtract(totalForeignCapitalInBaseCurrency));
            dto.setFxToLocal(fx.getRate());
            dto.setFxDate(fx.getDate().toLocalDate());
            // Adjust gain for local currency as well for gain
            BigDecimal currentValueInBaseCurrency = currentValue.multiply(new BigDecimal(fx.getRate()));
            dto.setTotalCapitalGain(currentValueInBaseCurrency.subtract(totalCapitalInBaseCurrency).subtract(dto.getTotalCurrencyGain()));
        } else {
            dto.setTotalCapitalInvested(capitalInvestedForReturn);
        }

        if (includeClosed) {
            if (userSecurity.getCurrencyCode() != null && !userSecurity.getCurrencyCode().equals(user.getLocalCurrency())) {
                // Add in the closed Captial Gains
                dto.setTotalCapitalGain(dto.getTotalCapitalGain().add(totalRealisedCapitalGainInBaseCurrency));
                // Previous currency gain is already included int he total currency gain number
                // (need to remove the realised gain if closed in else statement)
            } else {
                // Add in the closed Captial Gains
                dto.setTotalCapitalGain(dto.getTotalCapitalGain().add(totalRealisedCapitalGain));
                // Previous currency gain is already included int he total currency gain number
                // (need to remove the realised gain if closed in else statement)
            }

            dto.setTotalCurrencyGain(dto.getTotalCurrencyGain().add(totalRealisedCurrencyGain));
        } else {
            dto.setTotalFees(dto.getTotalFees().subtract(totalRealisedFees));
        }

        dto.setTotalIncome(totalIncome);
        dto.setTotalReturn(dto.getTotalCapitalGain().add(totalIncome).add(dto.getTotalCurrencyGain().subtract(dto.getTotalFees())));

        // HERE - What if zero? TODO
        // String type = includeClosed ? "all" : "onlyOpen";

        double ayi = calcAYIFromEvents(
            events,
            dto.getTotalCapitalInvested().doubleValue(),
            LocalDate.now(),
            includeClosed,
            userSecurity.getCurrencyCode() != null && !userSecurity.getCurrencyCode().equals(user.getLocalCurrency())
        );
        if (Double.isInfinite(ayi)) {
            ayi = 0;
        } else if (ayi < 1) {
            ayi = 1;
            // If AYI is less than 1 year then make 1 year to remove issues with AYI less
            // than 1 year...
        }

        //dto.setAyi(ayi);

        double totalCapitalGainCAGR = calcCAGR(dto.getTotalCapitalGain().doubleValue(), dto.getTotalCapitalInvested().doubleValue(), ayi);
        dto.setTotalCapitalGainCAGR(totalCapitalGainCAGR);
        dto.setTotalCapitalGainPC(calcReturn(dto.getTotalCapitalGain().doubleValue(), dto.getTotalCapitalInvested().doubleValue(), ayi));

        double totalCurrencyGainCAGR = calcCAGR(dto.getTotalCurrencyGain().doubleValue(), dto.getTotalCapitalInvested().doubleValue(), ayi);
        dto.setTotalCurrencyGainCAGR(totalCurrencyGainCAGR);
        dto.setTotalCurrencyGainPC(calcReturn(dto.getTotalCurrencyGain().doubleValue(), dto.getTotalCapitalInvested().doubleValue(), ayi));

        double totalIncomeCagr = calcCAGR(dto.getTotalIncome().doubleValue(), dto.getTotalCapitalInvested().doubleValue(), ayi); // MPL diff +1232.70
        dto.setTotalIncomeCAGR(totalIncomeCagr);
        dto.setTotalIncomePC(calcReturn(dto.getTotalIncome().doubleValue(), dto.getTotalCapitalInvested().doubleValue(), ayi));

        double totalReturnCagr = calcCAGR(dto.getTotalReturn().doubleValue(), dto.getTotalCapitalInvested().doubleValue(), ayi);
        dto.setTotalReturnCAGR(totalReturnCagr);
        dto.setTotalReturnPC(calcReturn(dto.getTotalReturn().doubleValue(), dto.getTotalCapitalInvested().doubleValue(), ayi));

        log.info(
            dto.getUserSecurityId() +
                ", TR(CAGR): " +
                dto.getTotalReturnCAGR() +
                ", TR(s): " +
                dto.getTotalReturnPC() +
                ", TCap(CAGR): " +
                dto.getTotalCapitalGainCAGR() +
                ", TCap(s): " +
                dto.getTotalCapitalGainPC() +
                ", TIn(CAGR): " +
                dto.getTotalIncomeCAGR() +
                ", TIn(s): " +
                dto.getTotalIncomePC() +
                ", TCur(CAGR): " +
                dto.getTotalCurrencyGainCAGR() +
                ", TCur(s): " +
                dto.getTotalCurrencyGainPC()
        );

        // double costBasis=dto.getTotalCapitalInvested().doubleValue()/quantity;
        // double returnPC =
        // (dto.getTotalReturn().doubleValue()/dto.getTotalCapitalInvested().doubleValue());
        // Period periodOpen = Period.between(positionOpenDate, LocalDate.now());
        // double returnPCPA = (returnPC/(ChronoUnit.DAYS.between(positionOpenDate,
        // LocalDate.now())/365)) ;
        // dto.setTotalReturnPC(returnPCPA);

        LocalDate now = LocalDate.now();
        double val = processDataWithModifiedDietz(events, currentValue, now);
        //dto.setModDietzPC(val);

        if (events != null && events.size() > 0) {
            FinanceInvestmentEvent event1 = events.get(0);
            LocalDate b = event1.getDate();
            long days = ChronoUnit.DAYS.between(b, now);
            // double years = ((double) days / 365);
            //log.info("Modified Dietz: " + formatPC(val) + ", Days Invested: " + days + " (" + years + ") - MDpa:"
            //        + formatPC(val / years));
            //dto.setModDietzPCpa(val / years);
        }

        return dto;
    }

    private Optional<FinanceInvestmentEvent> findLatestPricedEvent(List<FinanceInvestmentEvent> events, LocalDate atDate) {
        if (events == null || events.isEmpty()) {
            return Optional.empty();
        }
        FinanceInvestmentEvent result = null;
        for (FinanceInvestmentEvent event : events) {
            if (event.getPrice() != null && event.getDate() != null && !event.getDate().isAfter(atDate)) {
                result = event;
            }
        }
        return Optional.ofNullable(result);
    }

    public static double calcAYIFromEvents(
        List<FinanceInvestmentEvent> events,
        double totalCapital,
        LocalDate toDate,
        boolean includeClosed
    ) {
        return calcAYIFromEvents(events, totalCapital, toDate, includeClosed, false);
    }

    public static double calcAYIFromEvents(
        List<FinanceInvestmentEvent> events,
        double totalCapital,
        LocalDate toDate,
        boolean includeClosed,
        boolean useBaseCurrencyCapital
    ) {
        if (events == null || events.isEmpty() || totalCapital == 0) {
            return 0;
        }
        double capitalDays = 0;
        double activeCapital = 0;
        double quantity = 0;
        LocalDate previousDate = null;
        for (FinanceInvestmentEvent event : events) {
            if (previousDate != null && activeCapital != 0) {
                capitalDays += activeCapital * ChronoUnit.DAYS.between(previousDate, event.getDate());
            }

            BigDecimal capitalDelta =
                useBaseCurrencyCapital && event.getBaseCurrencyCapitalDelta() != null
                    ? event.getBaseCurrencyCapitalDelta()
                    : event.getCapitalDelta();
            if (capitalDelta != null) {
                activeCapital += capitalDelta.doubleValue();
                if (Math.abs(activeCapital) < 0.00001) {
                    activeCapital = 0;
                }
            }
            if (event.getQuantity() != null) {
                quantity = quantity + event.getQuantity();
                if (quantity == 0 && !includeClosed) {
                    activeCapital = 0;
                    capitalDays = 0;
                }
            }
            previousDate = event.getDate();
        }

        if (previousDate != null && activeCapital != 0) {
            capitalDays += activeCapital * ChronoUnit.DAYS.between(previousDate, toDate);
        }

        // Now div by 365 to get years
        double ayi = capitalDays / totalCapital / 365;

        logStaticAyi(ayi);
        return ayi;
    }

    private static void logStaticAyi(double ayi) {
        System.out.println("AYI: " + ayi);
    }

    public Optional<FinanceUserSecurity> getUserSecurity(User user, String id) {
        return userSecurityRepository.findByIdAndUserGuid(UUID.fromString(id), user.getGuid().toString());
    }

    public List<FinanceUserSecurity> getUserSecurities(User user, String accountId) {
        List<FinanceUserSecurity> fus = userSecurityRepository.findByUserGuid(user.getGuid().toString());
        log.warn("getUserSecurities initial lookup. userGuid={}, accountId={}, repositoryCount={}", user.getGuid(), accountId, fus.size());

        if (fus.isEmpty()) {
            List<FinanceSecurityInvestmentSummary> securityTransactions;
            if (accountId != null) {
                securityTransactions = this.financeTransactionService.getFinanceSecurityInvestmentTransactionsForAccount(
                    user,
                    accountId,
                    true
                );
            } else {
                securityTransactions = this.financeTransactionService.getFinanceSecurityInvestmentTransactions(user, true, null);
            }

            Map<String, String> securityIds = new HashMap<String, String>();
            for (FinanceSecurityInvestmentSummary item : securityTransactions) {
                if (item.getSecurityId() != null && !item.getSecurityId().isBlank()) {
                    securityIds.put(item.getSecurityId(), item.getSecurityId());
                }
            }

            List<UUID> ids = new ArrayList<UUID>();
            for (String securityId : securityIds.keySet()) {
                try {
                    ids.add(UUID.fromString(securityId));
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping non-UUID securityId from transaction summaries: {}", securityId);
                }
            }

            if (!ids.isEmpty()) {
                List<FinanceUserSecurity> resolved = new ArrayList<FinanceUserSecurity>();
                for (UUID id : ids) {
                    userSecurityRepository.findById(id).ifPresent(resolved::add);
                }
                fus = resolved;
                log.warn(
                    "getUserSecurities fallback lookup by transaction security ids. transactionSecurityCount={}, resolvedCount={}",
                    ids.size(),
                    fus.size()
                );
            } else {
                log.warn("getUserSecurities fallback found no candidate security ids in transaction summaries.");
            }
        }

        if (accountId != null) {
            // Since user securities are linked to account via transactions, not directly,
            // we need to remove user securities which are not in the account
            List<FinanceSecurityInvestmentSummary> securitiesInAccount =
                this.financeTransactionService.getFinanceSecurityInvestmentTransactionsForAccount(user, accountId, true);

            Map<String, FinanceSecurityInvestmentSummary> index = new HashMap<String, FinanceSecurityInvestmentSummary>();
            for (FinanceSecurityInvestmentSummary item : securitiesInAccount) {
                index.put(item.getSecurityId(), item);
            }
            List<FinanceUserSecurity> toRemove = new ArrayList<FinanceUserSecurity>();
            for (FinanceUserSecurity usToInterogate : fus) {
                if (index.get(usToInterogate.getId().toString()) == null) {
                    // Couldn't find it
                    toRemove.add(usToInterogate);
                    log.info(
                        "Going to remove userSecurityID: " +
                            usToInterogate.getId().toString() +
                            "(" +
                            usToInterogate.getName() +
                            ") since not in the account"
                    );
                }
            }

            fus.removeAll(toRemove);
        }

        log.warn("getUserSecurities final result count={}", fus.size());

        return fus;
    }

    public FinanceUserSecurity save(User user, FinanceUserSecurity usec) {
        if (user.getGuid().toString().equals(usec.getUserGuid())) {
            return userSecurityRepository.save(usec);
        } else {
            throw new RuntimeException("Not the owning user saving UserSecurity: " + user.getGuid().toString());
        }
    }

    public void removeClosedTransactions(List<FinanceInvestmentTransactionDTO> dtos) {
        int closedIndex = -1; // the index we have reached a new closed position at. -1 means still open.
        double quantity = 0;
        for (int i = dtos.size() - 1; i >= 0; i--) {
            FinanceInvestmentTransactionDTO txn = dtos.get(i);

            if (
                txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.BUY ||
                txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.REINVEST_DIVIDEND_COMBINED ||
                txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.ADD_SHARES
            ) {
                quantity += txn.getQuantity();
            }

            if (txn.getTransaction().getInvestmentActivityType() == FinanceInvestmentActivityType.SELL) {
                quantity -= txn.getQuantity();
            }

            if (quantity == 0) {
                closedIndex = i;
            }
        }

        if (closedIndex != -1) {
            int toRemove = dtos.size() - closedIndex - 1;
            for (int i = 0; i <= toRemove; i++) {
                dtos.remove(dtos.size() - 1);
            }
        }
    }

    public static String formatCurrency(BigDecimal in) {
        if (in == null) {
            return "";
        }
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);
        return numberFormat.format(in);
    }

    public static String formatCurrency(double in) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);
        return numberFormat.format(in);
    }

    public static String formatPC(double in) {
        NumberFormat numberFormat = NumberFormat.getPercentInstance();
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);
        return numberFormat.format(in);
    }

    public static double calcReturn(double gain, double totalCapital, double ayi) {
        if (totalCapital == 0 || ayi == 0) {
            return 0;
        }
        return (gain / totalCapital) / ayi;
    }

    public static double calcCAGR(List<TransactionTrancheItem> openTransactions, double gain, double totalCapital) {
        double ati = calcAYI(openTransactions, totalCapital);
        return calcCAGR(gain, totalCapital, ati);
    }

    public static double calcCAGR(double gain, double totalCapital, double ayi) {
        if (totalCapital == 0 || ayi == 0) {
            return 0;
        }
        double a = 1 + (gain / totalCapital);
        // System.out.println("ATI: " + ati + ", totalCaptial: " + totalCapital + ",
        // Gain: " + gain);
        double cagr = Math.pow(a, (1 / ayi)) - 1;
        return cagr;
    }

    public static double calcAYI(List<TransactionTrancheItem> openTransactions, double totalCapital) {
        double ati = 0;
        for (TransactionTrancheItem tranche : openTransactions) {
            long days = tranche.getDays(LocalDate.now());
            double proporation = Math.abs(tranche.txn.getAmount().doubleValue()) / totalCapital;
            ati = ati + (days * proporation);
            //System.out.println("ATI CALC: Days: " + days + ", Amount: " + tranche.txn.getAmount() + ", Total Captial: " + totalCapital);
        }

        // Now div by 365 to get years
        ati = ati / 365;

        return ati;
    }

    private CapitalGainEvent trackPositionAfterSell(
        List<TransactionTrancheItem> openTransactions,
        double quantity,
        FinanceTransaction txn
    ) {
        CapitalGainEvent cge = new CapitalGainEvent(txn);
        cge.gain = txn.getPrice() * txn.getQuantity();
        cge.price = txn.getPrice();
        double fee = Math.abs(Math.abs(txn.getAmount().doubleValue()) - cge.gain);
        cge.fee = fee;
        // cge.gain = cge.gain; // - fee // gainBeingCalculated starts at full sale
        // price - fees and then we remove the capital inflows

        trackPositionAfterSell(openTransactions, quantity, cge, true, (openTransactions.size() - 1));
        log.info("** CGE total of  $" + cge.gain);
        return cge;
    }

    private void trackPositionAfterSell(
        List<TransactionTrancheItem> openTranches,
        double quantity,
        CapitalGainEvent cge,
        boolean remove,
        int position
    ) {
        if (quantity > 0.0 && openTranches.size() == 0) {
            if (quantity > 0.0000001) {
                throw new RuntimeException("Selling more than available: " + quantity + " of " + cge.userSecurityId);
            } else {
                return; // minute left over.
            }
        }

        // Get the last open transaction
        TransactionTrancheItem tranche = openTranches.get(position);
        double cgeBaseCaptialAmount = 0;
        BigDecimal cgeBaseCurrencyAmount = null;
        double trancheGain = 0;

        if (quantity < tranche.quantity) {
            // Tranche is still open but will cover quanity in sell
            cgeBaseCaptialAmount = (tranche.txn.getPrice() * quantity);
            trancheGain = (cge.price * quantity) - cgeBaseCaptialAmount;
            tranche.quantity = tranche.quantity - quantity;
            tranche.capitalLeft = tranche.capitalLeft - cgeBaseCaptialAmount;
            quantity = 0;
            if (tranche.txn.getRateToBase() != null && tranche.txn.getAmountBase() != null) {
                // Different Base. Track for currency gain/loss
                cgeBaseCurrencyAmount = new BigDecimal(cgeBaseCaptialAmount * tranche.txn.getRateToBase(), FinanceInvestmentEvent.MC);
            }
        } else if (quantity >= tranche.quantity) {
            // Tranche is closed
            quantity = quantity - tranche.quantity;
            cgeBaseCaptialAmount = (tranche.txn.getPrice() * tranche.quantity);
            trancheGain = (cge.price * tranche.quantity) - cgeBaseCaptialAmount;
            tranche.capitalLeft = tranche.capitalLeft - cgeBaseCaptialAmount;
            cge.fee = cge.fee + tranche.fee;
            tranche.fee = 0;

            if (tranche.txn.getRateToBase() != null && tranche.txn.getAmountBase() != null) {
                // Different Base. Track for currency gain/loss
                cgeBaseCurrencyAmount = new BigDecimal(cgeBaseCaptialAmount * tranche.txn.getRateToBase(), FinanceInvestmentEvent.MC);
            }

            // Closing tranche
            tranche.quantity = 0;
            if (remove) {
                openTranches.remove(tranche);
            }
        }

        cge.gain = cge.gain - cgeBaseCaptialAmount; // - cge.fee
        cge.capitalUsed = cge.capitalUsed + cgeBaseCaptialAmount;

        if (cgeBaseCurrencyAmount != null) {
            BigDecimal adjCapitalAtBaseFX = new BigDecimal(cgeBaseCaptialAmount * cge.fxRateToBase, FinanceInvestmentEvent.MC);
            // 5000 - 5434 = negative value
            BigDecimal currencyGain = adjCapitalAtBaseFX.subtract(cgeBaseCurrencyAmount);
            if (cge.currencyGain == null) {
                cge.currencyGain = currencyGain;
            } else {
                cge.currencyGain = cge.currencyGain.add(currencyGain);
            }

            // Also track base currency captial that was used.
            // double calc = cgeBaseCaptialAmount * tranche.txn.getRateToBase();
            cge.baseCurrencyCapitalUsed = cge.baseCurrencyCapitalUsed.add(cgeBaseCurrencyAmount);

            cge.baseCurrencyGain = cge.baseCurrencyGain + (trancheGain * tranche.txn.getRateToBase());
        }

        log.info(
            "Logged base captial of " +
                formatCurrency(cgeBaseCaptialAmount) +
                " for tranche: " +
                tranche.txn.getDate() +
                " - qty in tranche: " +
                tranche.quantity +
                ". Capital used: " +
                formatCurrency(cge.capitalUsed) +
                ", Currency Gain: " +
                formatCurrency(cge.currencyGain)
        );

        if (quantity > 0) {
            trackPositionAfterSell(openTranches, quantity, cge, remove, position - 1);
        }
    }

    private double processDataWithModifiedDietz(List<FinanceInvestmentEvent> eventsIn, BigDecimal currentValue, LocalDate periodEndDate) {
        List<FinanceInvestmentEvent> events = new ArrayList<FinanceInvestmentEvent>();
        events.addAll(eventsIn);
        if (events.size() == 0) {
            return 0;
        }
        // Initial values
        FinanceInvestmentEvent event1 = events.get(0);
        BigDecimal bmv = event1.getBaseCurrencyCapitalDelta();
        if (bmv == null) {
            bmv = event1.getCapitalDelta();
        }
        LocalDate b = event1.getDate();
        double emv = currentValue.doubleValue();

        events.remove(0);

        // Cashflow
        List<Double> cashFlows = new ArrayList<Double>();
        List<Integer> numDays = new ArrayList<Integer>();

        for (FinanceInvestmentEvent fie : events) {
            BigDecimal v = fie.getBaseCurrencyCapitalDelta();
            if (v == null) {
                v = fie.getCapitalDelta();
            }
            if (fie.getBaseCurrencyRealisedGain() != null) {
                v = v.subtract(fie.getBaseCurrencyRealisedGain());
            }

            // if(fie.getIncome() != null) {
            // if(v == null) {
            // v = fie.getIncome();
            // } else {
            // v
            // }
            // }

            if (v != null) {
                cashFlows.add(v.doubleValue());
                long l = ChronoUnit.DAYS.between(b, fie.getDate());
                numDays.add((int) l);
            }
        }

        int numD[] = org.apache.commons.lang.ArrayUtils.toPrimitive(numDays.toArray(new Integer[0]));

        int numCD = (int) ChronoUnit.DAYS.between(b, periodEndDate);

        double[] cf = org.apache.commons.lang.ArrayUtils.toPrimitive(cashFlows.toArray(new Double[0]));
        log.info(
            "Calling MD with emv: " +
                emv +
                ", bmv:" +
                bmv +
                ", cf: " +
                Arrays.toString(cf) +
                ", numCD: " +
                numCD +
                ", numD: " +
                Arrays.toString(numD)
        );
        return modifiedDietz(emv, bmv.doubleValue(), cf, numCD, numD);
    }

    /*
     * emv: Ending Market Value
     * bmv: Beginning Market Value
     * cashFlow[]: Cash Flow
     * numCD: actual number of days in the period
     * numD[]: number of days between beginning of the period and date of cashFlow[]
     */
    public static double modifiedDietz(double emv, double bmv, double[] cashFlow, int numCD, int[] numD) {
        double md = -99999; // initialize modified dietz with a debugging number

        try {
            double[] weight = new double[cashFlow.length];

            if (numCD <= 0) {
                throw new ArithmeticException("numCD <= 0");
            }

            for (int i = 0; i < cashFlow.length; i++) {
                if (numD[i] < 0) {
                    throw new ArithmeticException("numD[i]<0 , " + "i=" + i);
                }
                weight[i] = (double) (numCD - numD[i]) / numCD;
            }

            double ttwcf = 0; // total time weighted cash flows
            for (int i = 0; i < cashFlow.length; i++) {
                ttwcf += weight[i] * cashFlow[i];
            }

            double tncf = 0; // total net cash flows
            for (int i = 0; i < cashFlow.length; i++) {
                tncf += cashFlow[i];
            }

            md = (emv - bmv - tncf) / (bmv + ttwcf);
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (ArithmeticException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return md;
    }
}

class CapitalGainEvent {

    LocalDate date;
    double quantity;
    UUID userSecurityId;
    double fxRateToBase;
    double price;
    double gain;
    double baseCurrencyGain; // this is different from currency gain! naming is weird - I agree. This is to
    // add and for the base currency realised gain FIE
    BigDecimal currencyGain = BigDecimal.ZERO;
    double capitalUsed;
    BigDecimal baseCurrencyCapitalUsed = BigDecimal.ZERO;

    double fee;

    public CapitalGainEvent(FinanceTransaction txn) {
        this.date = txn.getDate();
        this.quantity = txn.getQuantity();
        this.userSecurityId = UUID.fromString(txn.getSecurityId());
        this.fxRateToBase = txn.getRateToBase();
    }

    public CapitalGainEvent() {}
}

class TransactionTrancheItem {

    double quantity;
    double fxRate;
    double capitalLeft;
    FinanceTransaction txn;
    double fee;

    public TransactionTrancheItem(FinanceTransaction txn) {
        this.txn = txn;
        this.capitalLeft = txn.getQuantity() * txn.getPrice();
        this.fee = Math.abs(txn.getAmount().doubleValue()) - this.capitalLeft;
        this.quantity = txn.getQuantity();
        // this.userSecurityId = UUID.fromString(txn.getSecurityId());
    }

    long getDays(LocalDate to) {
        if (txn != null) {
            return ChronoUnit.DAYS.between(txn.getDate(), to);
        } else {
            throw new RuntimeException("Transaction missing");
        }
    }
}

class HoldingSnapshot implements Serializable {

    LocalDate date;
    double quantity = 0;
    UUID userSecurityId;
    double fxRateToBase;
    double price;
}
