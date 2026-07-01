package ovaro.plat4m.service;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.Table;
import com.le.sunriise.accountviewer.AccountUtil;
import com.le.sunriise.accountviewer.MnyContext;
import com.le.sunriise.currency.FX;
import com.le.sunriise.currency.FXUtil;
import com.le.sunriise.mnyobject.Currency;
import com.le.sunriise.mnyobject.FinancialInstitution;
import com.le.sunriise.mnyobject.InvestmentTransaction;
import com.le.sunriise.mnyobject.Security;
import com.le.sunriise.mnyobject.TransactionSplit;
import com.le.sunriise.mnyobject.TransferLink;
import com.le.sunriise.mnyobject.impl.CategoryImplUtil;
import com.le.sunriise.mnyobject.impl.CurrencyImplUtil;
import com.le.sunriise.mnyobject.impl.FinancialInstitutionImplUtil;
import com.le.sunriise.mnyobject.impl.InvestmentTransactionImplUtil;
import com.le.sunriise.mnyobject.impl.PayeeImplUtil;
import com.le.sunriise.mnyobject.impl.SecurityImplUtil;
import com.le.sunriise.mnyobject.impl.TransactionImplUtil;
import com.le.sunriise.mnyobject.impl.TransferLinkImplUtil;
import com.le.sunriise.viewer.OpenedDb;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinanceCurrency;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.domain.FinanceImportStatus;
import ovaro.plat4m.domain.FinanceInstitution;
import ovaro.plat4m.domain.FinancePayee;
import ovaro.plat4m.domain.FinanceSecurity;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.FinanceSecurityType;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.FinanceTransferLink;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.Source;
import ovaro.plat4m.domain.SourceLink;
import ovaro.plat4m.domain.SourceType;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceCategoryRepository;
import ovaro.plat4m.repository.FinanceCurrencyRepository;
import ovaro.plat4m.repository.FinanceFXRepository;
import ovaro.plat4m.repository.FinanceInstitutionRepository;
import ovaro.plat4m.repository.FinancePayeeRepository;
import ovaro.plat4m.repository.FinanceSecurityPriceRepository;
import ovaro.plat4m.repository.FinanceSecurityRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.FinanceTransferLinkRepository;
import ovaro.plat4m.repository.FinanceUserSecurityRepository;
import ovaro.plat4m.repository.SourceLinkRepository;
import ovaro.plat4m.repository.SourceRepository;
import ovaro.plat4m.service.dto.MsMoneyImportDTO;

@Service
public class MsMoneyService {

    private static final int LOAN_ACCOUNT_TYPE = 6;
    private static final String LOAN_TRANSFER_SUFFIX = "loan-transfer";
    private static final String LOAN_INTEREST_SUFFIX = "loan-interest";

    private final Logger log = LoggerFactory.getLogger(MsMoneyService.class);
    private FinanceAccountRepository accountRepository;
    private SourceRepository sourceRepository;
    private SourceLinkRepository sourceLinkRepository;
    private FinanceTransactionRepository transactionRepository;
    private FinancePayeeRepository payeeRepository;
    private FinanceCategoryRepository categoryRepository;
    private FinanceCurrencyRepository currencyRepository;
    private FinanceFXRepository fxRepository;
    private FinanceUserSecurityRepository userSecurityRepository;
    private FinanceSecurityRepository securityRepository;
    private FinanceSecurityPriceRepository securityPriceRepository;
    private FinanceInstitutionRepository fiRepository;
    private FinanceTransferLinkRepository transferLinkRepository;

    private FinanceSecurityService securityService;
    private FinanceTransactionEditorLookupCacheService editorLookupCacheService;

    public static final String TABLE_TRN = "TRN";
    public static final String TABLE_ACCOUNT = "ACCT";
    public static final String TABLE_PAYEE = "PAY";
    public static final String TABLE_CATEGORY = "CAT";
    public static final String TABLE_CURRENCY = "CRNC";
    public static final String TABLE_FX = "CRNC_EXCHG";
    public static final String TABLE_SECURITY = "SEC";
    public static final String TABLE_SECURITY_PRICE = "SP";
    public static final String TABLE_FI = "FI";
    public static final String TABLE_XFER = "fin_xfer";

    // STOMP for import status websocket
    private final SimpMessageSendingOperations messagingTemplate;

    private static boolean FETCH_SECUIRTY_INFO_FROM_WEB = false;

    public MsMoneyService(
        FinanceAccountRepository accountRepository,
        FinanceTransactionRepository transactionRepository,
        SourceRepository sourceRepository,
        SourceLinkRepository sourceLinkRepository,
        FinancePayeeRepository payeeRepository,
        FinanceCategoryRepository categoryRepository,
        FinanceCurrencyRepository currencyRepository,
        FinanceFXRepository fxRepository,
        FinanceUserSecurityRepository userSecurityRepository,
        FinanceSecurityRepository securityRepository,
        FinanceSecurityPriceRepository securityPriceRepository,
        FinanceInstitutionRepository fiRepository,
        FinanceTransferLinkRepository transferLinkRepository,
        FinanceSecurityService securityService,
        FinanceTransactionEditorLookupCacheService editorLookupCacheService,
        SimpMessageSendingOperations messagingTemplate
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.sourceRepository = sourceRepository;
        this.sourceLinkRepository = sourceLinkRepository;
        this.categoryRepository = categoryRepository;
        this.payeeRepository = payeeRepository;
        this.currencyRepository = currencyRepository;
        this.fxRepository = fxRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.securityRepository = securityRepository;
        this.securityPriceRepository = securityPriceRepository;
        this.fiRepository = fiRepository;
        this.transferLinkRepository = transferLinkRepository;
        this.securityService = securityService;
        this.editorLookupCacheService = editorLookupCacheService;
        this.messagingTemplate = messagingTemplate;
    }

    //@Transactional
    @Async
    public CompletableFuture<MsMoneyImportDTO> startImport(User user, String moneyFile, String password, MsMoneyImportDTO monitor)
        throws FileNotFoundException, IOException {
        StopWatch sw = new StopWatch();
        sw.setKeepTaskList(true);
        monitor.setCurrentTask("Start");
        File mdbFile = new File(moneyFile);
        if (!mdbFile.exists()) {
            log.warn("importMny: " + moneyFile + " - NOT FOUND");
            throw new FileNotFoundException();
        }
        log.info("startImport: " + moneyFile + " password: " + (password != null ? "PROVIDED" : "NOT PROVIDED"));
        if (password == null) {
            password = "";
        }
        OpenedDb oDB = null;
        FinanceImportStatus fis = null;
        try {
            oDB = com.le.sunriise.Utils.openDb(mdbFile, password);
            MnyContext mnyContext = new MnyContext();
            ZonedDateTime syncStart = ZonedDateTime.now();
            Source source = getSource(user, oDB.getDbFile().getName(), true);

            //
            // Currency Sync
            //
            monitor.setCurrentTask("Sync Currencies");
            fis = syncCurrencies(sw, user, oDB, mnyContext, source);
            processImportStatusEvent(user, fis);
            Map<Integer, Currency> currencies = CurrencyImplUtil.getCurrencies(oDB.getDb());

            //
            // FX Sync
            //
            monitor.setCurrentTask("Sync FX");
            fis = syncFX(sw, oDB, mnyContext, source, currencies);
            processImportStatusEvent(user, fis);

            //
            // Security Sync
            //
            monitor.setCurrentTask("Sync Securities");
            fis = syncUserSecurities(sw, user, oDB, mnyContext, source, currencies);
            processImportStatusEvent(user, fis);

            //
            // Security Prices Sync
            //
            Map<String, SourceLink> securitySourceLinks = getSourceLinksForType(user, source, TABLE_SECURITY);
            monitor.setCurrentTask("Sync Security Prices");
            fis = syncSecurityPrices(sw, user, oDB, mnyContext, source, securitySourceLinks);
            processImportStatusEvent(user, fis);

            //
            // Institutions Sync
            //
            monitor.setCurrentTask("Sync Institutions (FIs)");
            fis = syncInstitutions(sw, user, oDB, mnyContext, source);
            processImportStatusEvent(user, fis);

            //
            // Accounts Sync
            //
            Map<String, SourceLink> fiSourceLinks = getSourceLinksForType(user, source, TABLE_FI);
            monitor.setCurrentTask("Sync Accounts");
            fis = syncAccounts(sw, user, oDB, mnyContext, source, currencies, fiSourceLinks);
            Map<String, SourceLink> accountSourceLinks = getSourceLinksForType(user, source, TABLE_ACCOUNT);
            fixRelatedToAccounts(sw, user, accountSourceLinks);
            processImportStatusEvent(user, fis);

            //
            // Categories Sync
            //
            monitor.setCurrentTask("Sync Categories");
            fis = syncCategories(sw, user, oDB, mnyContext, source);
            processImportStatusEvent(user, fis);

            //
            // Payee Sync
            //
            monitor.setCurrentTask("Sync Payees");
            fis = syncPayees(sw, user, oDB, mnyContext, source);
            processImportStatusEvent(user, fis);

            //
            // Transaction Sync
            //
            // TODO - This is memory intensive. Could optimise this in the future if needed.
            Map<String, SourceLink> payeeSourceLinks = getSourceLinksForType(user, source, TABLE_PAYEE);
            Map<String, SourceLink> categoriesSourceLinks = getSourceLinksForType(user, source, TABLE_CATEGORY);
            monitor.setCurrentTask("Sync Transactions");
            fis = syncTransactions(
                sw,
                user,
                oDB,
                mnyContext,
                source,
                accountSourceLinks,
                payeeSourceLinks,
                categoriesSourceLinks,
                securitySourceLinks,
                currencies
            );
            removeDeletedTransactions(sw, user, fis, oDB, source);
            processImportStatusEvent(user, fis);

            // Update last sync time
            source.setLastSyncDateTime(syncStart);
            sourceRepository.save(source);

            // Clear Finance Investment Events so they are regenerated (i.e. if data changed this is brute force to catch)
            invalidateCachedFinancialEvents(user);
        } catch (Exception e) {
            log.error("Import failed while processing file {}", moneyFile, e);
            monitor.setCurrentTask("Failed");

            fis = new FinanceImportStatus();
            fis.setTaskName(monitor.getCurrentTask());
            fis.setTaskFinished(true);
            fis.setImportFinished(true);
            fis.setDuration(sw.getTotalTimeMillis());
            fis.setError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            processImportStatusEvent(user, fis);

            return CompletableFuture.failedFuture(e);
        } finally {
            if (oDB != null) {
                oDB.close();
            }
            log.info("Closed DB");
        }
        log.info(sw.prettyPrint());
        monitor.setCurrentTask("Done");

        //
        // Send Import Complete Message
        //
        fis = new FinanceImportStatus();
        fis.setImportFinished(true);
        fis.setTaskName(monitor.getCurrentTask());
        fis.setDuration(sw.getTotalTimeMillis());
        processImportStatusEvent(user, fis);

        return CompletableFuture.completedFuture(monitor);
    }

    private void processImportStatusEvent(User user, FinanceImportStatus fis) {
        log.info(
            "Sending import status to user {}: taskName='{}', importFinished={}, taskFinished={}, error='{}'",
            user.getLogin(),
            fis.getTaskName(),
            fis.isImportFinished(),
            fis.isTaskFinished(),
            fis.getError()
        );
        messagingTemplate.convertAndSendToUser(user.getLogin(), "/queue/import", fis);
    }

    private Source getSource(User user, String name, boolean useTypeOnly) {
        log.debug("Finding Source...");
        Optional<Source> source = null;
        if (!useTypeOnly) {
            source = this.sourceRepository.findByUserGuidAndTypeIdAndName(user.getGuid().toString(), SourceType.MS_MONEY.getValue(), name);
        } else {
            List<Source> sources = this.sourceRepository.findByUserGuidAndTypeId(user.getGuid().toString(), SourceType.MS_MONEY.getValue());
            if (sources != null && sources.size() > 0) {
                source = Optional.of(sources.get(0));
            } else {
                source = Optional.empty();
            }
        }
        log.debug("Found Source: " + source.isPresent());

        Source s = null;
        if (source.isPresent()) {
            s = source.get();
        } else {
            Source mny = new Source();
            mny.setTypeId(SourceType.MS_MONEY.getValue());
            mny.setTypeName("Microsoft Money");
            mny.setName(name);
            mny.setUserGuid(user.getGuid().toString());

            s = sourceRepository.save(mny);
            log.info("Creted new Source for 'Microsoft Money'. ID: " + s.getId());
        }
        return s;
    }

