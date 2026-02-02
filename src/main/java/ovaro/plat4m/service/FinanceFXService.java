package ovaro.plat4m.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceCurrencyRepository;
import ovaro.plat4m.repository.FinanceFXRepository;
import ovaro.plat4m.service.dto.FinanceSnapshot;

@Service
public class FinanceFXService {

    private final Logger log = LoggerFactory.getLogger(FinanceFXService.class);

    private FinanceFXRepository fxRepository;

    //private FinanceCurrencyRepository currencyRepository;

    public FinanceFXService(FinanceFXRepository fxRepository) { // , FinanceCurrencyRepository currencyRepository
        this.fxRepository = fxRepository;
        //this.currencyRepository = currencyRepository;
    }

    public FinanceFX getLatestFX(String from, String to, LocalDate date) {
        StopWatch sw = new StopWatch();
        sw.start("getLastestFX");
        List<FinanceFX> fxs = this.fxRepository.findByFromIsoCodeAndToIsoCode(from, to, date);
        // Check result:
        FinanceFX result = null;
        FinanceFX other = null;

        for (FinanceFX fx : fxs) {
            // Find the right one
            if (fx.getFromIsoCode().equals(from)) {
                result = fx;
            }

            if (fx.getToIsoCode().equals(from)) {
                other = fx;
            }
        }

        if (result == null && other != null) {
            result = new FinanceFX(other.getDate(), from, to, 1 / other.getRate());
        }

        sw.stop();
        log.info(sw.prettyPrint());
        return result;
    }

    // public List<FinanceFX> getLatestFX(List<String> from, String to) {
    //     StopWatch sw = new StopWatch();
    //     sw.start("getLastestFX");
    //     List<FinanceFX> accounts = this.fxRepository.findLatestFX(from, to);
    //     sw.stop();
    //     log.info(sw.prettyPrint());
    //     return accounts;
    // }

    public List<FinanceFX> getLatestFXAll() {
        StopWatch sw = new StopWatch();
        sw.start("getLastestFX");
        List<FinanceFX> accounts = this.fxRepository.findAll();
        sw.stop();
        log.info(sw.prettyPrint());
        return accounts;
    }

    public List<FinanceFX> findFXSummaries(String fromIsoCode, String toIsoCode, LocalDate fromDate, LocalDate toDate, int days) {
        if (days > 1) {
            return this.fxRepository.findFXSummaries(fromIsoCode, toIsoCode, fromDate, toDate, days);
        } else {
            return this.fxRepository.monthlyFX(fromIsoCode, toIsoCode, fromDate, toDate);
        }
    }

    // Expects snapshots to be in order
    public void addFXDetailsToMonthlySnapshots(User user, String currencyCode, List<FinanceSnapshot> snapshots) {
        Map<String, List<FinanceFX>> cache = new HashMap<String, List<FinanceFX>>();
        if (snapshots != null && snapshots.size() > 0) {
            // Check the snapshots which should be consistent on what currency they are in (i.e. cannot change)
            FinanceSnapshot sFirst = snapshots.get(0);

            if (currencyCode != null && !currencyCode.equals(user.getLocalCurrency())) {
                // If currency code is not the local one (or not set which implies local)

                FinanceSnapshot sLast = snapshots.get(snapshots.size() - 1);
                LocalDate start = sFirst.getDate();
                LocalDate end = sLast.getDate();

                List<FinanceFX> fxs = cache.get(currencyCode);
                if (fxs == null) {
                    fxs = this.fxRepository.monthlyFXAsending(currencyCode, user.getLocalCurrency(), start.minus(Period.ofMonths(2)), end); // Just get two months just in case.

                    // If no records we should at least grab the earliest value available and shove in.
                    if (fxs == null) {
                        fxs = new ArrayList<FinanceFX>();
                    }
                    if (fxs.size() == 0) {
                        FinanceFX fx = this.getLatestFX(currencyCode, user.getLocalCurrency(), start);
                        fxs.add(fx);
                    }
                    cache.put(currencyCode, fxs);
                }

                addFXDetailsToMonthlySnapshots(user, currencyCode, snapshots, fxs);
            }
        }
    }

    private void addFXDetailsToMonthlySnapshots(User user, String currencyCode, List<FinanceSnapshot> snapshots, List<FinanceFX> fxs) {
        Iterator<FinanceFX> fxIterator = fxs.iterator();
        FinanceFX lastFX = null;
        FinanceFX fx = null;
        LocalDate fxDate = null;
        for (FinanceSnapshot s : snapshots) {
            if (fx == null && fxIterator.hasNext()) {
                fx = fxIterator.next();
                if (fx != null) {
                    fxDate = fx.getDate().toLocalDate();
                }
            } else {
                // End of FXs
            }

            if (fxDate != null && fx != null && s.getDate().compareTo(fxDate) == 0) {
                lastFX = fx;
                s.setFxDate(fxDate);
                s.setFxToLocal(fx.getRate());
            } else if (fxDate != null && fx != null && s.getDate().compareTo(fxDate) > 0) {
                // Continue rolling forward until equal or less
                lastFX = fx;
                while (s.getDate().compareTo(fxDate) > 0) {
                    if (fxIterator.hasNext()) {
                        fx = fxIterator.next();

                        fxDate = fx.getDate().toLocalDate();
                        if (s.getDate().compareTo(fxDate) >= 0) {
                            lastFX = fx;
                        }
                    } else {
                        break;
                    }
                }
                s.setFxDate(lastFX.getDate().toLocalDate());
                s.setFxToLocal(lastFX.getRate());
            } else if (lastFX != null) {
                // Snapshot date is before the FX date but there was a lastFX we can use
                s.setFxDate(lastFX.getDate().toLocalDate());
                s.setFxToLocal(lastFX.getRate());
            } else {
                log.warn("No previous FX rate to use for snapshot date: " + s.getDate() + ", querying again (inefficient)");
                lastFX = this.getLatestFX(currencyCode, user.getLocalCurrency(), s.getDate());
                if (lastFX == null) {
                    // get the latest rate we can
                    lastFX = this.getLatestFX(currencyCode, user.getLocalCurrency(), LocalDate.now());
                }

                if (lastFX != null) {
                    s.setFxDate(lastFX.getDate().toLocalDate());
                    s.setFxToLocal(lastFX.getRate());
                }
            }
        }
    }
}
