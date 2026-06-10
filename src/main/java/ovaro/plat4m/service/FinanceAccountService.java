package ovaro.plat4m.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceAccountType;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.domain.FinanceSecurityInvestmentSummary;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.IFinanceCategorisedIncomeExpenses;
import ovaro.plat4m.domain.IFinanceMonthlySummary;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.service.dto.FinanceAccountDTO;
import ovaro.plat4m.service.dto.FinanceIndicatorDTO;
import ovaro.plat4m.service.dto.FinanceSecurityHoldingDTO;
import ovaro.plat4m.service.dto.FinanceSnapshot;
import ovaro.plat4m.service.dto.FinanceSnapshotWithComparison;
import ovaro.plat4m.service.dto.FinanceSnapshotsDTO;
import ovaro.plat4m.service.dto.FinanceSnapshotsPerResourceDTO;
import ovaro.plat4m.service.dto.FinanceTransactionRowDTO;
import ovaro.plat4m.service.mapper.FinanceAccountMapper;

@Service
public class FinanceAccountService {

    private final Logger log = LoggerFactory.getLogger(FinanceAccountService.class);

    private FinanceAccountRepository accountRepository;
    private FinanceTransactionService transactionService;
    private FinanceFXService fxService;
    private FinanceAccountMapper financeAccountMapper;
    private FinanceSecurityService securityService;