    public FinanceImportStatus syncFX(StopWatch sw, OpenedDb oDB, MnyContext mnyContext, Source source, Map<Integer, Currency> currencies)
        throws IOException {
        List<FX> fxs = FXUtil.getFX(oDB.getDb());

        sw.start("syncFX");
        FinanceImportStatus fis = new FinanceImportStatus();
        fis.setTaskName("Sync FX");

        for (FX fx : fxs) {
            fis.setNumInput(fis.getNumInput() + 1);

            if (fx.getFromCurrencyId() != null && fx.getToCurrencyId() != null) {
                Currency from = currencies.get(fx.getFromCurrencyId());
                Currency to = currencies.get(fx.getToCurrencyId());
                if (from != null && to != null) {
                    FinanceFX financeFX = new FinanceFX();
                    financeFX.setDate(ZonedDateTime.ofInstant(fx.getDate().toInstant(), ZoneId.systemDefault()));
                    financeFX.setFromIsoCode(from.getIsoCode());
                    financeFX.setToIsoCode(to.getIsoCode());
                    financeFX.setRate(fx.getRate());

                    try {
                        fis.setNumUpdated(fis.getNumUpdated() + 1);
                        fxRepository.save(financeFX);
                    } catch (Exception ex) {
                        // Ignore since in DB already.
                        log.info("Not updating because already in DB");
                    }
                } else {
                    log.error("Cannot map to and/or from currency ids: " + fx.getFromCurrencyId() + " , " + fx.getToCurrencyId());
                }
            } else {
                log.warn("Missing to or/and from currency ids: " + fx.getFromCurrencyId() + " , " + fx.getToCurrencyId());
            }
        }

        sw.stop();

        fis.setDuration(sw.getLastTaskTimeMillis());
        fis.setTaskFinished(true);
        return fis;
    }

    public FinanceImportStatus syncUserSecurities(
        StopWatch sw,
        User user,
        OpenedDb oDB,
        MnyContext mnyContext,
        Source source,
        Map<Integer, Currency> currencies
    ) throws IOException {
        sw.start("syncUserSecurities");

        FinanceImportStatus fiStatus = new FinanceImportStatus();
        fiStatus.setTaskName("Sync User Securities");

        Map<String, SourceLink> securirtySourceLinks = getSourceLinksForType(user, source, TABLE_SECURITY);

        Map<Integer, Security> securities = SecurityImplUtil.getSecurities(oDB.getDb());

        log.debug("Importing all since: " + source.getLastSyncDateTime());

        Map<Integer, Security> securitiesWithoutLinks = new HashMap<Integer, Security>();
        Map<Integer, Security> securitiesWithLinks = new HashMap<Integer, Security>();

        for (com.le.sunriise.mnyobject.Security o : securities.values()) {
            if (o.getLinkId() == null) {
                securitiesWithoutLinks.put(o.getId(), o);
            } else {
                securitiesWithLinks.put(o.getId(), o);
            }
        }

        syncSecuritiesSub(user, fiStatus, securitiesWithoutLinks, source, securirtySourceLinks, currencies);
        syncSecuritiesSub(user, fiStatus, securitiesWithLinks, source, securirtySourceLinks, currencies);

        sw.stop();

        fiStatus.setDuration(sw.getLastTaskTimeMillis());
        fiStatus.setTaskFinished(true);
        if (fiStatus.getNumCreated() > 0 || fiStatus.getNumUpdated() > 0) {
            this.editorLookupCacheService.invalidateCategoryOptions(user.getGuid().toString());
            this.editorLookupCacheService.invalidateWhoOptions(user.getGuid().toString());
            this.editorLookupCacheService.invalidateWhoTreeOptions(user.getGuid().toString());
        }
        return fiStatus;
    }

    public void syncSecuritiesSub(
        User user,
        FinanceImportStatus fiStatus,
        Map<Integer, Security> securities,
        Source source,
        Map<String, SourceLink> securirtySourceLinks,
        Map<Integer, Currency> currencies
    ) {
        Map<String, FinanceSecurity> securityCache = new HashMap<String, FinanceSecurity>();
        List<SourceLink> newSlinks = new ArrayList<SourceLink>();
        for (com.le.sunriise.mnyobject.Security o : securities.values()) {
            log.debug(
                "Processing Security: " +
                    o.getName() +
                    ". Last Sync: " +
                    source.getLastSyncDateTime() +
                    ", Serial Date: " +
                    o.getSerialDate() +
                    ", Converted Serial: " +
                    ZonedDateTime.ofInstant(o.getSerialDate().toInstant(), ZoneId.systemDefault())
            );
            FinanceUserSecurity nObj = null;
            fiStatus.setNumInput(fiStatus.getNumInput() + 1);
            if (
                source.getLastSyncDateTime() == null ||
                (source.getLastSyncDateTime() != null &&
                    source.getLastSyncDateTime().isBefore(ZonedDateTime.ofInstant(o.getSerialDate().toInstant(), ZoneId.systemDefault())))
            ) {
                log.debug("Security: " + o.getName() + " updated since last sync. Checking if exists.");
                // Find Security since updated.
                Optional<SourceLink> sl = sourceLinkRepository.findByUserGuidAndSourceIdAndSourceEntityAndSourceTypeId(
                    user.getGuid().toString(),
                    o.getId().toString(),
                    TABLE_SECURITY,
                    source.getId().toString()
                );
                if (sl.isPresent()) {
                    // Check Source GUID - WIP
                    if (!sl.get().getSourceGuid().equals(trimSguid(o.getGuid()))) {
                        throw new RuntimeException(
                            "ID: " +
                                o.getId().toString() +
                                " returned an account with a different GUID. This is probably a different Money File. More than one money file import is not supported."
                        );
                    }

                    // Has to exist if data model is consistent
                    Optional<FinanceUserSecurity> a = userSecurityRepository.findById(UUID.fromString(sl.get().getLocalId()));
                    if (a.isPresent()) {
                        nObj = a.get();
                        log.debug("Account: " + o.getName() + " found.");
                    }
                }

                if (nObj == null) {
                    log.debug("Security: " + o.getName() + " is a new user security.");
                    nObj = new FinanceUserSecurity();
                    nObj.setUserGuid(user.getGuid().toString());
                    fiStatus.setNumCreated(fiStatus.getNumCreated() + 1);
                } else {
                    fiStatus.setNumUpdated(fiStatus.getNumUpdated() + 1);
                }

                // Double check if the linked security has been added already or not

                convertUserSecurity(user, o, nObj, securirtySourceLinks, securityCache, currencies);

                FinanceUserSecurity n = null;

                try {
                    n = userSecurityRepository.save(nObj);
                } catch (org.springframework.dao.DataIntegrityViolationException die) {
                    // This would show we have a previous item that has updated details. Move to update
                    // log.error("error", die.getCause());
                    // log.info("Error message:" + die.getMessage());
                    log.info(
                        "Looks like row might alredy exist. Trying to update. Messages: " +
                            die.getMessage() +
                            ", nObj masterGuid: " +
                            nObj.getMasterGuid()
                    );
                    Optional<FinanceUserSecurity> osec = userSecurityRepository.findByMasterGuid(nObj.getMasterGuid());
                    if (osec.isPresent()) {
                        nObj.setId(osec.get().getId());
                        n = userSecurityRepository.save(nObj);
                    } else {
                        // Not the reason - continuing throwing the RTE
                        throw die;
                    }
                }

                // Save only if an existing account wasn't present and already linked
                if (!sl.isPresent()) {
                    //SourceLink link = saveSourceLink(user, source, o, nObj);
                    SourceLink link = createSourceLink(
                        user,
                        source,
                        n.getId().toString(),
                        o.getId().toString(),
                        trimSguid(o.getGuid()),
                        TABLE_SECURITY,
                        true
                    );
                    newSlinks.add(link);
                    securirtySourceLinks.put(link.getSourceId(), link);
                }
            } else {
                log.debug("Security: " + o.getName() + " not updated since last sync. Skipping.");
            }
        }

        sourceLinkRepository.saveAll(newSlinks);
        newSlinks = null;
    }

    public FinanceImportStatus syncSecurityPrices(
        StopWatch sw,
        User user,
        OpenedDb oDB,
        MnyContext mnyContext,
        Source source,
        Map<String, SourceLink> securitySourceLinks
    ) throws IOException {
        sw.start("syncSecurityPrices");
        FinanceImportStatus fiStatus = new FinanceImportStatus();
        fiStatus.setTaskName("Sync Security Prices");

        Table table = oDB.getDb().getTable(TABLE_SECURITY_PRICE);

        Cursor cursor = null;
        cursor = Cursor.createCursor(table);

        Map<String, Object> row = null;
        log.debug("Importing all SPs since: " + source.getLastSyncDateTime());
        int batchSize = 500;

        List<com.le.sunriise.mnyobject.SecurityPrice> originals = new ArrayList<com.le.sunriise.mnyobject.SecurityPrice>(batchSize);

        // Map<Integer, com.le.sunriise.mnyobject.Payee> payees = PayeeImplUtil.getPayees(oDB.getDb());

        Instant instant = null;
        if (source.getLastSyncDateTime() != null) {
            instant = source.getLastSyncDateTime().toInstant();
        }

        Map<String, FinanceUserSecurity> userSecurities = getUserSecuritiesFromDB(user);

        while (cursor.moveToNextRow()) {
            row = cursor.getCurrentRow();
            fiStatus.setNumInput(fiStatus.getNumInput() + 1);

            com.le.sunriise.mnyobject.SecurityPrice original = SecurityImplUtil.createSPTransactionFromRow(oDB.getDb(), row, instant);

            if (original != null) {
                //log.info("ZZZ-AccountID on original txn : hacct = " + originalTxn.getAccountId() + " for txn: " + originalTxn.getId());
                originals.add(original);
            }

            if (originals.size() == batchSize) {
                // Process Batch
                processSPSyncBatch(user, fiStatus, originals, source, securitySourceLinks, oDB, userSecurities);
                originals.clear();
            } else {
                log.debug("Transaction Batched: " + originals.size());
            }
        }

        if (originals.size() > 0) {
            // Process Batch
            processSPSyncBatch(user, fiStatus, originals, source, securitySourceLinks, oDB, userSecurities);
            originals.clear();
        }

        sw.stop();

        fiStatus.setDuration(sw.getLastTaskTimeMillis());
        fiStatus.setTaskFinished(true);
        return fiStatus;
    }

    private void processSPSyncBatch(
        User user,
        FinanceImportStatus fiStatus,
        List<com.le.sunriise.mnyobject.SecurityPrice> originals,
        Source source,
        Map<String, SourceLink> securitySourceLinks,
        OpenedDb oDB,
        Map<String, FinanceUserSecurity> userSecurities
    ) {
        List<FinanceSecurityPrice> o = new ArrayList<FinanceSecurityPrice>(originals.size());
        for (com.le.sunriise.mnyobject.SecurityPrice original : originals) {
            FinanceSecurityPrice sp = new FinanceSecurityPrice();

            SourceLink sl = securitySourceLinks.get(original.getSecurityId().toString());
            if (sl != null) {
                FinanceUserSecurity s = userSecurities.get(sl.getLocalId());

                if (s.getSecurity() != null && s.getSecurity().getSymbol() != null) {
                    sp.setSymbol(s.getSecurity().getSymbol());
                } else {
                    String symbol = user.getGuid().toString() + ":" + original.getSecurityId();
                    sp.setSymbol(symbol);
                }
            } else {
                log.warn("Cannot find local ID for securityId: " + original.getSecurityId());
            }

            if (original.getChange() != null) sp.setChange(new BigDecimal(original.getChange()));
            if (original.getHigh() != null) sp.setHigh(new BigDecimal(original.getHigh()));
            if (original.getLow() != null) sp.setLow(new BigDecimal(original.getLow()));
            if (original.getOpen() != null) sp.setOpen(new BigDecimal(original.getOpen()));
            if (original.getClose() != null) sp.setClose(new BigDecimal(original.getClose()));
            if (original.getPrice() != null) sp.setPrice(new BigDecimal(original.getPrice()));

            sp.setDate(ZonedDateTime.ofInstant(original.getDate().toInstant(), ZoneId.systemDefault()));

            sp.setVolume(original.getVolume());

            o.add(sp);
            fiStatus.setNumUpdated(fiStatus.getNumUpdated() + 1);
        }

        try {
            securityPriceRepository.saveAll(o);
        } catch (Exception e) {
            log.warn("Got " + e.getMessage() + " exception, however ignoring since not critical to import process.");
            fiStatus.setError(e.getMessage());
        }
    }

    public FinanceImportStatus syncInstitutions(StopWatch sw, User user, OpenedDb oDB, MnyContext mnyContext, Source source)
        throws IOException {
        sw.start("importInstitutions");
        FinanceImportStatus fiStatus = new FinanceImportStatus();
        fiStatus.setTaskName("Sync Institutions");

        final Map<Integer, FinancialInstitution> fis = FinancialInstitutionImplUtil.getFinancialInstitutions(oDB.getDb());

        log.debug("Importing all Institutions since: " + source.getLastSyncDateTime());
        for (FinancialInstitution oldFI : fis.values()) {
            log.debug(
                "Processing Institutions: " +
                    oldFI.getName() +
                    ". Last Sync: " +
                    source.getLastSyncDateTime() +
                    ", Serial Date: " +
                    oldFI.getSerialDate() +
                    ", Converted Serial: " +
                    ZonedDateTime.ofInstant(oldFI.getSerialDate().toInstant(), ZoneId.systemDefault())
            );
            fiStatus.setNumInput(fiStatus.getNumInput() + 1);
            FinanceInstitution fi = null;
            if (
                source.getLastSyncDateTime() == null ||
                (source.getLastSyncDateTime() != null &&
                    source
                        .getLastSyncDateTime()
                        .isBefore(ZonedDateTime.ofInstant(oldFI.getSerialDate().toInstant(), ZoneId.systemDefault())))
            ) {
                log.debug("Institutions: " + oldFI.getName() + " updated since last sync. Checking if exists.");
                // Find Institution since updated.
                Optional<SourceLink> sl = sourceLinkRepository.findByUserGuidAndSourceIdAndSourceEntityAndSourceTypeId(
                    user.getGuid().toString(),
                    oldFI.getId().toString(),
                    TABLE_FI,
                    source.getId().toString()
                );
                if (sl.isPresent()) {
                    // Has to exist if data model is consistent
                    Optional<FinanceInstitution> a = fiRepository.findById(UUID.fromString(sl.get().getLocalId()));
                    if (a.isPresent()) {
                        fi = a.get();
                        log.debug("Institutions: " + oldFI.getName() + " found.");
                    }
                }

                if (fi == null) {
                    log.debug("FI: " + oldFI.getName() + " is a new FI.");
                    fi = new FinanceInstitution();
                    fiStatus.setNumCreated(fiStatus.getNumCreated() + 1);
                } else {
                    fiStatus.setNumUpdated(fiStatus.getNumUpdated() + 1);
                }

                fi.setName(oldFI.getName());

                FinanceInstitution newFI = fiRepository.save(fi);

                // Save only if an existing account wasn't present and already linked
                if (!sl.isPresent()) {
                    saveSourceLink(user, source, oldFI, newFI);
                }
            } else {
                log.debug("FI: " + oldFI.getName() + " not updated since last sync. Skipping.");
            }
        }

        sw.stop();
        fiStatus.setDuration(sw.getLastTaskTimeMillis());
        fiStatus.setTaskFinished(true);
        if (fiStatus.getNumCreated() > 0 || fiStatus.getNumUpdated() > 0) {
            this.editorLookupCacheService.invalidatePayeeOptions(user.getGuid().toString());
        }
        return fiStatus;
    }

    public FinanceImportStatus syncAccounts(
        StopWatch sw,
        User user,
        OpenedDb oDB,
        MnyContext mnyContext,
        Source source,
        Map<Integer, Currency> currencies,
        Map<String, SourceLink> fiSourceLinks
    ) throws IOException {
        sw.start("importAccounts");
        FinanceImportStatus fiStatus = new FinanceImportStatus();
        fiStatus.setTaskName("Sync Institutions");

        final List<com.le.sunriise.mnyobject.Account> accounts = AccountUtil.initMnyContext(oDB, mnyContext);

        log.debug("Importing all since: " + source.getLastSyncDateTime());
        for (com.le.sunriise.mnyobject.Account oldAccount : accounts) {
            log.debug(
                "Processing Account: " +
                    oldAccount.getName() +
                    ". Last Sync: " +
                    source.getLastSyncDateTime() +
                    ", Serial Date: " +
                    oldAccount.getSerialDate() +
                    ", Converted Serial: " +
                    ZonedDateTime.ofInstant(oldAccount.getSerialDate().toInstant(), ZoneId.systemDefault())
            );
            FinanceAccount account = null;
            if (
                source.getLastSyncDateTime() == null ||
                (source.getLastSyncDateTime() != null &&
                    source
                        .getLastSyncDateTime()
                        .isBefore(ZonedDateTime.ofInstant(oldAccount.getSerialDate().toInstant(), ZoneId.systemDefault())))
            ) {
                log.debug("Account: " + oldAccount.getName() + " updated since last sync. Checking if exists.");
                // Find account since updated.
                fiStatus.setNumInput(fiStatus.getNumInput() + 1);
                Optional<SourceLink> sl = sourceLinkRepository.findByUserGuidAndSourceIdAndSourceEntityAndSourceTypeId(
                    user.getGuid().toString(),
                    oldAccount.getId().toString(),
                    TABLE_ACCOUNT,
                    source.getId().toString()
                );
                if (sl.isPresent()) {
                    // Check Source GUID - WIP
                    if (!sl.get().getSourceGuid().equals(trimSguid(oldAccount.getGuid()))) {
                        throw new RuntimeException(
                            "ID: " +
                                oldAccount.getId().toString() +
                                " returned an account with a different GUID. This is probably a different Money File. More than one money file import is not supported."
                        );
                    }

                    // Has to exist if data model is consistent
                    Optional<FinanceAccount> a = accountRepository.findById(UUID.fromString(sl.get().getLocalId()));
                    if (a.isPresent()) {
                        account = a.get();
                        log.debug("Account: " + oldAccount.getName() + " found.");
                    }
                }

                if (account == null) {
                    log.info("Account: " + oldAccount.getName() + " is a new account.");
                    account = new FinanceAccount();
                    account.setUserGuid(user.getGuid().toString());
                    fiStatus.setNumCreated(fiStatus.getNumCreated() + 1);
                } else {
                    fiStatus.setNumUpdated(fiStatus.getNumUpdated() + 1);
                }

                convertAccount(oldAccount, account, currencies, fiSourceLinks);

                FinanceAccount newAccount = accountRepository.save(account);

                // Save only if an existing account wasn't present and already linked
                if (!sl.isPresent()) {
                    saveSourceLink(user, source, oldAccount, newAccount);
                }
            } else {
                log.debug("Account: " + oldAccount.getName() + " not updated since last sync. Skipping.");
            }
        }

        sw.stop();

        fiStatus.setDuration(sw.getLastTaskTimeMillis());
        fiStatus.setTaskFinished(true);
        return fiStatus;
    }

    public void fixRelatedToAccounts(StopWatch sw, User user, Map<String, SourceLink> accountSourceLinks) {
        sw.start("fixRelatedToAccounts");

        List<FinanceAccount> accounts = accountRepository.findAllByUserGuidAndClosed(user.getGuid().toString(), false);

        for (FinanceAccount account : accounts) {
            if (account.getRelatedToAccountId() == null && account.getRelatedToAccountSrcId() != null) {
                SourceLink sl = accountSourceLinks.get("" + account.getRelatedToAccountSrcId());
                // FinanceAccount a = accountRepository.findById(UUID.fromString(sl.getLocalId()));
                account.setRelatedToAccountId(sl.getLocalId());
                accountRepository.save(account);
            }
        }
        sw.stop();
    }

    public FinanceImportStatus syncCurrencies(StopWatch sw, User user, OpenedDb oDB, MnyContext mnyContext, Source source)
        throws IOException {
        sw.start("syncCurrencies");

        FinanceImportStatus fiStatus = new FinanceImportStatus();
        fiStatus.setTaskName("Sync Currencies");

        final Map<Integer, Currency> currencies = CurrencyImplUtil.getCurrencies(oDB.getDb());

        log.debug("Importing all since: " + source.getLastSyncDateTime());
        List<SourceLink> newSlinks = new ArrayList<SourceLink>();
        for (Currency origCurrency : currencies.values()) {
            log.debug(
                "Processing currency: " +
                    origCurrency.getName() +
                    ". Last Sync: " +
                    source.getLastSyncDateTime() +
                    ", Serial Date: " +
                    origCurrency.getSerialDate() +
                    ", Converted Serial: " +
                    ZonedDateTime.ofInstant(origCurrency.getSerialDate().toInstant(), ZoneId.systemDefault())
            );
            fiStatus.setNumInput(fiStatus.getNumInput() + 1);
            FinanceCurrency currency = null;
            if (
                source.getLastSyncDateTime() == null ||
                (source.getLastSyncDateTime() != null &&
                    source
                        .getLastSyncDateTime()
                        .isBefore(ZonedDateTime.ofInstant(origCurrency.getSerialDate().toInstant(), ZoneId.systemDefault())))
            ) {
                log.debug("Currency: " + origCurrency.getName() + " updated since last sync. Checking if exists.");

                Optional<SourceLink> sl = Optional.empty();

                Optional<FinanceCurrency> a = currencyRepository.findByIsoCode(origCurrency.getIsoCode());
                if (a.isPresent()) {
                    currency = a.get();
                    log.debug("currency: " + origCurrency.getName() + " found.");
                    sl = sourceLinkRepository.findByUserGuidAndSourceIdAndSourceEntityAndSourceTypeId(
                        user.getGuid().toString(),
                        origCurrency.getId().toString(),
                        TABLE_CURRENCY,
                        source.getId().toString()
                    );
                    fiStatus.setNumUpdated(fiStatus.getNumUpdated() + 1);
                } else {
                    log.debug("currency: " + origCurrency.getName() + " is a new currency.");
                    fiStatus.setNumCreated(fiStatus.getNumCreated() + 1);
                    currency = new FinanceCurrency();
                }

                convertCurrency(origCurrency, currency);

                FinanceCurrency updateCurrency = currencyRepository.save(currency);

                // Save only if an existing currency wasn't present and already linked
                if (!sl.isPresent()) {
                    //saveSourceLink(user, source, newAccount.getId().toString(), origCurrency.getId().toString(), trimSguid(origCurrency.getGuid()), TABLE_CURRENCY, true);
                    SourceLink nsl = createSourceLink(
                        user,
                        source,
                        updateCurrency.getId().toString(),
                        origCurrency.getId().toString(),
                        trimSguid(origCurrency.getGuid()),
                        TABLE_CURRENCY,
                        true
                    );
                    newSlinks.add(nsl);
                }
            } else {
                log.debug("Currency: " + origCurrency.getName() + " not updated since last sync. Skipping.");
            }
        }
        sourceLinkRepository.saveAll(newSlinks);
        sw.stop();

        fiStatus.setDuration(sw.getLastTaskTimeMillis());
        fiStatus.setTaskFinished(true);
        return fiStatus;
    }

    public FinanceCurrency convertCurrency(com.le.sunriise.mnyobject.Currency origCurrency, FinanceCurrency currency) {
        currency.setName(origCurrency.getName());
        currency.setIsoCode(origCurrency.getIsoCode());
        currency.setMasterGuid(trimSguid(origCurrency.getGuid()));

        return currency;
    }

    public FinanceUserSecurity convertUserSecurity(
        User user,
        com.le.sunriise.mnyobject.Security o,
        FinanceUserSecurity d,
        Map<String, SourceLink> securitySourceLinks,
        Map<String, FinanceSecurity> securityCache,
        Map<Integer, Currency> currencies
    ) {
        d.setComment(o.getComment());
        d.setMasterGuid(trimSguid(o.getGuid()));
        d.setSymbol(o.getSymbol());
        d.setName(o.getName()); // Name is included in case it is a user manual or they want to override.
        d.setType(o.getType());

        Currency c = currencies.get(Integer.decode(o.getCurrencyId()));

        d.setCurrencyCode(c.getIsoCode());

        FinanceSecurity security = securityCache.get(o.getSymbol());
        if (security == null) {
            security = this.securityService.handleUserSecurity(user, d, c.getIsoCode(), FETCH_SECUIRTY_INFO_FROM_WEB);
            if (security != null) {
                applyAustralianExchangeDefaults(security);
                security = securityRepository.save(security);
                securityCache.put(security.getSymbol(), security);
            }
        }

        FinanceSecurityType type = FinanceSecurityType.toSecurityType(o.getType());

        if (
            security == null &&
            (type == FinanceSecurityType.STOCK ||
                type == FinanceSecurityType.MUTUAL_FUND ||
                type == FinanceSecurityType.INDEX ||
                type == FinanceSecurityType.OTHER)
        ) {
            // Check there isn't a user-based security registered already
            Optional<FinanceSecurity> osecurity = securityRepository.findBySymbol(user.getGuid().toString() + ":" + o.getId());
            if (osecurity.isPresent()) {
                security = osecurity.get();
            }

            if (security == null) {
                security = new FinanceSecurity();
                security.setSymbol(user.getGuid() + ":" + o.getId());
                security.setName(o.getName());
                security.setCurrencyCode(c.getIsoCode());
                applyAustralianExchangeDefaults(security);
                securityRepository.save(security);
            }
            securityCache.put(security.getSymbol(), security);
        }

        d.setSecurity(security);

        if (o.getLinkId() != null) {
            SourceLink linkedAccount = securitySourceLinks.get("" + o.getLinkId());
            if (linkedAccount != null) {
                d.setLinked(new FinanceUserSecurity(UUID.fromString(linkedAccount.getLocalId())));
            } else {
                log.warn("Couldn't find local id (i.e. sourcelink) for Linked Security: " + linkedAccount);
            }
        }

        return d;
    }