    public FinanceAccountService(
        FinanceAccountRepository accountRepository,
        FinanceTransactionService transactionService,
        FinanceAccountMapper financeAccountMapper,
        FinanceSecurityService securityService,
        FinanceFXService fxService
    ) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.financeAccountMapper = financeAccountMapper;
        this.securityService = securityService;
        this.fxService = fxService;
    }

    public List<FinanceAccountDTO> getAccounts(User user, Integer type, Boolean closed) {
        StopWatch sw = new StopWatch();
        sw.start("getAccounts");
        List<FinanceAccount> accounts = null;
        if (type == null && closed == null) {
            accounts = this.accountRepository.findAllByUserGuid(user.getGuid().toString());
        } else if (type == null && closed != null) {
            accounts = this.accountRepository.findAllByUserGuidAndClosed(user.getGuid().toString(), closed);
        } else if (closed == null) {
            accounts = this.accountRepository.findAllByUserGuidAndType(user.getGuid().toString(), type);
        } else {
            accounts = this.accountRepository.findAllByUserGuidAndTypeAndClosed(user.getGuid().toString(), type, closed);
        }
        List<FinanceAccountDTO> accountDTOs = financeAccountMapper.accountToAccountDTOs(accounts);
        // this.processFinanceAccountDTOs(user, accountDTOs);
        // for(FinanceAccountDTO account : accountDTOs) {
        // BigDecimal balance = this.transactionService.getAccountBalance(account);
        // account.setBalance(balance);
        // }
        sw.stop();
        log.info(sw.prettyPrint());
        this.processFinanceAccountDTOs(user, accountDTOs);
        return accountDTOs;
    }

    private void processFinanceAccountDTOs(User user, List<FinanceAccountDTO> accountDTOs) {
        Map<String, FinanceFX> fxs = new HashMap<String, FinanceFX>();
        for (FinanceAccountDTO account : accountDTOs) {
            account.setAccountType(FinanceAccountType.getValueString(account.getType()));
            if (!account.getCurrencyCode().equals(user.getLocalCurrency())) {
                // Switch currency
                FinanceFX fx = fxs.get(account.getCurrencyCode());
                if (fx == null) {
                    fx = fxService.getLatestFX(account.getCurrencyCode(), user.getLocalCurrency(), LocalDate.now());
                    if (fx != null) {
                        fxs.put(account.getCurrencyCode(), fx);
                    }
                }

                if (fx != null) {
                    account.setFxDateTime(fx.getDate());
                    account.setFxRateToLocal(fx.getRate());
                } else {
                    log.warn("Cannot find FX rate for account: " + account.getName() + ", ISO_CODE: " + account.getCurrencyCode());
                }
            }
        }
    }

    public List<FinanceAccountDTO> getAccountsOptimisedOld(User user) {
        StopWatch sw = new StopWatch();
        sw.start("getAccountsOptimised");
        List<FinanceAccount> accounts = this.accountRepository.findAllByUserGuidAndClosed(user.getGuid().toString(), false);
        List<FinanceAccountDTO> accountDTOs = financeAccountMapper.accountToAccountDTOs(accounts);
        for (FinanceAccountDTO account : accountDTOs) {
            BigDecimal balance = this.transactionService.getAccountBalanceForAccountUptoDate(account, null);
            account.setBalance(balance);
        }
        this.processFinanceAccountDTOs(user, accountDTOs);
        sw.stop();
        log.info(sw.prettyPrint());
        return accountDTOs;
    }

    /**
     *
     * @param user
     * @param includeInvestments
     * @param date
     * @param includeClosed
     * @return
     */
    public List<FinanceAccountDTO> getAccountsOptimised(User user, boolean includeInvestments, LocalDate date, boolean includeClosed) {
        if (user == null) {
            return null;
        }
        StopWatch sw = new StopWatch();
        sw.start("getAccountsOptimised");
        List<FinanceAccount> accounts = null;
        if (!includeClosed) {
            accounts = this.accountRepository.findAllByUserGuidAndClosed(user.getGuid().toString(), false);
        } else {
            if (date == null) {
                accounts = this.accountRepository.findAllByUserGuid(user.getGuid().toString());
            } else {
                accounts = this.accountRepository.findAllByUserGuidAndDateOpenedBefore(user.getGuid().toString(), date);
            }
        }
        List<FinanceAccountDTO> accountDTOs = financeAccountMapper.accountToAccountDTOs(accounts);

        Map<String, BigDecimal> transactionsSum = this.transactionService.sumTransactionsForAllAccounts(date);
        for (FinanceAccountDTO account : accountDTOs) {
            BigDecimal toAdd = BigDecimal.ZERO;
            if (transactionsSum != null) {
                toAdd = transactionsSum.get(account.getId().toString());
            } else {
                // No transactions in period.
            }
            // If there is a date the account opened and it is not before the date we are querying
            if (account.getDateOpened() == null || date == null || (date != null && !date.isBefore(account.getDateOpened()))) {
                if (toAdd != null) {
                    BigDecimal balance = account.getStartingBalance().add(toAdd);
                    account.setBalance(balance);
                } else {
                    account.setBalance(account.getStartingBalance());
                }
            } else {
                account.setNaBalance(true);
            }
            // account.setCurrentBalance(balance);
        }

        if (includeInvestments) {
            // Get all the security transactions
            List<FinanceSecurityInvestmentSummary> securityTransactions = null;

            securityTransactions = transactionService.getFinanceSecurityInvestmentTransactions(user, includeClosed, date);

            // Get the security prives for all the securities
            List<String> securityList = new ArrayList<String>();
            for (FinanceSecurityInvestmentSummary si : securityTransactions) {
                securityList.add(si.getSecurityId());
            }
            List<FinanceSecurityPrice> prices = securityService.getLatestBySecurityIds(securityList.toArray(new String[0]));
            Map<String, FinanceSecurityPrice> pricesById = new HashMap<String, FinanceSecurityPrice>();
            for (FinanceSecurityPrice sp : prices) {
                pricesById.put(sp.getSymbol().toString(), sp);
            }

            // Now add up how much it is all worth
            // MathContext m2 = new MathContext (2, RoundingMode.UP);
            // MathContext m4 = new MathContext (5, RoundingMode.DOWN);
            // NumberFormat formatter3 = new DecimalFormat("#0.000");
            NumberFormat formatter2 = new DecimalFormat("#0.00");
            for (FinanceAccountDTO a : accountDTOs) {
                if (a.getType() == FinanceAccountType.INVESTMENT.getValue()) {
                    // Collect securities in this account
                    BigDecimal tally = new BigDecimal(0);
                    // tally.setScale(4, RoundingMode.UP);
                    for (FinanceSecurityInvestmentSummary si : securityTransactions) {
                        log.info("Checking si: " + si.getAccountId() + "- for account: " + a.getId() + " (aka " + a.getName() + ")");
                        if (a.getId().toString().equals(si.getAccountId())) {
                            // Found Investment Txn for this account.
                            // log.info("Looking for Security ID: " + si.getSecurityId() + " (" +
                            // si.getName() + ")");
                            FinanceSecurityPrice price = pricesById.get(si.getSymbol());
                            if (price != null) {
                                log.info(
                                    "Security: {} is add {}, remove {}, price {}",
                                    price.getSymbol(),
                                    si.getAddSec(),
                                    si.getRemoveSec(),
                                    price.getPrice()
                                );
                                double total = 0;
                                BigDecimal value = new BigDecimal(0);
                                if (si.getAddSec() != null) {
                                    total += si.getAddSec();
                                    // String v = formatter3.format(si.getAddSec());
                                    // total += Double.parseDouble(v);//.round(new MathContext(0)).doubleValue();
                                }

                                if (si.getRemoveSec() != null) {
                                    total += si.getRemoveSec();
                                }
                                if (price.getPrice() != null) {
                                    value = price.getPrice().multiply(new BigDecimal(total));
                                    // double d = price.getPrice().doubleValue() * total;
                                    // value = new BigDecimal(d, m);
                                } else {
                                    log.warn("Price is null for symbol: " + price.getSymbol());
                                }

                                // Worth
                                log.info("Security: {} is worth {}", price.getSymbol(), value);
                                tally = tally.add(value);
                            } else {
                                log.warn("Couldn't find price for Security ID: " + si.getSecurityId() + " (" + si.getName() + ")");
                                if (a.getBalanceWarning() == null) {
                                    a.setBalanceWarning("Missing some or all price information");
                                }
                            }
                        }
                    }

                    String v = formatter2.format(tally);
                    BigDecimal bd = new BigDecimal(v);
                    log.info("Account: {} is worth {} ({}) ", a.getName(), v, bd);

                    a.setBalance(bd); // .round(m)
                }
            }
        }

        this.processFinanceAccountDTOs(user, accountDTOs);

        sw.stop();
        log.info(sw.prettyPrint());
        return accountDTOs;
    }

    /**
     *
     * @param user
     * @param accountId
     * @param includeClosed
     * @param date - Only for when the accountId == null at this stage. need to enhance code to handle date for account (not needed right now)
     * @return
     */
    public Collection<FinanceSecurityHoldingDTO> investmentAccountHoldings(
        User user,
        String accountId,
        boolean includeClosed,
        LocalDate date
    ) {
        // Get all the security transactions
        List<FinanceSecurityInvestmentSummary> securityTransactions = null;

        if (accountId != null && accountId.length() != 0) {
            securityTransactions = transactionService.getFinanceSecurityInvestmentTransactionsForAccount(user, accountId, includeClosed);
        } else {
            securityTransactions = transactionService.getFinanceSecurityInvestmentTransactions(user, includeClosed, date);
        }

        // Get the security prices for all the securities
        List<String> securityList = new ArrayList<String>();
        for (FinanceSecurityInvestmentSummary si : securityTransactions) {
            securityList.add(si.getSecurityId());
        }

        Map<String, FinanceSecurityHoldingDTO> holdings = new HashMap<String, FinanceSecurityHoldingDTO>();

        Map<String, FinanceSecurityPrice> pricesByCode = getLatestPrices(securityTransactions);

        for (FinanceSecurityInvestmentSummary si : securityTransactions) {
            FinanceSecurityHoldingDTO holding = holdings.get(si.getSymbol());
            if (holding == null) {
                holding = new FinanceSecurityHoldingDTO();
                holding.setId(si.getSecurityId());
                holding.setSymbol(si.getSymbol());
                holding.setUserSymbol(si.getUserSymbol());
                holding.setName(si.getName());
                holding.setSector(si.getSector());
                holding.setIndustry(si.getIndustry());
                holding.setExchangeName(si.getExchangeName());
                holding.setAccountId(accountId);
                holding.setCurrencyCode(si.getCurrencyCode());
                holding.setType(si.getSecurityType());
                if (si.getAccountId() != null) {
                    holding.setAccountId(si.getAccountId());
                    holding.setAccountName(si.getAccountName());
                }
                holdings.put(si.getSymbol(), holding);
            }

            if (si.getAddSec() != null) {
                holding.setQuantity(holding.getQuantity() + si.getAddSec());
            }

            if (si.getRemoveSec() != null) {
                holding.setQuantity(holding.getQuantity() + si.getRemoveSec()); // plus since already negative in DB result
            }

            FinanceSecurityPrice price = pricesByCode.get(holding.getSymbol());
            if (price != null) {
                holding.setPrice(price.getPrice());
                holding.setCurrencyCode(si.getCurrencyCode());
                holding.setValue(price.getPrice().multiply(new BigDecimal(holding.getQuantity())));
                holding.setPriceDateTime(price.getDate());
            }
        }

        Collection<FinanceSecurityHoldingDTO> list = holdings.values();
        processFinanceSecurityHoldingDTOs(user, list);

        return list;
    }

    private void processFinanceSecurityHoldingDTOs(User user, Collection<FinanceSecurityHoldingDTO> dtos) {
        Map<String, FinanceFX> fxs = new HashMap<String, FinanceFX>();
        for (FinanceSecurityHoldingDTO o : dtos) {
            if (o.getCurrencyCode() != null && !o.getCurrencyCode().equals(user.getLocalCurrency())) {
                // Switch currency
                FinanceFX fx = fxs.get(o.getCurrencyCode());
                if (fx == null) {
                    fx = fxService.getLatestFX(o.getCurrencyCode(), user.getLocalCurrency(), LocalDate.now());
                    if (fx != null) {
                        fxs.put(o.getCurrencyCode(), fx);
                    }
                }

                if (fx != null) {
                    o.setFxDateTime(fx.getDate());
                    o.setFxRateToLocal(fx.getRate());
                } else {
                    log.warn("Cannot find FX rate for account: " + o.getName() + ", ISO_CODE: " + o.getCurrencyCode());
                }
            }
        }
    }

    public Map<String, FinanceSecurityPrice> getLatestPrices(List<FinanceSecurityInvestmentSummary> securityTransactions) {
        // Get the security prives for all the securities
        List<String> securityList = new ArrayList<String>();
        for (FinanceSecurityInvestmentSummary si : securityTransactions) {
            // System.out.println("SI: " + si + ", ID: " + si.getSecurityId() + ", Account
            // Name: " + si.getAccountName() + ", accountID: " + si.getAccountId() + ",
            // Name: " + si.getName());
            securityList.add(si.getSecurityId());
        }
        List<FinanceSecurityPrice> prices = securityService.getLatestBySecurityIds(securityList.toArray(new String[0]));
        Map<String, FinanceSecurityPrice> pricesByCode = new HashMap<String, FinanceSecurityPrice>();
        for (FinanceSecurityPrice sp : prices) {
            pricesByCode.put(sp.getSymbol(), sp);
        }

        return pricesByCode;
    }

    public Page<FinanceTransaction> getTransactions(String accountId, Pageable pageable) {
        return this.transactionService.getTransactions(accountId, pageable);
    }

    public List<FinanceTransaction> getTransactionsNonPage(User user, String accountId) {
        StopWatch sw = new StopWatch();
        sw.start("getTransactionsNonPage");
        List<FinanceTransaction> res = this.transactionService.getTransactionsNonPage(user, accountId);
        sw.stop();
        log.info(sw.prettyPrint());
        return res;
    }

    public Page<FinanceTransactionRowDTO> getTransactionsPaging(User user, String accountId, Pageable pageable, String filterModel) {
        StopWatch sw = new StopWatch();
        sw.start("getTransactionsPaging");
        Page<FinanceTransactionRowDTO> res = this.transactionService.getTransactionsPaging(user, accountId, pageable, filterModel);
        sw.stop();
        log.info(sw.prettyPrint());
        return res;
    }

    // Account Indicators
    public void processBasicAccountValues(List<FinanceAccountDTO> accounts, List<FinanceIndicatorDTO> indicators) {
        for (FinanceAccountDTO account : accounts) {
            FinanceIndicatorDTO iDTO = new FinanceIndicatorDTO();
            iDTO.setId(account.getId().toString());
            iDTO.setName(account.getName());
            iDTO.setType("balance");
            FinanceSnapshotWithComparison financeSnapshotWithDelta = new FinanceSnapshotWithComparison(null);
            financeSnapshotWithDelta.setValue(account.getBalance());
            if (account.getFxRateToLocal() != null) {
                financeSnapshotWithDelta.setFxToLocal(account.getFxRateToLocal());
                // financeSnapshotWithDelta.setValue(account.getBalance().multiply(new BigDecimal(account.getFxRateToLocal())));
                financeSnapshotWithDelta.setCurrencyIsoCode(account.getCurrencyCode());
                if (account.getFxDateTime() != null) {
                    financeSnapshotWithDelta.setFxDate(account.getFxDateTime().toLocalDate());
                }
            }
            iDTO.setSnapshot(financeSnapshotWithDelta);
            indicators.add(iDTO);
        }
    }

    public void updateBasicAccountValuesWithDelta(List<FinanceAccountDTO> accounts, LocalDate date, List<FinanceIndicatorDTO> indicators) {
        for (FinanceAccountDTO account : accounts) {
            for (FinanceIndicatorDTO iDTO : indicators) {
                if (iDTO.getId().equals(account.getId().toString())) {
                    if (iDTO.getSnapshot() != null) {
                        iDTO.getSnapshot().setComparisonDate(date);

                        if (account.getBalance() != null) {
                            iDTO.getSnapshot().setComparisonValue(account.getBalance());
                        } else {
                            iDTO.getSnapshot().setComparisonValue(BigDecimal.ZERO);
                        }

                        if (account.getFxRateToLocal() != null) {
                            iDTO.getSnapshot().setComparisonFxToLocal(account.getFxRateToLocal());
                            //iDTO.getSnapshot().setComparisonValue(iDTO.getSnapshot().getComparisonValue().multiply(new BigDecimal(account.getFxRateToLocal())));
                            if (account.getFxDateTime() != null) {
                                iDTO.getSnapshot().setComparisonFxDate(account.getFxDateTime().toLocalDate());
                            }
                        }
                    } else {
                        log.warn(
                            "No original snapshot for: " + account.getId().toString() + "[" + account.getName() + "] to add delta to."
                        );
                    }
                    break;
                }
            }
        }
    }

    public void processAccountValuesByType(List<FinanceAccountDTO> accounts, List<FinanceIndicatorDTO> indicators) {
        for (FinanceAccountType type : FinanceAccountType.values()) {
            String typeStr = type.getValueString();
            BigDecimal typeValue = sumAccountValues(accounts, typeStr);
            FinanceIndicatorDTO iDTO = new FinanceIndicatorDTO();
            iDTO.setId("sumAccountType-" + typeStr);
            iDTO.setName(typeStr);
            iDTO.setType("sumAccountType");
            FinanceSnapshotWithComparison financeSnapshotWithDelta = new FinanceSnapshotWithComparison(null);
            financeSnapshotWithDelta.setValue(typeValue);
            iDTO.setSnapshot(financeSnapshotWithDelta);
            indicators.add(iDTO);
        }
    }

    public void updateAccountValuesByTypeWithDelta(List<FinanceAccountDTO> accounts, LocalDate date, List<FinanceIndicatorDTO> indicators) {
        for (FinanceAccountType type : FinanceAccountType.values()) {
            for (FinanceIndicatorDTO iDTO : indicators) {
                String typeStr = type.getValueString();
                if (iDTO.getId().equals("sumAccountType-" + typeStr)) {
                    BigDecimal typeValue = sumAccountValues(accounts, typeStr);
                    iDTO.getSnapshot().setComparisonDate(date);
                    if (typeValue != null) {
                        iDTO.getSnapshot().setComparisonValue(typeValue);
                    } else {
                        iDTO.getSnapshot().setComparisonValue(BigDecimal.ZERO);
                    }
                    break;
                }
            }
        }
    }

    public BigDecimal sumAccountValues(List<FinanceAccountDTO> accounts, String accountType) {
        BigDecimal value = BigDecimal.ZERO;
        for (FinanceAccountDTO account : accounts) {
            if (accountType.equals(account.getAccountType()) && account.getBalance() != null) {
                if (account.getFxRateToLocal() != null) {
                    value = value.add(account.getBalance().multiply(new BigDecimal(account.getFxRateToLocal())));
                } else {
                    value = value.add(account.getBalance());
                }
            }
        }
        return value;
    }

    // Monthly figures

    public Collection<FinanceSnapshotsPerResourceDTO> getAllAccountsMonthlyRunningBalance(User user, LocalDate start, LocalDate end) {
        StopWatch sw = new StopWatch();
        sw.start("getAllAccountsMonthlyRunningBalance");
        List<IFinanceMonthlySummary> summaries = this.transactionService.getAllAccountsMonthlyRunningBalance(user);

        // Copy so we can modify
        List<FinanceSnapshotsDTO> summaryDTOs = summaries
            .stream()
            .filter(Objects::nonNull)
            .map(this::iFinanceMonthlySummaryToFinanceMonthlySummaryDTO)
            .collect(Collectors.toList());

        List<FinanceAccount> accounts = this.accountRepository.findAllByUserGuid(user.getGuid().toString());

        // Need to adjust for accounts that have no transactions but have a starting balance. i.e. not in "summaries"
        Map<String, FinanceAccount> accountsWithNoTransactions = new HashMap<String, FinanceAccount>();
        Map<String, FinanceAccount> indexedAccounts = new HashMap<String, FinanceAccount>();

        // Add starting balance correction
        for (FinanceAccount account : accounts) {
            indexedAccounts.put(account.getId().toString(), account);

            boolean foundOpeningMonthValue = false;
            LocalDate endOfMonthAfterOpen = null;
            if (account.getDateOpened() == null) {
                foundOpeningMonthValue = true; // not needed so just set to true automatically.
            } else {
                endOfMonthAfterOpen = LocalDate.of(account.getDateOpened().getYear(), account.getDateOpened().getMonth(), 1).with(
                    TemporalAdjusters.lastDayOfMonth()
                );
            }
            if (account.getStartingBalance() != null && account.getStartingBalance().compareTo(BigDecimal.ZERO) != 0) {
                for (FinanceSnapshotsDTO summary : summaryDTOs) {
                    accountsWithNoTransactions.remove(summary.getAccountId());
                    //log.info("Summary: " + summary.getAccountId() + " [" + summary.getY() + ", " + summary.getM() + "]: " + summary.getRunningBalance() );
                    if (account.getId().toString().equals(summary.getAccountId())) {
                        if (
                            endOfMonthAfterOpen != null &&
                            (summary.getDate().isEqual(endOfMonthAfterOpen) || summary.getDate().isAfter(endOfMonthAfterOpen))
                        ) {
                            summary.setRunningBalance(summary.getRunningBalance().add(account.getStartingBalance()));
                        } else {
                            log.debug(
                                "Not adjusting with starting balance for months before the opening of the account: " +
                                    account.getId().toString()
                            );
                        }

                        if (endOfMonthAfterOpen != null && summary.getDate().equals(endOfMonthAfterOpen)) {
                            foundOpeningMonthValue = true;
                        }
                    }
                }
            }

            if (!foundOpeningMonthValue && endOfMonthAfterOpen != null) {
                FinanceSnapshotsDTO isnapshot = createFinanceSnapshotDTO(user, account, endOfMonthAfterOpen);

                summaryDTOs.add(isnapshot);
            }
        }
        LocalDate endOfMonthForEnd = end.with(TemporalAdjusters.lastDayOfMonth());
        if (accountsWithNoTransactions.size() > 0) {
            log.info("The following accounts where not catered for (i.e. have no transactions so not in the summaries):");
            for (FinanceAccount account : accountsWithNoTransactions.values()) {
                log.info(
                    "  : " +
                        account.getId().toString() +
                        ". Starting: " +
                        account.getStartingBalance() +
                        " needs to be added into the summaries to have its balance handled properly."
                );
                LocalDate endOfMonthAfterOpen = LocalDate.of(account.getDateOpened().getYear(), account.getDateOpened().getMonth(), 1).with(
                    TemporalAdjusters.lastDayOfMonth()
                );
                FinanceSnapshotsDTO isnapshot = createFinanceSnapshotDTO(user, account, endOfMonthAfterOpen);
                summaryDTOs.add(isnapshot);
            }
        }

        // Zero Closed Accounts after closed date.
        Map<String, FinanceAccount> closedAccounts = new HashMap<String, FinanceAccount>();
        for (FinanceAccount account : accounts) {
            if (account.isClosed()) {
                closedAccounts.put(account.getId().toString(), account);
            }
        }

        Collection<FinanceSnapshotsPerResourceDTO> resourceSnapshotDTOs = filterAndFillOutMonthlyBalanceSummaries(
            user,
            summaryDTOs,
            start,
            endOfMonthForEnd,
            closedAccounts
        );
        //this.processFinanceAccountDTOs(user, accountDTOs);

        addAccountNames(indexedAccounts, resourceSnapshotDTOs);
        addFXDetails(user, resourceSnapshotDTOs);

        resourceSnapshotDTOs = processAccountRunningBalanceByType(accounts, resourceSnapshotDTOs);
        sw.stop();
        log.info(sw.prettyPrint());
        return resourceSnapshotDTOs;
    }

    private void addAccountNames(
        Map<String, FinanceAccount> indexedAccounts,
        Collection<FinanceSnapshotsPerResourceDTO> resourceSnapshotDTOs
    ) {
        for (FinanceSnapshotsPerResourceDTO resourceSnapshotDTO : resourceSnapshotDTOs) {
            //if(resourceSnapshotDTO.getName() == null) {
            FinanceAccount a = indexedAccounts.get(resourceSnapshotDTO.getId());
            if (a != null) {
                resourceSnapshotDTO.setName(a.getName());
                resourceSnapshotDTO.setCurrencyCode(a.getCurrencyCode());
            }
            //}
        }
    }

    private void addFXDetails(User user, Collection<FinanceSnapshotsPerResourceDTO> resourceSnapshotDTOs) {
        for (FinanceSnapshotsPerResourceDTO resourceSnapshotDTO : resourceSnapshotDTOs) {
            if (resourceSnapshotDTO.getSnapshots() != null && resourceSnapshotDTO.getSnapshots().size() > 0) {
                // Check the snapshots which should be consistent on what currency they are in (i.e. cannot change)
                // FinanceSnapshot s = resourceSnapshotDTO.getSnapshots().get(0);
                if (
                    resourceSnapshotDTO.getCurrencyCode() != null &&
                    user.getLocalCurrency() != null &&
                    !user.getLocalCurrency().equals(resourceSnapshotDTO.getCurrencyCode())
                ) {
                    // If currency code is not the local one (or not set which implies local)
                    this.fxService.addFXDetailsToMonthlySnapshots(
                        user,
                        resourceSnapshotDTO.getCurrencyCode(),
                        resourceSnapshotDTO.getSnapshots()
                    );
                }
            }
        }
    }

    private Collection<FinanceSnapshotsPerResourceDTO> filterAndFillOutMonthlyBalanceSummaries(
        User user,
        List<FinanceSnapshotsDTO> input,
        LocalDate start,
        LocalDate end,
        Map<String, FinanceAccount> closedAccounts
    ) {
        //List<FinanceIndicatorDTO> newSummaries = new ArrayList<FinanceIndicatorDTO>();
        Map<String, FinanceSnapshotsPerResourceDTO> mapedSummaries = new HashMap<String, FinanceSnapshotsPerResourceDTO>();

        Map<String, FinanceSnapshot> periodStartBalance = new HashMap<String, FinanceSnapshot>();

        for (FinanceSnapshotsDTO summary : input) {
            if (
                summary.getDate() == null ||
                (start != null && summary.getDate().isBefore(start)) ||
                (end != null && summary.getDate().isAfter(end))
            ) {
                // Track the balance prior to the period start, so that a snapshot can be stored at the begining of the period
                FinanceSnapshot s = periodStartBalance.get(summary.getAccountId());
                if (s == null || (s != null && s.getDate().isBefore(summary.getDate()))) {
                    s = new FinanceSnapshot(summary.getDate(), summary.getRunningBalance());
                    if (
                        summary.getCurrencyCode() != null &&
                        user.getLocalCurrency() != null &&
                        !user.getLocalCurrency().equals(summary.getCurrencyCode())
                    ) {
                        s.setCurrencyIsoCode(summary.getCurrencyCode());
                        s.setFxToLocal(summary.getRateToBase());
                    }
                    periodStartBalance.put(summary.getAccountId(), s);
                }
            } else {
                String accountId = summary.getAccountId();
                if (accountId == null) {
                    accountId = "<NOT_FOUND>";
                }
                FinanceSnapshotsPerResourceDTO dto = mapedSummaries.get(accountId);
                if (dto == null) {
                    dto = new FinanceSnapshotsPerResourceDTO();
                    dto.setId(summary.getAccountId());
                    dto.setName(summary.getAccountName());
                    dto.setCurrencyCode(summary.getCurrencyCode());
                    mapedSummaries.put(summary.getAccountId(), dto);
                }

                FinanceSnapshot s = new FinanceSnapshot(summary.getDate(), summary.getRunningBalance());
                if (
                    summary.getCurrencyCode() != null &&
                    user.getLocalCurrency() != null &&
                    !user.getLocalCurrency().equals(summary.getCurrencyCode())
                ) {
                    s.setCurrencyIsoCode(summary.getCurrencyCode());
                    s.setFxToLocal(summary.getRateToBase());
                }

                dto.addSnapshot(s);
                // log.info("1* " + summary.getAccountId() + "["+ summary.getAccountName() + "]," + s.getDate() + ":$" +s.getValue());
            }
        }

        // Save period start values
        // First check if there is a period start balance that we need to store
        if (start != null) {
            for (String accountId : periodStartBalance.keySet()) {
                FinanceSnapshot s = periodStartBalance.get(accountId);
                // Check if s > 0 and for a closed accont, before the start of the period. If so, set it to Zero.
                // NOTE: this was stopping close asset account from having its balance carried forward properly (i.e. closed 2018, last txn in 2008, and looking for period in 2013 - so effectively "CLOSED" but shouldn't be zeroed. Therefore changed to "isAfter" but not sure that can happen at all.)
                if (closedAccounts.get(accountId) != null && s.getValue().doubleValue() != 0.0 && s.getDate().isAfter(start)) {
                    s.setValue(BigDecimal.ZERO);
                }

                FinanceSnapshotsPerResourceDTO dto = mapedSummaries.get(accountId);

                boolean found = false;
                LocalDate endOfStartMonth = LocalDate.of(start.getYear(), start.getMonth(), 1).with(TemporalAdjusters.lastDayOfMonth());

                if (dto != null && s != null) {
                    // Check not already in the list
                    for (FinanceSnapshot existingSnap : dto.getSnapshots()) {
                        if (existingSnap.getDate().isEqual(endOfStartMonth)) {
                            found = true;
                            break;
                        }
                    }
                }

                if (dto == null) {
                    dto = new FinanceSnapshotsPerResourceDTO();
                    dto.setId(accountId);
                    mapedSummaries.put(accountId, dto);
                }

                if (s != null && !found) {
                    s.setDate(endOfStartMonth);
                    dto.addSnapshot(s);
                    // log.info("2* " + accountId + "[NONE]," + s.getDate() + ":$" +s.getValue());
                }
            }
        }

        Collection<FinanceSnapshotsPerResourceDTO> result = mapedSummaries.values();
        filloutMonthlyGaps(user, result, start, end, Period.ofMonths(1), closedAccounts);

        return result;
    }

    private FinanceSnapshotsDTO createFinanceSnapshotDTO(User user, FinanceAccount account, LocalDate date) {
        FinanceSnapshotsDTO isnapshot = new FinanceSnapshotsDTO();
        isnapshot.setAccountId(account.getId().toString());
        isnapshot.setAccountName(account.getName());
        isnapshot.setDate(date);
        isnapshot.setCurrencyCode(account.getCurrencyCode());
        isnapshot.setRunningBalance(account.getStartingBalance());

        return isnapshot;
    }

    private void filloutMonthlyGaps(
        User user,
        Collection<FinanceSnapshotsPerResourceDTO> snapshotsPerResource,
        LocalDate start,
        LocalDate end,
        Period period,
        Map<String, FinanceAccount> closedAccounts
    ) {
        sortResourceSnapshots(snapshotsPerResource);

        // Move start to end of month
        if (start != null) {
            start = LocalDate.of(start.getYear(), start.getMonth(), 1).with(TemporalAdjusters.lastDayOfMonth());
        }

        for (FinanceSnapshotsPerResourceDTO s : snapshotsPerResource) {
            // If start == null that means need to acertain from the initial value.
            LocalDate currentPeriodStart = start;
            BigDecimal currentValue = BigDecimal.ZERO;
            List<FinanceSnapshot> toAdd = new ArrayList<FinanceSnapshot>();
            Double lastRateToBase = null;

            for (FinanceSnapshot snapshot : s.getSnapshots()) {
                if (currentPeriodStart == null) {
                    // If currentPeriodStart == null that means need to acertain from the initial value.
                    currentPeriodStart = snapshot.getDate();
                }

                if (snapshot.getDate() != null && snapshot.getDate().compareTo(currentPeriodStart) <= 0) {
                    currentValue = snapshot.getValue();
                    currentPeriodStart = currentPeriodStart.plus(period);
                    currentPeriodStart = LocalDate.of(currentPeriodStart.getYear(), currentPeriodStart.getMonth(), 1).with(
                        TemporalAdjusters.lastDayOfMonth()
                    );
                } else if (snapshot.getDate() != null && snapshot.getDate().isAfter(currentPeriodStart)) {
                    // Fill some out
                    while (currentPeriodStart.compareTo(end) <= 0 && snapshot.getDate().isAfter(currentPeriodStart)) {
                        FinanceSnapshot newSnap = new FinanceSnapshot(currentPeriodStart, currentValue);
                        if (
                            snapshot.getCurrencyIsoCode() != null &&
                            user.getLocalCurrency() != null &&
                            !user.getLocalCurrency().equals(snapshot.getCurrencyIsoCode())
                        ) {
                            newSnap.setCurrencyIsoCode(snapshot.getCurrencyIsoCode());
                            newSnap.setFxToLocal(lastRateToBase);
                        }

                        toAdd.add(newSnap);
                        // log.info("3a* " + s.getResourceId() + "["+ s.getResourceName() + "]," + newSnap.getDate() + ":$" +newSnap.getValue());
                        currentPeriodStart = currentPeriodStart.plus(period);
                        currentPeriodStart = LocalDate.of(currentPeriodStart.getYear(), currentPeriodStart.getMonth(), 1).with(
                            TemporalAdjusters.lastDayOfMonth()
                        );
                    }
                    currentValue = snapshot.getValue();
                }
                if (snapshot.getFxToLocal() != null) {
                    lastRateToBase = snapshot.getFxToLocal();
                }
            }

            // Check if we are missing between the last snapshot at the end of the period
            while (currentPeriodStart != null && currentPeriodStart.compareTo(end) <= 0) {
                BigDecimal v = currentValue;
                if (closedAccounts.get(s.getId()) != null) {
                    v = BigDecimal.ZERO;
                }

                FinanceSnapshot newSnap = new FinanceSnapshot(currentPeriodStart, v);
                toAdd.add(newSnap);
                // log.info("3b* " + s.getResourceId() + "["+ s.getResourceName() + "]," + newSnap.getDate() + ":$" +newSnap.getValue());
                currentPeriodStart = currentPeriodStart.plus(period);
                currentPeriodStart = LocalDate.of(currentPeriodStart.getYear(), currentPeriodStart.getMonth(), 1).with(
                    TemporalAdjusters.lastDayOfMonth()
                );
            }
            s.getSnapshots().addAll(toAdd);
        }
        sortResourceSnapshots(snapshotsPerResource);

        // Remove duplicates
        for (FinanceSnapshotsPerResourceDTO s : snapshotsPerResource) {
            List<FinanceSnapshot> toRemove = new ArrayList<FinanceSnapshot>();
            FinanceSnapshot prevSnap = null;
            for (FinanceSnapshot snap : s.getSnapshots()) {
                if (prevSnap != null && prevSnap.getDate().equals(snap.getDate())) {
                    toRemove.add(snap);
                    // log.debug("Removing: " + s.getResourceId() + "["+ s.getResourceName() +"], Snapshot date: " + snap.getDate() + "=$" + snap.getValue() + ", whereas the previous snap " + prevSnap.getDate() + "=$"+ prevSnap.getValue() + ".");
                    // Double check value is same
                    if (!prevSnap.getValue().equals(snap.getValue())) {
                        log.warn(
                            "Removing what looks to be a duplicate snapshot but different balance: " +
                                s.getId() +
                                "[" +
                                s.getName() +
                                "], Snapshot date: " +
                                snap.getDate() +
                                "=$" +
                                snap.getValue() +
                                ", whereas the previous snap " +
                                prevSnap.getDate() +
                                "=$" +
                                prevSnap.getValue() +
                                "."
                        );
                    }
                }
                prevSnap = snap;
            }

            s.getSnapshots().removeAll(toRemove);
        }
    }

    public List<FinanceSnapshotsPerResourceDTO> processAccountRunningBalanceByType(
        List<FinanceAccount> accounts,
        Collection<FinanceSnapshotsPerResourceDTO> resourceSnapshots
    ) {
        Map<String, FinanceSnapshotsPerResourceDTO> indexedResourceSnapshots = this.indexResourceSnapshotsByAccountId(resourceSnapshots);
        Map<String, FinanceSnapshotsPerResourceDTO> indexByType = new HashMap<String, FinanceSnapshotsPerResourceDTO>();
        for (FinanceAccountType type : FinanceAccountType.values()) {
            if (type != FinanceAccountType.INVESTMENT) {
                String typeStr = type.getValueString();
                FinanceSnapshotsPerResourceDTO typeResourceSnapshot = indexByType.get(typeStr);

                if (typeResourceSnapshot == null) {
                    typeResourceSnapshot = new FinanceSnapshotsPerResourceDTO();
                    typeResourceSnapshot.setId("sumAccountType-" + typeStr);
                    typeResourceSnapshot.setName(typeStr);
                    typeResourceSnapshot.setType("sumAccountType");
                    indexByType.put(typeStr, typeResourceSnapshot);
                }

                for (FinanceAccount account : accounts) {
                    if (
                        type.getValue() == account.getType() &&
                        (type.getValue() != FinanceAccountType.BANKING.getValue() ||
                            (type.getValue() == FinanceAccountType.BANKING.getValue() && account.getRelatedToAccountId() == null))
                    ) {
                        FinanceSnapshotsPerResourceDTO rDTO = indexedResourceSnapshots.get(account.getId().toString());
                        if (rDTO == null) {
                            log.warn("Couldn't find rDTO for account: " + account.getName() + "[" + account.getId().toString() + "]");
                        } else {
                            //if(type.getValue() == FinanceAccountType.BANKING.getValue()) {
                            FinanceSnapshot s = null;
                            if (rDTO.getSnapshots() != null && rDTO.getSnapshots().size() > 0) {
                                s = rDTO.getSnapshots().get(rDTO.getSnapshots().size() - 1);
                            }
                            if (s != null) {
                                log.info(
                                    "Adding account: " +
                                        account.getName() +
                                        "[" +
                                        account.getId().toString() +
                                        "] to sumAccountType-" +
                                        account.getType() +
                                        ": " +
                                        s.getValue() +
                                        " > " +
                                        s.getFxToLocal()
                                );
                            }
                            //}
                            addToExistingSnapshots(typeResourceSnapshot, rDTO.getSnapshots());
                        }
                    }
                }
            }
        }

        Collection<FinanceSnapshotsPerResourceDTO> newResourceSnapshots = indexByType.values();
        List<FinanceSnapshotsPerResourceDTO> result = new ArrayList<FinanceSnapshotsPerResourceDTO>();
        for (FinanceSnapshotsPerResourceDTO a : resourceSnapshots) {
            result.add(a);
        }
        result.addAll(newResourceSnapshots);
        return result;
    }

    private void addToExistingSnapshots(FinanceSnapshotsPerResourceDTO typeResourceSnapshot, List<FinanceSnapshot> snapshotsToAdd) {
        List<FinanceSnapshot> currentSnapshots = typeResourceSnapshot.getSnapshots();
        if (currentSnapshots == null) {
            currentSnapshots = new ArrayList<FinanceSnapshot>();
            currentSnapshots = new ArrayList<FinanceSnapshot>();
            typeResourceSnapshot.setSnapshots(currentSnapshots);
        }

        for (FinanceSnapshot snap : snapshotsToAdd) {
            findAndAdd(currentSnapshots, snap);
        }
    }

    private void findAndAdd(List<FinanceSnapshot> currentSnapshots, FinanceSnapshot snapshotToAdd) {
        boolean found = false;
        for (FinanceSnapshot existingSnap : currentSnapshots) {
            if (existingSnap.getDate().isEqual(snapshotToAdd.getDate())) {
                found = true;
                if (snapshotToAdd.getFxToLocal() == null) {
                    existingSnap.setValue(existingSnap.getValue().add(snapshotToAdd.getValue()));
                } else {
                    existingSnap.setValue(
                        existingSnap.getValue().add(snapshotToAdd.getValue().multiply(BigDecimal.valueOf(snapshotToAdd.getFxToLocal())))
                    );
                }
            }
        }
        if (!found) {
            FinanceSnapshot nSnap = new FinanceSnapshot(snapshotToAdd.getDate(), snapshotToAdd.getValue());
            currentSnapshots.add(nSnap);
        }
    }

    private void sortResourceSnapshots(Collection<FinanceSnapshotsPerResourceDTO> snapshotsPerResource) {
        for (FinanceSnapshotsPerResourceDTO s : snapshotsPerResource) {
            Collections.sort(
                s.getSnapshots(),
                new Comparator<FinanceSnapshot>() {
                    @Override
                    public int compare(FinanceSnapshot o1, FinanceSnapshot o2) {
                        return o1.getDate().compareTo(o2.getDate());
                    }
                }
            );
        }
    }

    public FinanceSnapshotsDTO iFinanceMonthlySummaryToFinanceMonthlySummaryDTO(IFinanceMonthlySummary summary) {
        return new FinanceSnapshotsDTO(summary);
    }

    public Map<String, FinanceSnapshotsPerResourceDTO> indexResourceSnapshotsByAccountId(
        Collection<FinanceSnapshotsPerResourceDTO> resourceSnapshots
    ) {
        Map<String, FinanceSnapshotsPerResourceDTO> indexed = new HashMap<String, FinanceSnapshotsPerResourceDTO>(resourceSnapshots.size());
        for (FinanceSnapshotsPerResourceDTO dto : resourceSnapshots) {
            indexed.put(dto.getId(), dto);
        }
        return indexed;
    }

    //
    // Income & Expenses
    //
    public List<FinanceAccountDTO> getCategorisedIncomeAndExpenses(User user, LocalDate fromDate, LocalDate toDate) {
        List<IFinanceCategorisedIncomeExpenses> incexps = this.accountRepository.findIncomeExpensesForPeriod(fromDate, toDate);
        return null;
    }
}