    private void applyAustralianExchangeDefaults(FinanceSecurity security) {
        if (security == null) {
            return;
        }
        if (
            !isAustralianSecurity(security.getCountry(), security.getCurrencyCode()) ||
            (security.getExchangeMic() != null && !security.getExchangeMic().isBlank())
        ) {
            return;
        }
        security.setExchangeMic("XASX");
        if (security.getExchangeName() == null || security.getExchangeName().isBlank()) {
            security.setExchangeName("ASX");
        }
        if (security.getExchangeSuffix() == null || security.getExchangeSuffix().isBlank()) {
            security.setExchangeSuffix("AX");
        }
    }

    private boolean isAustraliaCountry(String country) {
        return country != null && "AUSTRALIA".equalsIgnoreCase(country.trim());
    }

    private boolean isAustralianSecurity(String country, String currencyCode) {
        return isAustraliaCountry(country) || (currencyCode != null && "AUD".equalsIgnoreCase(currencyCode.trim()));
    }

    public FinanceAccount convertAccount(
        com.le.sunriise.mnyobject.Account o,
        FinanceAccount d,
        Map<Integer, Currency> currencies,
        Map<String, SourceLink> fiSourceLinks
    ) {
        d.setName(o.getName());

        d.setStartingBalance(o.getStartingBalance());
        //d.setCurrentBalance(o.getCurrentBalance());
        d.setRetirement(o.getRetirement());
        d.setClosed(o.getClosed());
        if (o.getDateOpened() != null) {
            LocalDate dOpened = LocalDate.ofInstant(o.getDateOpened().toInstant(), ZoneId.systemDefault());
            if (!dOpened.isAfter(LocalDate.now().plusYears(1000))) {
                d.setDateOpened(dOpened);
            } else {
                d.setDateOpened(LocalDate.now().minusYears(20));
            }
        } else {
            d.setDateOpened(LocalDate.now().minusYears(20));
        }
        d.setType(o.getType());
        if (o.getRelatedToAccountId() != null) {
            d.setRelatedToAccountSrcId(o.getRelatedToAccountId());
        }

        //SourceLink link = currencySourceLinks.get(""+o.getCurrencyId());
        Currency currency = currencies.get(o.getCurrencyId());
        d.setCurrencyCode(currency.getIsoCode());

        if (o.getFI() != null) {
            SourceLink fiLink = fiSourceLinks.get("" + o.getFI());
            d.setInstitution(new FinanceInstitution(UUID.fromString(fiLink.getLocalId())));
        }

        return d;
    }

    public FinanceImportStatus syncCategories(StopWatch sw, User user, OpenedDb oDB, MnyContext mnyContext, Source source)
        throws IOException {
        // Refresh Source - Just in case updated out of proc.

        sw.start("syncCategories");

        FinanceImportStatus fiStatus = new FinanceImportStatus();
        fiStatus.setTaskName("syncCategories");

        Map<Integer, com.le.sunriise.mnyobject.Category> categories = CategoryImplUtil.getCategories(oDB.getDb());

        //Map<String, SourceLink> sourceLinkCache = cacheSourceLinksBySourceIdsCategories(categories);
        //source.setLastSyncDateTime(null);
        log.debug("Importing all since: " + source.getLastSyncDateTime());
        List<FinanceCategory> updateParent = new ArrayList<FinanceCategory>();
        List<SourceLink> newSlinks = new ArrayList<SourceLink>();
        for (com.le.sunriise.mnyobject.Category original : categories.values()) {
            log.debug(
                "Processing Category : " +
                    original.getName() +
                    ". Last Sync: " +
                    source.getLastSyncDateTime() +
                    ", Serial Date: " +
                    original.getSerialDate() +
                    ", Converted Serial: " +
                    ZonedDateTime.ofInstant(original.getSerialDate().toInstant(), ZoneId.systemDefault())
            );
            fiStatus.setNumInput(fiStatus.getNumInput() + 1);
            FinanceCategory category = null;
            Optional<SourceLink> sl = Optional.empty();
            if (
                source.getLastSyncDateTime() == null ||
                (source.getLastSyncDateTime() != null &&
                    source
                        .getLastSyncDateTime()
                        .isBefore(ZonedDateTime.ofInstant(original.getSerialDate().toInstant(), ZoneId.systemDefault())))
            ) {
                log.debug("Category: " + original.getName() + " updated since last sync. Checking if exists.");
                // Find account since updated.

                if (source.getLastSyncDateTime() != null) {
                    sl = sourceLinkRepository.findByUserGuidAndSourceIdAndSourceEntityAndSourceTypeId(
                        user.getGuid().toString(),
                        original.getId().toString(),
                        TABLE_CATEGORY,
                        source.getId().toString()
                    );
                    if (sl.isPresent()) {
                        // Has to exist if data model is consistent
                        Optional<FinanceCategory> a = categoryRepository.findById(UUID.fromString(sl.get().getLocalId()));
                        if (a.isPresent()) {
                            category = a.get();
                            log.info("Category: " + original.getName() + " found.");
                        }
                    }
                }

                if (category == null) {
                    log.info("Category: " + original.getName() + " is a new Category.");
                    category = new FinanceCategory();
                    category.setUserGuid(user.getGuid().toString());
                    fiStatus.setNumCreated(fiStatus.getNumCreated() + 1);
                } else {
                    fiStatus.setNumUpdated(fiStatus.getNumUpdated() + 1);
                }

                convertCategory(original, category);
                if (category.getSourceParentId() != null) {
                    updateParent.add(category);
                }

                FinanceCategory newCategory = categoryRepository.save(category);

                // Save only if an existing category wasn't present and already linked
                if (!sl.isPresent()) {
                    //saveSourceLink(user, source, original, newCategory);
                    SourceLink nsl = createSourceLink(
                        user,
                        source,
                        newCategory.getId().toString(),
                        original.getId().toString(),
                        trimSguid(original.getGuid()),
                        TABLE_CATEGORY,
                        true
                    );
                    newSlinks.add(nsl);
                }
            } else {
                log.info("Category: " + original.getName() + " not updated since last sync. Skipping.");
            }
        }
        sourceLinkRepository.saveAll(newSlinks);
        sw.stop();

        sw.start("Fix Parent Category Links");
        // Update Parents
        Map<String, SourceLink> categoriesSourceLinks = getSourceLinksForType(user, source, TABLE_CATEGORY);
        log.debug("categoriesSourceLinks: " + categoriesSourceLinks.toString());
        for (FinanceCategory category : updateParent) {
            SourceLink sl = categoriesSourceLinks.get(category.getSourceParentId().toString());
            if (sl == null) {
                log.warn("Cannot find parent category: " + category.getSourceParentId());
                //throw new RuntimeException("Cannot find parent category: " + category.getParentId());
            } else {
                category.setParent(new FinanceCategory(sl.getLocalId()));
                categoryRepository.save(category);
            }
        }

        sw.stop();

        fiStatus.setDuration(sw.getLastTaskTimeMillis());
        fiStatus.setTaskFinished(true);
        return fiStatus;
    }

    public FinanceCategory convertCategory(com.le.sunriise.mnyobject.Category oldCategory, FinanceCategory category) {
        category.setName(oldCategory.getName());
        category.setLevel(oldCategory.getLevel());

        category.setClassificationId(oldCategory.getClassificationId());
        if (oldCategory.getParentId() != null) {
            category.setSourceParentId(oldCategory.getParentId());
        }
        return category;
    }

    public FinanceImportStatus syncPayees(StopWatch sw, User user, OpenedDb oDB, MnyContext mnyContext, Source source) throws IOException {
        // Refresh Source - Just in case updated out of proc.
        StopWatch saveSW = new StopWatch();
        StopWatch slSW = new StopWatch();
        sw.start("syncPayees");
        FinanceImportStatus fiStatus = new FinanceImportStatus();
        fiStatus.setTaskName("Sync Payees");

        Map<Integer, com.le.sunriise.mnyobject.Payee> payees = PayeeImplUtil.getPayees(oDB.getDb());
        sourceLinkRepository.flush();

        //Map<String, SourceLink> sourceLinkCache = cacheSourceLinksBySourceIdsPayees(payees);
        //source.setLastSyncDateTime(null);
        log.debug("Importing all since: " + source.getLastSyncDateTime());
        List<FinancePayee> updateParent = new ArrayList<FinancePayee>();
        //List<FinancePayee> newPayees = new ArrayList<FinancePayee>();
        List<SourceLink> newSlinks = new ArrayList<SourceLink>();
        for (com.le.sunriise.mnyobject.Payee originalPayee : payees.values()) {
            log.debug(
                "Processing Payee : " +
                    originalPayee.getName() +
                    ". Last Sync: " +
                    source.getLastSyncDateTime() +
                    ", Serial Date: " +
                    originalPayee.getSerialDate() +
                    ", Converted Serial: " +
                    ZonedDateTime.ofInstant(originalPayee.getSerialDate().toInstant(), ZoneId.systemDefault())
            );
            FinancePayee payee = null;
            fiStatus.setNumInput(fiStatus.getNumInput() + 1);
            if (
                source.getLastSyncDateTime() == null ||
                (source.getLastSyncDateTime() != null &&
                    source
                        .getLastSyncDateTime()
                        .isBefore(ZonedDateTime.ofInstant(originalPayee.getSerialDate().toInstant(), ZoneId.systemDefault())))
            ) {
                log.debug("Payee: " + originalPayee.getName() + " updated since last sync. Checking if exists.");
                // Find account since updated.
                Optional<SourceLink> sl = Optional.empty();
                if (source.getLastSyncDateTime() != null) {
                    sl = sourceLinkRepository.findByUserGuidAndSourceIdAndSourceEntityAndSourceTypeId(
                        user.getGuid().toString(),
                        originalPayee.getId().toString(),
                        TABLE_PAYEE,
                        source.getId().toString()
                    );
                    if (sl.isPresent()) {
                        // Has to exist if data model is consistent
                        Optional<FinancePayee> a = payeeRepository.findById(UUID.fromString(sl.get().getLocalId()));
                        if (a.isPresent()) {
                            payee = a.get();
                            log.info("Payee: " + originalPayee.getName() + " found.");
                        }
                    }
                }

                if (payee == null) {
                    log.debug("Payee: " + originalPayee.getName() + " is a new Payee.");
                    payee = new FinancePayee();
                    payee.setUserGuid(user.getGuid().toString());
                    fiStatus.setNumCreated(fiStatus.getNumCreated() + 1);
                } else {
                    fiStatus.setNumUpdated(fiStatus.getNumUpdated() + 1);
                }

                convertPayee(originalPayee, payee);
                if (payee.getParentId() != null) {
                    updateParent.add(payee);
                }

                saveSW.start();
                FinancePayee newPayee = payeeRepository.save(payee);
                saveSW.stop();

                // Save only if an existing account wasn't present and already linked
                if (!sl.isPresent()) {
                    // slSW.start();
                    // saveSourceLink(user, source, originalPayee, newPayee);
                    // slSW.stop();
                    SourceLink nsl = createSourceLink(
                        user,
                        source,
                        newPayee.getId().toString(),
                        originalPayee.getId().toString(),
                        trimSguid(originalPayee.getGuid()),
                        TABLE_PAYEE,
                        true
                    );
                    newSlinks.add(nsl);
                }
            } else {
                log.debug("Payee: " + originalPayee.getName() + " not updated since last sync. Skipping.");
            }
        }

        slSW.start();
        sourceLinkRepository.saveAll(newSlinks);
        slSW.stop();

        log.debug("PAYEE SAVE: " + saveSW.getTotalTimeNanos());
        log.debug("SL SAVE: " + slSW.getTotalTimeNanos());
        sw.stop();

        sw.start("Fix Parent Payee Links");
        // Update Parents
        Map<String, SourceLink> sourceLinks = getSourceLinksForType(user, source, TABLE_PAYEE);
        log.debug("payeeSourceLinks: " + sourceLinks.toString());
        for (FinancePayee payee : updateParent) {
            SourceLink sl = sourceLinks.get(payee.getParentId());
            if (sl == null) {
                log.warn("Cannot find parent payee: " + payee.getParentId());
                //throw new RuntimeException("Cannot find parent category: " + category.getParentId());
            } else {
                payee.setParentId(sl.getLocalId());
                payeeRepository.save(payee);
            }
        }

        sw.stop();
        fiStatus.setDuration(sw.getLastTaskTimeMillis());
        fiStatus.setTaskFinished(true);
        return fiStatus;
    }

    public FinancePayee convertPayee(com.le.sunriise.mnyobject.Payee oldPayee, FinancePayee payee) {
        payee.setName(oldPayee.getName());
        if (oldPayee.getParent() != null) {
            payee.setParentId(oldPayee.getParent().toString());
        }
        payee.setHidden(oldPayee.getHidden());
        payee.setMasterGuid(oldPayee.getGuid());
        payee.setSerialDateTime(ZonedDateTime.ofInstant(oldPayee.getSerialDate().toInstant(), ZoneId.systemDefault()));
        return payee;
    }

    private void saveSourceLink(User user, Source source, com.le.sunriise.mnyobject.Account sourceAccount, FinanceAccount account) {
        saveSourceLink(
            user,
            source,
            account.getId().toString(),
            sourceAccount.getId().toString(),
            trimSguid(sourceAccount.getGuid()),
            TABLE_ACCOUNT,
            true
        );
    }

    private SourceLink saveSourceLink(User user, Source source, FinancialInstitution sObj, FinanceInstitution nObj) {
        return saveSourceLink(user, source, nObj.getId().toString(), sObj.getId().toString(), null, TABLE_FI, true);
    }

    private Map<Integer, SourceLink> getSourceLinksByIDForType(User user, Source source, String entityType) {
        String userId = null;
        if (user != null) {
            userId = user.getGuid().toString();
        }
        List<SourceLink> links = sourceLinkRepository.findByUserGuidAndSourceEntityAndSourceTypeId(
            userId,
            entityType,
            source.getId().toString()
        );

        Map<Integer, SourceLink> sourceLinkCacheOut = new HashMap<Integer, SourceLink>();
        for (SourceLink link : links) {
            sourceLinkCacheOut.put(Integer.parseInt(link.getSourceId()), link);
        }
        return sourceLinkCacheOut;
    }

    private Map<String, SourceLink> getSourceLinksForType(User user, Source source, String entityType) {
        String userId = null;
        if (user != null) {
            userId = user.getGuid().toString();
        }
        List<SourceLink> links = sourceLinkRepository.findByUserGuidAndSourceEntityAndSourceTypeId(
            userId,
            entityType,
            source.getId().toString()
        );
        return transferSourceLinkListToSourceIds(links);
    }

    private Map<String, SourceLink> transferSourceLinkListToSourceIds(List<SourceLink> links) {
        Map<String, SourceLink> sourceLinkCacheOut = new HashMap<String, SourceLink>();
        for (SourceLink link : links) {
            sourceLinkCacheOut.put(link.getSourceId(), link);
        }
        return sourceLinkCacheOut;
    }

    private void syncTransferLinks(User user, OpenedDb oDB, Source source) throws IOException {
        List<TransferLink> sourceTransferLinks = TransferLinkImplUtil.getTransferLinks(oDB.getDb());
        Map<String, SourceLink> transactionSourceLinks = getSourceLinksForType(user, source, TABLE_TRN);
        Map<String, SourceLink> transferSourceLinks = getSourceLinksForType(user, source, TABLE_XFER);

        List<SourceLink> newTransferSourceLinks = new ArrayList<>();
        List<String> seenTransferSourceIds = new ArrayList<>();

        for (TransferLink sourceTransferLink : sourceTransferLinks) {
            String sourceTransferId = buildTransferSourceId(sourceTransferLink.getFromId(), sourceTransferLink.getLinkId());
            seenTransferSourceIds.add(sourceTransferId);

            SourceLink fromTransactionLink = transactionSourceLinks.get(sourceTransferLink.getFromId().toString());
            SourceLink linkedTransactionLink = transactionSourceLinks.get(sourceTransferLink.getLinkId().toString());
            if (fromTransactionLink == null || linkedTransactionLink == null) {
                log.warn(
                    "Skipping transfer link {} because one of the transactions was not mapped locally. from={}, link={}",
                    sourceTransferId,
                    fromTransactionLink,
                    linkedTransactionLink
                );
                continue;
            }

            UUID fromId = UUID.fromString(fromTransactionLink.getLocalId());
            UUID linkId = UUID.fromString(linkedTransactionLink.getLocalId());
            FinanceTransferLink transferLink = resolveExistingTransferLink(user, transferSourceLinks.get(sourceTransferId), fromId, linkId);
            transferLink.setUserGuid(user.getGuid().toString());
            transferLink.setFromId(fromId);
            transferLink.setLinkId(linkId);
            FinanceTransferLink savedTransferLink = this.transferLinkRepository.save(transferLink);

            if (!transferSourceLinks.containsKey(sourceTransferId)) {
                newTransferSourceLinks.add(
                    createSourceLink(user, source, savedTransferLink.getId().toString(), sourceTransferId, null, TABLE_XFER, true)
                );
            }
        }

        if (!newTransferSourceLinks.isEmpty()) {
            this.sourceLinkRepository.saveAll(newTransferSourceLinks);
        }

        for (SourceLink existingTransferSourceLink : transferSourceLinks.values()) {
            if (seenTransferSourceIds.contains(existingTransferSourceLink.getSourceId())) {
                continue;
            }

            this.transferLinkRepository
                .findById(UUID.fromString(existingTransferSourceLink.getLocalId()))
                .ifPresent(this.transferLinkRepository::delete);
            this.sourceLinkRepository.delete(existingTransferSourceLink);
        }
    }

    private FinanceTransferLink resolveExistingTransferLink(User user, SourceLink existingTransferSourceLink, UUID fromId, UUID linkId) {
        if (existingTransferSourceLink != null) {
            Optional<FinanceTransferLink> persistedTransferLink = this.transferLinkRepository.findById(
                UUID.fromString(existingTransferSourceLink.getLocalId())
            );
            if (persistedTransferLink.isPresent()) {
                return persistedTransferLink.get();
            }
        }

        return this.transferLinkRepository
            .findByUserGuidAndFromIdAndLinkId(user.getGuid().toString(), fromId, linkId)
            .or(() -> this.transferLinkRepository.findByUserGuidAndTransactionId(user.getGuid().toString(), fromId))
            .or(() -> this.transferLinkRepository.findByUserGuidAndTransactionId(user.getGuid().toString(), linkId))
            .orElseGet(FinanceTransferLink::new);
    }

    private String buildTransferSourceId(Integer fromId, Integer linkId) {
        return fromId + ":" + linkId;
    }

    private void deleteTransferLinksForTransactions(User user, List<UUID> transactionIds) {
        if (transactionIds.isEmpty()) {
            return;
        }

        List<UUID> transferLinkIdsToDelete = new ArrayList<>();
        for (UUID transactionId : transactionIds) {
            this.transferLinkRepository.findByUserGuidAndTransactionId(user.getGuid().toString(), transactionId).ifPresent(transferLink -> {
                if (!transferLinkIdsToDelete.contains(transferLink.getId())) {
                    transferLinkIdsToDelete.add(transferLink.getId());
                }
            });
        }

        if (transferLinkIdsToDelete.isEmpty()) {
            return;
        }

        List<String> transferLinkLocalIds = transferLinkIdsToDelete.stream().map(UUID::toString).toList();
        List<SourceLink> transferLinkSourceLinks = this.sourceLinkRepository.findByUserGuidAndLocalIdIn(
            user.getGuid().toString(),
            transferLinkLocalIds
        );
        if (!transferLinkSourceLinks.isEmpty()) {
            this.sourceLinkRepository.deleteAll(transferLinkSourceLinks);
        }
        this.transferLinkRepository.deleteAllById(transferLinkIdsToDelete);
    }

    private SourceLink saveSourceLink(
        User user,
        Source source,
        String localId,
        String sourceId,
        String sourceGuid,
        String sourceEntity,
        boolean master
    ) {
        log.debug("Saving sourceLink:" + localId + ":" + sourceId);
        SourceLink sl = sourceLinkRepository.save(createSourceLink(user, source, localId, sourceId, sourceGuid, sourceEntity, master));
        log.debug("Saved sourceLink:" + localId + ":" + sourceId);
        return sl;
    }

    private SourceLink createSourceLink(
        User user,
        Source source,
        String localId,
        String sourceId,
        String sourceGuid,
        String sourceEntity,
        boolean master
    ) {
        SourceLink sl = new SourceLink();
        if (user != null) {
            sl.setUserGuid(user.getGuid().toString());
        }

        sl.setLocalId(localId);
        sl.setSourceId(sourceId);
        sl.setSourceGuid(sourceGuid);
        sl.setSourceEntity(sourceEntity);
        sl.setSourceTypeId(source.getId().toString());
        sl.setMaster(master);
        return sl;
    }

    public Set<String> getTables(OpenedDb oDB) throws IOException {
        Set<String> tableNames = oDB.getDb().getTableNames();
        for (String tableName : tableNames) {
            try {
                Table table = oDB.getDb().getTable(tableName);

                log.debug("Found Table: " + table.getName());
            } catch (IOException e) {
                log.warn("" + e, e);
            }
        }
        return tableNames;
    }

    public FinanceImportStatus syncTransactions(
        StopWatch sw,
        User user,
        OpenedDb oDB,
        MnyContext mnyContext,
        Source source,
        Map<String, SourceLink> accountSourceLinks,
        Map<String, SourceLink> payeeSourceLinks,
        Map<String, SourceLink> categoriesSourceLinks,
        Map<String, SourceLink> securitySourceLinks,
        Map<Integer, Currency> currencies
    ) throws IOException {
        sw.start("syncTransactions");
        FinanceImportStatus fiStatus = new FinanceImportStatus();
        fiStatus.setTaskName("Sync Transactions");

        Table table = oDB.getDb().getTable(TABLE_TRN);

        Cursor cursor = null;
        cursor = Cursor.createCursor(table);

        Map<String, Object> row = null;
        log.debug("Importing all txns since: " + source.getLastSyncDateTime());
        int batchSize = 750;

        List<com.le.sunriise.mnyobject.Transaction> originalTxns = new ArrayList<com.le.sunriise.mnyobject.Transaction>(batchSize);

        Map<Integer, com.le.sunriise.mnyobject.Payee> payees = PayeeImplUtil.getPayees(oDB.getDb());

        Map<Integer, Integer> pendingSplits = new HashMap<Integer, Integer>();
        Map<Integer, Integer> transferLinksByFromId = new HashMap<Integer, Integer>();
        for (TransferLink transferLink : TransferLinkImplUtil.getTransferLinks(oDB.getDb())) {
            transferLinksByFromId.put(transferLink.getFromId(), transferLink.getLinkId());
            //transferLinksByFromId.put(transferLink.getLinkId(), transferLink.getFromId());
        }

        Instant instant = null;
        if (source.getLastSyncDateTime() != null) {
            instant = source.getLastSyncDateTime().toInstant();
        }

        while (cursor.moveToNextRow()) {
            row = cursor.getCurrentRow();
            fiStatus.setNumInput(fiStatus.getNumInput() + 1);
            com.le.sunriise.mnyobject.Transaction originalTxn = TransactionImplUtil.createTransactionFromRow(
                oDB.getDb(),
                row,
                instant,
                payees
            );

            if (originalTxn != null) {
                log.debug("AccountID on original txn : hacct = " + originalTxn.getAccountId() + " for txn: " + originalTxn.getId());
                originalTxns.add(originalTxn);
            }

            if (originalTxns.size() == batchSize) {
                // Process Batch
                AccountUtil.handleSplitNoRemove(oDB.getDb(), originalTxns);
                // Process Batch
                processTransactionSyncBatch(
                    user,
                    fiStatus,
                    originalTxns,
                    source,
                    accountSourceLinks,
                    payeeSourceLinks,
                    categoriesSourceLinks,
                    securitySourceLinks,
                    oDB,
                    currencies,
                    pendingSplits,
                    transferLinksByFromId
                );
            } else {
                log.debug("Transaction Batched: " + originalTxns.size());
            }
        }

        if (originalTxns.size() > 0) {
            // Process Batch
            AccountUtil.handleSplitNoRemove(oDB.getDb(), originalTxns);

            processTransactionSyncBatch(
                user,
                fiStatus,
                originalTxns,
                source,
                accountSourceLinks,
                payeeSourceLinks,
                categoriesSourceLinks,
                securitySourceLinks,
                oDB,
                currencies,
                pendingSplits,
                transferLinksByFromId
            );
        }

        fixPendingSplits(user, pendingSplits);
        syncTransferLinks(user, oDB, source);

        sw.stop();
        fiStatus.setDuration(sw.getLastTaskTimeMillis());
        fiStatus.setTaskFinished(true);
        return fiStatus;
    }

    private void fixPendingSplits(User user, Map<Integer, Integer> pendingSplits) {
        for (Integer childId : pendingSplits.keySet()) {
            Integer parentId = pendingSplits.get(childId);
            if (parentId != null) {
                SourceLink parent = sourceLinkRepository.findByUserGuidAndSourceEntityAndSourceId(
                    user.getGuid().toString(),
                    TABLE_TRN,
                    parentId.toString()
                );
                SourceLink child = sourceLinkRepository.findByUserGuidAndSourceEntityAndSourceId(
                    user.getGuid().toString(),
                    TABLE_TRN,
                    childId.toString()
                );

                if (child != null) {
                    Optional<FinanceTransaction> childTXN = transactionRepository.findById(UUID.fromString(child.getLocalId()));
                    //Optional<FinanceTransaction> parentTXN = transactionRepository.findById(UUID.fromString(parent.getLocalId()));

                    if (childTXN.isPresent() && parent != null) {
                        childTXN.get().setParentId(UUID.fromString(parent.getLocalId()));
                        transactionRepository.save(childTXN.get());
                    }
                } else {
                    log.error("Cannot find child transaction with original ID: " + childId);
                }
            }
        }
    }

    public void removeDeletedTransactions(StopWatch sw, User user, FinanceImportStatus fiStatus, OpenedDb oDB, Source source)
        throws IOException {
        sw.start("removeDeletedTransactions");
        Table table = oDB.getDb().getTable(TABLE_TRN);

        Cursor cursor = null;
        cursor = Cursor.createCursor(table);
        Map<String, Object> row = null;
        Map<String, SourceLink> txnSourceLinks = getSourceLinksForType(user, source, TABLE_TRN);
        while (cursor.moveToNextRow()) {
            row = cursor.getCurrentRow();
            Integer htrn = (Integer) row.get(TransactionImplUtil.COL_ID);
            String sourceId = htrn.toString();
            boolean removed = txnSourceLinks.entrySet().removeIf(entry -> getBaseTransactionSourceId(entry.getKey()).equals(sourceId));
            if (!removed) {
                log.error("Couldn't find transaction " + htrn + " in the database.");
            }
        }

        log.info("Looks like " + txnSourceLinks.size() + " were deleted");
        if (txnSourceLinks.size() > 0) {
            List<UUID> ids = new ArrayList<UUID>(txnSourceLinks.size());
            List<Long> slIds = new ArrayList<Long>(txnSourceLinks.size());

            for (SourceLink sl : txnSourceLinks.values()) {
                log.info("SourceLink: " + sl + " should be deleted");
                ids.add(UUID.fromString(sl.getLocalId()));
                slIds.add(sl.getId());
            }

            deleteTransferLinksForTransactions(user, ids);
            transactionRepository.deleteAllById(ids);
            sourceLinkRepository.deleteAllById(slIds);
        }

        sw.stop();
    }

    private static int ITERATION = 0;

    private static class ImportedTransactionItem {

        private final FinanceTransaction transaction;
        private final String sourceId;
        private final String sourceGuid;

        private ImportedTransactionItem(FinanceTransaction transaction, String sourceId, String sourceGuid) {
            this.transaction = transaction;
            this.sourceId = sourceId;
            this.sourceGuid = sourceGuid;
        }
    }

    private static class ImportedTransferLinkItem {

        private final FinanceTransaction fromTransaction;
        private final FinanceTransaction linkTransaction;
        private final String sourceId;

        private ImportedTransferLinkItem(FinanceTransaction fromTransaction, FinanceTransaction linkTransaction, String sourceId) {
            this.fromTransaction = fromTransaction;
            this.linkTransaction = linkTransaction;
            this.sourceId = sourceId;
        }
    }

    private void processTransactionSyncBatch(
        User user,
        FinanceImportStatus fiStatus,
        List<com.le.sunriise.mnyobject.Transaction> originalTxns,
        Source source,
        Map<String, SourceLink> accountSourceLinks,
        Map<String, SourceLink> payeeSourceLinks,
        Map<String, SourceLink> categoriesSourceLinks,
        Map<String, SourceLink> securitySourceLinks,
        OpenedDb oDB,
        Map<Integer, Currency> currencies,
        Map<Integer, Integer> pendingSplits,
        Map<Integer, Integer> transferLinksByFromId
    ) {
        StopWatch sw = new StopWatch();
        ITERATION++;
        sw.start("Process Txn Sync #" + ITERATION + ". Cache Source Links");

        //sw.start("Process Batch Size: " + originalTxns.size() + "");
        ///Map<String, SourceLink> sourceLinkCache2 = cacheSourceLinksBySourceIds(originalTxns);
        Map<String, SourceLink> txnSourceLinks = getSourceLinksForType(user, source, TABLE_TRN);
        Map<String, SourceLink> transferSourceLinks = getSourceLinksForType(user, source, TABLE_XFER);
        sw.stop();
        sw.start("Process Txn Sync #" + ITERATION + ". Processing Txns[" + originalTxns.size() + "]");
        Map<Integer, com.le.sunriise.mnyobject.Transaction> originalTxnsById = new HashMap<
            Integer,
            com.le.sunriise.mnyobject.Transaction
        >();
        Map<Integer, com.le.sunriise.mnyobject.Transaction> splitParentsByChildId = new HashMap<
            Integer,
            com.le.sunriise.mnyobject.Transaction
        >();
        for (com.le.sunriise.mnyobject.Transaction originalTxn : originalTxns) {
            originalTxnsById.put(originalTxn.getId(), originalTxn);
            if (originalTxn.getSplits() != null) {
                for (TransactionSplit split : originalTxn.getSplits()) {
                    if (split.getTransaction() != null) {
                        splitParentsByChildId.put(split.getTransaction().getId(), originalTxn);
                    }
                }
            }
        }

        List<ImportedTransactionItem> importedItems = new ArrayList<ImportedTransactionItem>(originalTxns.size() * 2);
        List<ImportedTransferLinkItem> importedTransferLinks = new ArrayList<>();
        Set<Integer> skippedSourceTxnIds = identifyLoanTransferCounterpartsToSkip(
            originalTxnsById,
            splitParentsByChildId,
            transferLinksByFromId,
            accountSourceLinks
        );
        for (com.le.sunriise.mnyobject.Transaction originalTxn : originalTxns) {
            ImportedBatchItem importedBatchItem = processTransactionSyncBatchItem(
                user,
                fiStatus,
                originalTxn,
                source,
                txnSourceLinks,
                transferSourceLinks,
                accountSourceLinks,
                payeeSourceLinks,
                categoriesSourceLinks,
                securitySourceLinks,
                oDB,
                currencies,
                pendingSplits,
                transferLinksByFromId,
                splitParentsByChildId,
                skippedSourceTxnIds
            );
            importedItems.addAll(importedBatchItem.transactions);
            importedTransferLinks.addAll(importedBatchItem.transferLinks);
        }
        List<FinanceTransaction> txns = importedItems
            .stream()
            .map(item -> item.transaction)
            .toList();
        sw.stop();
        sw.start("Process Txn Sync #" + ITERATION + ". Saving Txns[" + originalTxns.size() + "]");
        List<FinanceTransaction> newTxns = transactionRepository.saveAll(txns);
        sw.stop();
        sw.start("Process Txn Sync #" + ITERATION + ". Saving SourceLinks[" + originalTxns.size() + "]");

        // Save Source Links
        List<SourceLink> newSlinks = new ArrayList<SourceLink>();
        Map<String, SourceLink> sourceLinkCacheByLocalId = transferSourceLinkCacheToLocalIds(txnSourceLinks);
        for (ImportedTransactionItem importedItem : importedItems) {
            FinanceTransaction newTxn = importedItem.transaction;
            if (sourceLinkCacheByLocalId.get(newTxn.getId().toString()) == null) {
                SourceLink sl = createSourceLink(
                    user,
                    source,
                    newTxn.getId().toString(),
                    importedItem.sourceId,
                    importedItem.sourceGuid,
                    TABLE_TRN,
                    true
                );
                newSlinks.add(sl);
            }
        }

        sourceLinkRepository.saveAll(newSlinks);

        List<SourceLink> newTransferSourceLinks = new ArrayList<>();
        for (ImportedTransferLinkItem importedTransferLink : importedTransferLinks) {
            UUID fromId = importedTransferLink.fromTransaction.getId();
            UUID linkId = importedTransferLink.linkTransaction.getId();
            FinanceTransferLink transferLink = resolveExistingTransferLink(
                user,
                transferSourceLinks.get(importedTransferLink.sourceId),
                fromId,
                linkId
            );
            transferLink.setUserGuid(user.getGuid().toString());
            transferLink.setFromId(fromId);
            transferLink.setLinkId(linkId);
            FinanceTransferLink savedTransferLink = this.transferLinkRepository.save(transferLink);

            if (!transferSourceLinks.containsKey(importedTransferLink.sourceId)) {
                newTransferSourceLinks.add(
                    createSourceLink(
                        user,
                        source,
                        savedTransferLink.getId().toString(),
                        importedTransferLink.sourceId,
                        null,
                        TABLE_XFER,
                        true
                    )
                );
            }
        }
        if (!newTransferSourceLinks.isEmpty()) {
            this.sourceLinkRepository.saveAll(newTransferSourceLinks);
        }
        sw.stop();
        log.info(sw.prettyPrint());

        newTxns.clear();
        originalTxns.clear();
        newSlinks.clear();
    }

    private static class ImportedBatchItem {

        private final List<ImportedTransactionItem> transactions;
        private final List<ImportedTransferLinkItem> transferLinks;

        private ImportedBatchItem(List<ImportedTransactionItem> transactions, List<ImportedTransferLinkItem> transferLinks) {
            this.transactions = transactions;
            this.transferLinks = transferLinks;
        }
    }

    private Map<String, SourceLink> transferSourceLinkCacheToLocalIds(Map<String, SourceLink> sourceLinkCacheIn) {
        Map<String, SourceLink> sourceLinkCacheOut = new HashMap<String, SourceLink>();
        for (SourceLink link : sourceLinkCacheIn.values()) {
            sourceLinkCacheOut.put(link.getLocalId(), link);
        }
        return sourceLinkCacheOut;
    }

    private ImportedBatchItem processTransactionSyncBatchItem(
        User user,
        FinanceImportStatus fiStatus,
        com.le.sunriise.mnyobject.Transaction originalTxn,
        Source source,
        Map<String, SourceLink> sourceLinkCache,
        Map<String, SourceLink> transferSourceLinks,
        Map<String, SourceLink> accountCache,
        Map<String, SourceLink> payeeSourceLinks,
        Map<String, SourceLink> categoriesSourceLinks,
        Map<String, SourceLink> securitySourceLinks,
        OpenedDb oDB,
        Map<Integer, Currency> currencies,
        Map<Integer, Integer> pendingSplits,
        Map<Integer, Integer> transferLinksByFromId,
        Map<Integer, com.le.sunriise.mnyobject.Transaction> splitParentsByChildId,
        Set<Integer> skippedSourceTxnIds
    ) {
        if (skippedSourceTxnIds.contains(originalTxn.getId())) {
            return new ImportedBatchItem(List.of(), List.of());
        }

        LoanImportResult loanImportResult = processLoanSplitTransactions(
            user,
            fiStatus,
            originalTxn,
            source,
            sourceLinkCache,
            transferSourceLinks,
            accountCache,
            payeeSourceLinks,
            categoriesSourceLinks,
            securitySourceLinks,
            oDB,
            currencies,
            pendingSplits,
            transferLinksByFromId,
            splitParentsByChildId
        );
        if (loanImportResult != null) {
            skippedSourceTxnIds.addAll(loanImportResult.sourceTxnIdsToSkip);
            return new ImportedBatchItem(loanImportResult.transactions, loanImportResult.transferLinks);
        }

        SourceLink sl = sourceLinkCache.get(originalTxn.getId().toString());
        FinanceTransaction txn;
        if (sl != null) {
            // Has to exist if data model is consistent
            log.debug(
                "Found SourceLink for transaction: " +
                    originalTxn.getId() +
                    ", suid: " +
                    originalTxn.getGuid() +
                    ", LocalID: " +
                    sl.getLocalId()
            );
            Optional<FinanceTransaction> a = transactionRepository.findById(UUID.fromString(sl.getLocalId()));
            if (a.isPresent()) {
                txn = a.get();
                log.debug(
                    "Existing transaction: " +
                        originalTxn.getId() +
                        " found with local ID: " +
                        txn.getId().toString() +
                        ", and suid: " +
                        txn.getMasterGuid()
                );

                updateTransaction(
                    txn,
                    originalTxn,
                    accountCache,
                    payeeSourceLinks,
                    categoriesSourceLinks,
                    securitySourceLinks,
                    oDB,
                    currencies,
                    pendingSplits
                );
                fiStatus.setNumUpdated(fiStatus.getNumUpdated() + 1);
            } else {
                String error =
                    "Couldn't find existing transaction which had a sourcelink: " +
                    sl.getLocalId() +
                    ", which was for original txn id: " +
                    originalTxn.getId();
                log.error(error);
                fiStatus.setError(error);
                throw new RuntimeException(error);
            }
        } else {
            log.debug("Txn: " + originalTxn.getId() + " is a new transaction, creating.");
            txn = convertTransaction(
                user,
                originalTxn,
                accountCache,
                payeeSourceLinks,
                categoriesSourceLinks,
                securitySourceLinks,
                oDB,
                currencies,
                pendingSplits
            );
            fiStatus.setNumCreated(fiStatus.getNumCreated() + 1);
        }

        return new ImportedBatchItem(
            List.of(new ImportedTransactionItem(txn, originalTxn.getId().toString(), trimSguid(originalTxn.getGuid()))),
            List.of()
        );
    }

    private static String trimSguid(String id) {
        id = id.substring(1, id.length() - 1);
        return id;
    }

    private void updateTransaction(
        FinanceTransaction txn,
        com.le.sunriise.mnyobject.Transaction originalTransaction,
        Map<String, SourceLink> accountCache,
        Map<String, SourceLink> payeeSourceLinks,
        Map<String, SourceLink> categoriesSourceLinks,
        Map<String, SourceLink> securitySourceLinks,
        OpenedDb oDB,
        Map<Integer, Currency> currencies,
        Map<Integer, Integer> transactionSplits
    ) {
        // if(originalTransaction.getGuid().equals("{96BE1D15-32D8-4751-A6C5-82160C89ED72}")) {
        //     log.info("ZZZ - Found 96BE1D15-32D8-4751-A6C5-82160C89ED72 txn: " + originalTransaction + ", " + originalTransaction.getFrequency().getFrequencyString());
        // }
        // if(originalTransaction.getGuid().equals("{8A1A08BB-834F-434C-96BC-3DB1994FE297}")) {
        //     log.info("ZZZ - Found 8A1A08BB-834F-434C-96BC-3DB1994FE297 txn: " + originalTransaction);
        //     if(originalTransaction.getFrequency() != null) {
        //         log.info("ZZZ - Found 8A1A08BB-834F-434C-96BC-3DB1994FE297 txn: " + originalTransaction + ", " + originalTransaction.getFrequency().getFrequencyString());
        //     }
        // }
        txn.setNumber(originalTransaction.getId());
        txn.setDate(LocalDate.ofInstant(originalTransaction.getDate().toInstant(), ZoneId.systemDefault()));
        txn.setAmount(originalTransaction.getAmount());
        txn.setPrincipalAmount(null);
        if (originalTransaction.getCategoryId() != null) {
            SourceLink categoryLink = categoriesSourceLinks.get(originalTransaction.getCategoryId().toString());
            txn.setCategory(new FinanceCategory(categoryLink.getLocalId()));
        } else {
            txn.setCategory(null);
        }

        if (originalTransaction.getWhoId() != null && !"-1".equals(originalTransaction.getWhoId().toString())) {
            SourceLink categoryLink = categoriesSourceLinks.get(originalTransaction.getWhoId().toString());
            txn.setWho(new FinanceCategory(categoryLink.getLocalId()));
        } else {
            txn.setWho(null);
        }

        txn.setSourcePayeeId(originalTransaction.getPayeeId());
        SourceLink payeeLink = payeeSourceLinks.get(originalTransaction.getPayeeId().toString());
        if (payeeLink != null) {
            txn.setPayeeId(payeeLink.getLocalId());
        } else {
            txn.setPayeeId(null);
            log.warn("Couldn't find local payee for id: " + originalTransaction.getPayeeId() + ", SL: " + payeeLink);
        }
        if (originalTransaction.getPayee() != null) {
            txn.setPayeeName(originalTransaction.getPayee().getName());
        }
        txn.setMemo(originalTransaction.getMemo());
        //txn.setRunningBalance(originalTransaction.getRunningBalance());

        SourceLink link = accountCache.get("" + originalTransaction.getAccountId());
        if (link != null) {
            txn.setAccountId(link.getLocalId());
        } else {
            log.warn("Couldn't find local account for id: " + originalTransaction.getAccountId());
        }

        SourceLink transferLink = accountCache.get("" + originalTransaction.getTransferredAccountId());
        if (transferLink != null) {
            txn.setTransferredAccountId(transferLink.getLocalId());
            txn.setTransfer(true);
        } else {
            txn.setTransferredAccountId(null);
            txn.setTransfer(false);
            log.warn("Couldn't find local account for id: " + originalTransaction.getTransferredAccountId());
        }

        txn.setStatementId(originalTransaction.getFiTransactionId());

        if (originalTransaction.getTransactionInfo() != null) {
            txn.setInvestment(originalTransaction.getTransactionInfo().isInvestment());
            txn.setSplitChild(originalTransaction.getTransactionInfo().isSplitChild());
            txn.setSplitParent(originalTransaction.getTransactionInfo().isSplitParent());
            txn.setTransferTo(originalTransaction.getTransactionInfo().isTransferTo());
        }

        if (originalTransaction.getSplits() != null && originalTransaction.getSplits().size() > 0) {
            for (TransactionSplit split : originalTransaction.getSplits()) {
                transactionSplits.put(split.getTransaction().getId(), originalTransaction.getId());
                // log.info("SPLIT Parent ID: " + split.getParentId());
                // log.info("SPLIT Row ID: " + split.getRowId());
                // log.info("SPLIT Transaction: " + );
                // if(split.getTransaction() == null) {
                //     log.info("SPLIT Transaction GUID: " + split.getTransaction().getGuid());
                // }

                // log.info("----");
            }
        }

        if (originalTransaction.isInvestment() && oDB != null) {
            try {
                InvestmentTransaction itxn = InvestmentTransactionImplUtil.getInvestmentTransaction(
                    oDB.getDb(),
                    originalTransaction.getId()
                );
                txn.setPrice(itxn.getPrice());
                txn.setQuantity(itxn.getQuantity());
                if (originalTransaction.getSecurityId() != null) {
                    SourceLink lin = securitySourceLinks.get(originalTransaction.getSecurityId().toString());
                    txn.setSecurityId(lin.getLocalId());
                } else {
                    log.warn("Investment transaction without security ID. ID:" + txn.getId());
                }
            } catch (IOException iox) {
                log.error("Error getting investment transaction details", iox);
            }
        }

        // Currencies
        if (originalTransaction.getUserCurrencyId() != null) {
            Currency c = currencies.get(originalTransaction.getUserCurrencyId());
            if (c != null) {
                txn.setCurrencyCode(c.getIsoCode());
            }
        }

        // Investment amount bases for txns that include FX
        txn.setAmountBase(originalTransaction.getAmountBase());
        txn.setRateToBase(originalTransaction.getRateToBase());

        txn.setVoided(originalTransaction.isVoid());

        if (originalTransaction.getInvestmentActivity() != null) {
            txn.setInvestmentActivityType(originalTransaction.getInvestmentActivity().getFlag());
        }
        // 0 == not cleared
        // 1 == cleared
        // 2 == reconciled
        if (originalTransaction.getClearedState() == 1 || originalTransaction.getClearedState() == 2) {
            txn.setCleared(true);
        }

        if (originalTransaction.getFrequency() != null) {
            txn.setRecurring(originalTransaction.getFrequency().isRecurring());
        }

        txn.setSerialDateTime(ZonedDateTime.ofInstant(originalTransaction.getSerialDate().toInstant(), ZoneId.systemDefault()));
    }

    private LoanImportResult processLoanSplitTransactions(
        User user,
        FinanceImportStatus fiStatus,
        com.le.sunriise.mnyobject.Transaction originalTxn,
        Source source,
        Map<String, SourceLink> sourceLinkCache,
        Map<String, SourceLink> transferSourceLinks,
        Map<String, SourceLink> accountCache,
        Map<String, SourceLink> payeeSourceLinks,
        Map<String, SourceLink> categoriesSourceLinks,
        Map<String, SourceLink> securitySourceLinks,
        OpenedDb oDB,
        Map<Integer, Currency> currencies,
        Map<Integer, Integer> pendingSplits,
        Map<Integer, Integer> transferLinksByFromId,
        Map<Integer, com.le.sunriise.mnyobject.Transaction> splitParentsByChildId
    ) {
        LoanSplitComponents loanSplit = identifyLoanSplit(originalTxn, accountCache, transferLinksByFromId, splitParentsByChildId);
        if (loanSplit == null) {
            return null;
        }

        removeLegacyLoanSplitTransactions(user, originalTxn, sourceLinkCache);

        String transferSourceId = buildDerivedTransactionSourceId(originalTxn.getId(), LOAN_TRANSFER_SUFFIX);
        String interestSourceId = buildDerivedTransactionSourceId(originalTxn.getId(), LOAN_INTEREST_SUFFIX);
        String baseGuid = trimSguid(originalTxn.getGuid());
        String transferGuid = buildDerivedTransactionSourceGuid(baseGuid, LOAN_TRANSFER_SUFFIX);
        String interestGuid = buildDerivedTransactionSourceGuid(baseGuid, LOAN_INTEREST_SUFFIX);

        FinanceTransaction transferTxn = resolveImportedTransaction(user, sourceLinkCache.get(transferSourceId), transferGuid, fiStatus);
        updateTransaction(
            transferTxn,
            originalTxn,
            accountCache,
            payeeSourceLinks,
            categoriesSourceLinks,
            securitySourceLinks,
            oDB,
            currencies,
            pendingSplits
        );
        transferTxn.setMasterGuid(transferGuid);
        transferTxn.setCategory(null);
        transferTxn.setWho(null);
        transferTxn.setAmount(loanSplit.totalAmount);
        transferTxn.setTransferredAccountId(loanSplit.loanAccountId);
        transferTxn.setTransfer(true);
        transferTxn.setTransferTo(loanSplit.totalAmount != null && loanSplit.totalAmount.signum() > 0);
        transferTxn.setSplitChild(false);
        transferTxn.setSplitParent(false);
        transferTxn.setParentId(null);
        transferTxn.setPrincipalAmount(loanSplit.principalAmount);

        FinanceTransaction loanCounterpartTxn = resolveImportedTransaction(
            user,
            sourceLinkCache.get(loanSplit.loanCounterpartSourceId.toString()),
            buildDerivedTransactionSourceGuid(baseGuid, "loan-counterpart"),
            fiStatus
        );
        configureImportedLoanTransferCounterpart(loanCounterpartTxn, transferTxn, loanSplit.loanAccountId);
        loanCounterpartTxn.setMasterGuid(buildDerivedTransactionSourceGuid(baseGuid, "loan-counterpart"));
        loanCounterpartTxn.setPrincipalAmount(null);

        FinanceTransaction interestTxn = resolveImportedTransaction(user, sourceLinkCache.get(interestSourceId), interestGuid, fiStatus);
        updateTransaction(
            interestTxn,
            loanSplit.interestTransaction,
            accountCache,
            payeeSourceLinks,
            categoriesSourceLinks,
            securitySourceLinks,
            oDB,
            currencies,
            pendingSplits
        );
        interestTxn.setMasterGuid(interestGuid);
        interestTxn.setAccountId(loanSplit.loanAccountId);
        interestTxn.setTransferredAccountId(null);
        interestTxn.setTransfer(false);
        interestTxn.setTransferTo(false);
        interestTxn.setSplitChild(false);
        interestTxn.setSplitParent(false);
        interestTxn.setParentId(null);
        interestTxn.setAmount(loanSplit.adjustedInterestAmount);
        if (loanSplit.interestAdjustmentNote != null) {
            interestTxn.setMemo(appendMemoNote(interestTxn.getMemo(), loanSplit.interestAdjustmentNote));
        }
        interestTxn.setPrincipalAmount(null);

        String transferLinkSourceId = buildTransferSourceId(loanSplit.principalSourceId, loanSplit.loanCounterpartSourceId);
        return new LoanImportResult(
            List.of(
                new ImportedTransactionItem(transferTxn, transferSourceId, transferGuid),
                new ImportedTransactionItem(
                    loanCounterpartTxn,
                    loanSplit.loanCounterpartSourceId.toString(),
                    loanCounterpartTxn.getMasterGuid()
                ),
                new ImportedTransactionItem(interestTxn, interestSourceId, interestGuid)
            ),
            List.of(new ImportedTransferLinkItem(transferTxn, loanCounterpartTxn, transferLinkSourceId)),
            List.of(loanSplit.principalSourceId, loanSplit.interestSourceId, loanSplit.loanCounterpartSourceId)
        );
    }

    private void configureImportedLoanTransferCounterpart(
        FinanceTransaction counterpartTxn,
        FinanceTransaction sourceTransferTxn,
        String loanAccountId
    ) {
        counterpartTxn.setUserGuid(sourceTransferTxn.getUserGuid());
        counterpartTxn.setAccountId(loanAccountId);
        counterpartTxn.setTransferredAccountId(sourceTransferTxn.getAccountId());
        counterpartTxn.setDate(sourceTransferTxn.getDate());
        counterpartTxn.setAmount(sourceTransferTxn.getAmount() == null ? null : sourceTransferTxn.getAmount().negate());
        counterpartTxn.setRecurring(false);
        counterpartTxn.setTransfer(true);
        counterpartTxn.setTransferTo(sourceTransferTxn.getAmount() != null && sourceTransferTxn.getAmount().signum() > 0);
        counterpartTxn.setSplitParent(false);
        counterpartTxn.setSplitChild(false);
        counterpartTxn.setParentId(null);
        counterpartTxn.setVoided(false);
        counterpartTxn.setInvestment(false);
        counterpartTxn.setCleared(sourceTransferTxn.isCleared());
        counterpartTxn.setReconciled(sourceTransferTxn.isReconciled());
        counterpartTxn.setMemo(sourceTransferTxn.getMemo());
        counterpartTxn.setCategory(null);
        counterpartTxn.setWho(null);
        counterpartTxn.setPayeeId(sourceTransferTxn.getPayeeId());
        counterpartTxn.setPayeeName(sourceTransferTxn.getPayeeName());
        counterpartTxn.setStatementId(null);
        counterpartTxn.setNumber(sourceTransferTxn.getNumber());
        counterpartTxn.setCurrencyCode(
            accountRepository
                .findById(UUID.fromString(loanAccountId))
                .map(FinanceAccount::getCurrencyCode)
                .orElse(sourceTransferTxn.getCurrencyCode())
        );
        counterpartTxn.setAmountBase(null);
        counterpartTxn.setRateToBase(null);
        counterpartTxn.setSourcePayeeId(sourceTransferTxn.getSourcePayeeId());
        counterpartTxn.setSerialDateTime(sourceTransferTxn.getSerialDateTime());
    }

    private void removeLegacyLoanSplitTransactions(
        User user,
        com.le.sunriise.mnyobject.Transaction originalTxn,
        Map<String, SourceLink> sourceLinkCache
    ) {
        List<String> legacySourceIds = new ArrayList<>();
        legacySourceIds.add(originalTxn.getId().toString());
        for (TransactionSplit split : originalTxn.getSplits()) {
            if (split.getTransaction() != null) {
                legacySourceIds.add(split.getTransaction().getId().toString());
            }
        }

        List<UUID> transactionIdsToDelete = new ArrayList<>();
        List<Long> sourceLinkIdsToDelete = new ArrayList<>();
        for (String legacySourceId : legacySourceIds) {
            SourceLink sourceLink = sourceLinkCache.remove(legacySourceId);
            if (sourceLink == null) {
                continue;
            }

            transactionIdsToDelete.add(UUID.fromString(sourceLink.getLocalId()));
            sourceLinkIdsToDelete.add(sourceLink.getId());
        }

        if (transactionIdsToDelete.isEmpty()) {
            return;
        }

        deleteTransferLinksForTransactions(user, transactionIdsToDelete);
        transactionRepository.deleteAllById(transactionIdsToDelete);
        sourceLinkRepository.deleteAllById(sourceLinkIdsToDelete);
    }

    private FinanceTransaction resolveImportedTransaction(
        User user,
        SourceLink sourceLink,
        String masterGuid,
        FinanceImportStatus fiStatus
    ) {
        if (sourceLink != null) {
            Optional<FinanceTransaction> existingTransaction = transactionRepository.findById(UUID.fromString(sourceLink.getLocalId()));
            if (existingTransaction.isPresent()) {
                fiStatus.setNumUpdated(fiStatus.getNumUpdated() + 1);
                return existingTransaction.get();
            }
        }

        FinanceTransaction transaction = new FinanceTransaction();
        transaction.setUserGuid(user.getGuid().toString());
        transaction.setMasterGuid(masterGuid);
        fiStatus.setNumCreated(fiStatus.getNumCreated() + 1);
        return transaction;
    }

    private LoanSplitComponents identifyLoanSplit(
        com.le.sunriise.mnyobject.Transaction originalTxn,
        Map<String, SourceLink> accountCache,
        Map<Integer, Integer> transferLinksByFromId,
        Map<Integer, com.le.sunriise.mnyobject.Transaction> splitParentsByChildId
    ) {
        if (originalTxn.getSplits() == null || originalTxn.getSplits().size() != 2) {
            return null;
        }

        if (originalTxn.isDefPmt()) {
            return null;
        }

        com.le.sunriise.mnyobject.Transaction principalTransaction = null;
        com.le.sunriise.mnyobject.Transaction interestTransaction = null;
        String loanAccountId = null;
        BigDecimal principalAmount = null;
        BigDecimal interestAmount = null;

        for (TransactionSplit split : originalTxn.getSplits()) {
            com.le.sunriise.mnyobject.Transaction splitTransaction = split.getTransaction();
            if (splitTransaction == null) {
                return null;
            }
            if ("Principal".equals(splitTransaction.getMemo()) && splitTransaction.getTransferredAccountId() != null) {
                SourceLink transferAccountLink = accountCache.get(splitTransaction.getTransferredAccountId().toString());
                if (transferAccountLink != null && isLoanAccount(transferAccountLink.getLocalId())) {
                    principalTransaction = splitTransaction;
                    loanAccountId = transferAccountLink.getLocalId();
                    principalAmount = splitTransaction.getAmount();
                }
            } else if (
                "Interest".equals(splitTransaction.getMemo()) &&
                splitTransaction.getAmount() != null &&
                splitTransaction.getAmount().signum() <= 0
            ) {
                interestTransaction = splitTransaction;
                interestAmount = splitTransaction.getAmount();
            }
        }

        if (
            principalTransaction == null ||
            interestTransaction == null ||
            loanAccountId == null ||
            principalAmount == null ||
            interestAmount == null
        ) {
            return null;
        }

        Integer loanCounterpartSourceId = resolveLoanCounterpartSourceId(principalTransaction, transferLinksByFromId);
        if (loanCounterpartSourceId == null) {
            log.warn(
                "Loan split principal transaction {} does not have a transfer link counterpart in either direction",
                principalTransaction.getId()
            );
            return null;
        }

        BigDecimal principalAbs = principalAmount.abs();
        BigDecimal interestAbs = interestAmount.abs();
        BigDecimal totalAbs = principalAbs.add(interestAbs);
        BigDecimal adjustedInterestAmount = interestAmount;
        String interestAdjustmentNote = null;

        if (originalTxn.getAmount() != null) {
            BigDecimal overallAbs = originalTxn.getAmount().abs();
            if (overallAbs.compareTo(totalAbs) != 0 && overallAbs.compareTo(principalAbs) >= 0) {
                BigDecimal adjustedInterestAbs = overallAbs.subtract(principalAbs);
                adjustedInterestAmount = interestAmount.signum() < 0 ? adjustedInterestAbs.negate() : adjustedInterestAbs;
                totalAbs = overallAbs;
                if (adjustedInterestAmount.compareTo(interestAmount) != 0) {
                    interestAdjustmentNote =
                        "Adjusted interest from source " + interestAmount + " to " + adjustedInterestAmount + " using total less principal";
                }
            }
        }

        BigDecimal totalAmount = totalAbs;
        if (principalAmount.signum() < 0) {
            totalAmount = totalAbs.negate();
        }

        return new LoanSplitComponents(
            loanAccountId,
            principalAmount,
            totalAmount,
            adjustedInterestAmount,
            interestAdjustmentNote,
            interestTransaction,
            principalTransaction.getId(),
            interestTransaction.getId(),
            loanCounterpartSourceId
        );
    }

    private Set<Integer> identifyLoanTransferCounterpartsToSkip(
        Map<Integer, com.le.sunriise.mnyobject.Transaction> originalTxnsById,
        Map<Integer, com.le.sunriise.mnyobject.Transaction> splitParentsByChildId,
        Map<Integer, Integer> transferLinksByFromId,
        Map<String, SourceLink> accountCache
    ) {
        Set<Integer> skippedSourceTxnIds = new HashSet<>();
        for (Map.Entry<Integer, Integer> transferLinkEntry : transferLinksByFromId.entrySet()) {
            Integer fromId = transferLinkEntry.getKey();
            Integer linkId = transferLinkEntry.getValue();

            com.le.sunriise.mnyobject.Transaction linkedTransaction = originalTxnsById.get(linkId);
            if (linkedTransaction == null || !"Principal".equals(linkedTransaction.getMemo())) {
                continue;
            }

            com.le.sunriise.mnyobject.Transaction splitParent = splitParentsByChildId.get(linkId);
            if (splitParent == null) {
                continue;
            }

            LoanSplitComponents loanSplit = identifyLoanSplit(splitParent, accountCache, transferLinksByFromId, splitParentsByChildId);
            if (loanSplit != null) {
                skippedSourceTxnIds.add(fromId);
            }
        }
        return skippedSourceTxnIds;
    }

    private boolean isLoanAccount(String localAccountId) {
        return accountRepository
            .findById(UUID.fromString(localAccountId))
            .map(FinanceAccount::getType)
            .map(type -> type == LOAN_ACCOUNT_TYPE)
            .orElse(false);
    }

    private String buildDerivedTransactionSourceId(Integer sourceId, String suffix) {
        return sourceId + ":" + suffix;
    }

    private String buildDerivedTransactionSourceGuid(String sourceGuid, String suffix) {
        return sourceGuid + ":" + suffix;
    }

    private String getBaseTransactionSourceId(String sourceId) {
        int separator = sourceId.indexOf(':');
        if (separator < 0) {
            return sourceId;
        }
        return sourceId.substring(0, separator);
    }

    private String appendMemoNote(String memo, String note) {
        if (note == null || note.isBlank()) {
            return memo;
        }
        if (memo == null || memo.isBlank()) {
            return note;
        }
        return memo + " [" + note + "]";
    }

    private Integer resolveLoanCounterpartSourceId(
        com.le.sunriise.mnyobject.Transaction principalTransaction,
        Map<Integer, Integer> transferLinksByFromId
    ) {
        boolean transferTo = principalTransaction.getTransactionInfo() != null && principalTransaction.getTransactionInfo().isTransferTo();
        if (transferTo) {
            return transferLinksByFromId.get(principalTransaction.getId());
        }

        for (Map.Entry<Integer, Integer> entry : transferLinksByFromId.entrySet()) {
            if (principalTransaction.getId().equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static class LoanSplitComponents {

        private final String loanAccountId;
        private final BigDecimal principalAmount;
        private final BigDecimal totalAmount;
        private final BigDecimal adjustedInterestAmount;
        private final String interestAdjustmentNote;
        private final com.le.sunriise.mnyobject.Transaction interestTransaction;
        private final Integer principalSourceId;
        private final Integer interestSourceId;
        private final Integer loanCounterpartSourceId;

        private LoanSplitComponents(
            String loanAccountId,
            BigDecimal principalAmount,
            BigDecimal totalAmount,
            BigDecimal adjustedInterestAmount,
            String interestAdjustmentNote,
            com.le.sunriise.mnyobject.Transaction interestTransaction,
            Integer principalSourceId,
            Integer interestSourceId,
            Integer loanCounterpartSourceId
        ) {
            this.loanAccountId = loanAccountId;
            this.principalAmount = principalAmount;
            this.totalAmount = totalAmount;
            this.adjustedInterestAmount = adjustedInterestAmount;
            this.interestAdjustmentNote = interestAdjustmentNote;
            this.interestTransaction = interestTransaction;
            this.principalSourceId = principalSourceId;
            this.interestSourceId = interestSourceId;
            this.loanCounterpartSourceId = loanCounterpartSourceId;
        }
    }

    private static class LoanImportResult {

        private final List<ImportedTransactionItem> transactions;
        private final List<ImportedTransferLinkItem> transferLinks;
        private final List<Integer> sourceTxnIdsToSkip;

        private LoanImportResult(
            List<ImportedTransactionItem> transactions,
            List<ImportedTransferLinkItem> transferLinks,
            List<Integer> sourceTxnIdsToSkip
        ) {
            this.transactions = transactions;
            this.transferLinks = transferLinks;
            this.sourceTxnIdsToSkip = sourceTxnIdsToSkip;
        }
    }

    private FinanceTransaction convertTransaction(
        User user,
        com.le.sunriise.mnyobject.Transaction originalTransaction,
        Map<String, SourceLink> accountCache,
        Map<String, SourceLink> payeeSourceLinks,
        Map<String, SourceLink> categoriesSourceLinks,
        Map<String, SourceLink> securitySourceLinks,
        OpenedDb oDB,
        Map<Integer, Currency> currencies,
        Map<Integer, Integer> pendingSplits
    ) {
        FinanceTransaction txn = new FinanceTransaction();
        String id = originalTransaction.getGuid();
        txn.setMasterGuid(trimSguid(id));
        txn.setUserGuid(user.getGuid().toString());

        updateTransaction(
            txn,
            originalTransaction,
            accountCache,
            payeeSourceLinks,
            categoriesSourceLinks,
            securitySourceLinks,
            oDB,
            currencies,
            pendingSplits
        );
        return txn;
    }

    public Map<String, FinanceCurrency> getCurrenciesFromDB() {
        List<FinanceCurrency> c = currencyRepository.findAll();
        Map<String, FinanceCurrency> currencyCache = new HashMap<String, FinanceCurrency>();
        for (FinanceCurrency fc : c) {
            currencyCache.put(fc.getId().toString(), fc);
        }

        return currencyCache;
    }

    public Map<String, FinanceUserSecurity> getUserSecuritiesFromDB(User user) {
        List<FinanceUserSecurity> c = userSecurityRepository.findByUserGuid(user.getGuid().toString());
        Map<String, FinanceUserSecurity> cache = new HashMap<String, FinanceUserSecurity>();
        for (FinanceUserSecurity fc : c) {
            cache.put(fc.getId().toString(), fc);
        }

        return cache;
    }

    public void invalidateCachedFinancialEvents(User user) {
        List<FinanceUserSecurity> c = userSecurityRepository.findByUserGuid(user.getGuid().toString());
        for (FinanceUserSecurity fc : c) {
            fc.setEventsValid(false);
        }
        userSecurityRepository.saveAll(c);
    }
}
