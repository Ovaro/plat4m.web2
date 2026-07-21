package ovaro.plat4m.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.config.ApplicationProperties;
import ovaro.plat4m.domain.FinanceSecurity;
import ovaro.plat4m.domain.FinanceSecurityInvestmentSummary;
import ovaro.plat4m.domain.FinanceSecurityPrice;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceSecurityPriceRepository;
import ovaro.plat4m.repository.UserRepository;
import ovaro.plat4m.service.AIService.AiServiceCallType;
import ovaro.plat4m.service.dto.FinanceSecurityPriceRefreshItemDTO;
import ovaro.plat4m.service.dto.FinanceSecurityPriceRefreshRequestDTO;
import ovaro.plat4m.service.dto.FinanceSecurityPriceRefreshResultDTO;

@Service
public class FinanceSecurityPriceRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(FinanceSecurityPriceRefreshService.class);
    private static final Duration QUOTE_REFRESH_COOLDOWN = Duration.ofHours(2);

    private final ApplicationProperties applicationProperties;
    private final FinanceSecurityService financeSecurityService;
    private final FinanceTransactionService financeTransactionService;
    private final FinanceSecurityPriceRepository financeSecurityPriceRepository;
    private final FinanceMarketDataService financeMarketDataService;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;
    private final Map<String, RefreshJob> refreshJobs = new ConcurrentHashMap<>();
    private final ExecutorService refreshExecutor = Executors.newFixedThreadPool(2);

    public FinanceSecurityPriceRefreshService(
        ApplicationProperties applicationProperties,
        FinanceSecurityService financeSecurityService,
        FinanceTransactionService financeTransactionService,
        FinanceSecurityPriceRepository financeSecurityPriceRepository,
        FinanceMarketDataService financeMarketDataService,
        UserRepository userRepository,
        AIService aiService,
        ObjectMapper objectMapper
    ) {
        this.applicationProperties = applicationProperties;
        this.financeSecurityService = financeSecurityService;
        this.financeTransactionService = financeTransactionService;
        this.financeSecurityPriceRepository = financeSecurityPriceRepository;
        this.financeMarketDataService = financeMarketDataService;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FinanceSecurityPriceRefreshResultDTO refreshQuotes(User user, FinanceSecurityPriceRefreshRequestDTO request) {
        ensureRefreshConfigured();

        PreparedRefresh preparedRefresh = prepareRefresh(user, request);
        FinanceSecurityPriceRefreshResultDTO result = createInitialResult(null, preparedRefresh.targets().size());
        processTargets(user, result, preparedRefresh.targets(), preparedRefresh.refreshMetadataByTargetId(), 0L, null, true);
        markResultComplete(result, "completed", "Quote refresh completed.");
        return result;
    }

    public FinanceSecurityPriceRefreshResultDTO startRefreshQuotesAsync(User user, FinanceSecurityPriceRefreshRequestDTO request) {
        ensureRefreshConfigured();

        PreparedRefresh preparedRefresh = prepareRefresh(user, request);
        String jobId = UUID.randomUUID().toString();
        FinanceSecurityPriceRefreshResultDTO result = createInitialResult(jobId, preparedRefresh.targets().size());
        result.setStatus("queued");
        result.setCurrentMessage("Queued for refresh.");

        RefreshJob job = new RefreshJob(jobId, user.getGuid().toString(), user, result);
        refreshJobs.put(jobId, job);

        refreshExecutor.submit(() -> runAsyncRefreshJob(job, preparedRefresh));
        return snapshotResult(job);
    }

    public FinanceSecurityPriceRefreshResultDTO getRefreshJobStatus(User user, String jobId) {
        RefreshJob job = requireOwnedJob(user, jobId);
        return snapshotResult(job);
    }

    public FinanceSecurityPriceRefreshResultDTO updateRefreshJobSelection(
        User user,
        String jobId,
        String userSecurityId,
        boolean selected
    ) {
        RefreshJob job = requireOwnedJob(user, jobId);
        synchronized (job.result()) {
            FinanceSecurityPriceRefreshItemDTO item = job.itemByUserSecurityId().get(userSecurityId);
            if (item == null) {
                throw new IllegalArgumentException("Quote refresh item not found.");
            }
            JobItemState state = job.itemStateByUserSecurityId().computeIfAbsent(userSecurityId, ignored -> new JobItemState());

            item.setSelected(selected);
            state.setSelected(selected);
            if (!selected) {
                item.setCanApply(false);
                if (!item.isApplied()) {
                    item.setMessage("Skipped by user.");
                    item.setStatus("skipped");
                }
            } else if (!item.isApplied()) {
                item.setCanApply(resolveBestSnapshot(state) != null);
                if ("skipped".equals(item.getStatus()) && "Skipped by user.".equals(item.getMessage())) {
                    item.setStatus(item.getPrice() != null ? "refreshed" : "pending");
                    item.setMessage(item.getPrice() != null ? "Quote ready to apply." : "Selected for refresh.");
                }
            }
            recalculateApplyState(job.result());
            return snapshotResult(job);
        }
    }

    public FinanceSecurityPriceRefreshResultDTO applyRefreshJobResults(User user, String jobId) {
        RefreshJob job = requireOwnedJob(user, jobId);
        synchronized (job.result()) {
            job.setApplyRequested(true);
            job.result().setApplyRequested(true);
            applyAvailableSelections(job);
            if (job.result().isComplete()) {
                job.result().setCurrentMessage("Selected quotes applied.");
            }
            return snapshotResult(job);
        }
    }

    @Transactional
    public void refreshQuotesForAllUsers() {
        if (!isRefreshConfigured()) {
            LOG.debug("Skipping scheduled quote refresh because market data is disabled or not configured.");
            return;
        }

        for (User user : userRepository.findAllByActivatedIsTrue()) {
            try {
                FinanceSecurityPriceRefreshResultDTO result = refreshQuotes(user, new FinanceSecurityPriceRefreshRequestDTO());
                LOG.info(
                    "Scheduled quote refresh completed for user {}. requested={}, refreshed={}, skipped={}, failed={}",
                    user.getLogin(),
                    result.getRequestedCount(),
                    result.getRefreshedCount(),
                    result.getSkippedCount(),
                    result.getFailedCount()
                );
            } catch (RuntimeException e) {
                LOG.error("Scheduled quote refresh failed for user {}", user.getLogin(), e);
            }
        }
    }

    public boolean isRefreshConfigured() {
        return (
            applicationProperties.getMarketData().isEnabled() &&
            applicationProperties.getMarketData().getTwelveData().getApiKey() != null &&
            !applicationProperties.getMarketData().getTwelveData().getApiKey().isBlank()
        );
    }

    private void ensureRefreshConfigured() {
        if (!applicationProperties.getMarketData().isEnabled()) {
            throw new IllegalStateException("Market data refresh is disabled.");
        }
        if (
            applicationProperties.getMarketData().getTwelveData().getApiKey() == null ||
            applicationProperties.getMarketData().getTwelveData().getApiKey().isBlank()
        ) {
            throw new IllegalStateException("Twelve Data API key is not configured.");
        }
    }

    private List<FinanceUserSecurity> resolveTargets(User user, String userSecurityId, String accountId) {
        if (userSecurityId != null) {
            FinanceUserSecurity userSecurity = financeSecurityService
                .getUserSecurity(user, userSecurityId)
                .orElseThrow(() -> new IllegalArgumentException("Investment not found"));
            return List.of(userSecurity);
        }
        return financeSecurityService.getUserSecurities(user, accountId);
    }

    private PreparedRefresh prepareRefresh(User user, FinanceSecurityPriceRefreshRequestDTO request) {
        String requestedUserSecurityId = normalize(request != null ? request.getUserSecurityId() : null);
        String requestedAccountId = normalize(request != null ? request.getAccountId() : null);
        Map<String, RefreshTargetMetadata> refreshMetadataByTargetId = resolveRefreshMetadata(user, requestedAccountId);
        List<FinanceUserSecurity> targets = resolveTargets(user, requestedUserSecurityId, requestedAccountId)
            .stream()
            .filter(target -> refreshMetadataByTargetId.containsKey(target.getId() != null ? target.getId().toString() : null))
            .toList();

        Map<String, FinanceUserSecurity> uniqueTargets = new LinkedHashMap<>();
        for (FinanceUserSecurity target : targets) {
            uniqueTargets.putIfAbsent(buildTargetKey(target), target);
        }

        return new PreparedRefresh(new ArrayList<>(uniqueTargets.values()), refreshMetadataByTargetId);
    }

    private FinanceSecurityPriceRefreshResultDTO createInitialResult(String jobId, int requestedCount) {
        FinanceSecurityPriceRefreshResultDTO result = new FinanceSecurityPriceRefreshResultDTO();
        result.setJobId(jobId);
        result.setRequestedCount(requestedCount);
        result.setProcessedCount(0);
        result.setStatus("running");
        result.setComplete(false);
        result.setStartedAtEpochMs(System.currentTimeMillis());
        result.setCurrentMessage("Preparing refresh.");
        return result;
    }

    private void runAsyncRefreshJob(RefreshJob job, PreparedRefresh preparedRefresh) {
        FinanceSecurityPriceRefreshResultDTO result = job.result();
        synchronized (result) {
            result.setStatus("running");
            result.setCurrentMessage("Refreshing quotes.");
        }

        try {
            processTargets(
                job.user(),
                result,
                preparedRefresh.targets(),
                preparedRefresh.refreshMetadataByTargetId(),
                applicationProperties.getMarketData().getQuoteRefresh().getRequestDelayMs(),
                job,
                true
            );
            synchronized (result) {
                if (job.isApplyRequested()) {
                    applyAvailableSelections(job);
                }
                markResultComplete(result, "completed", "Quote refresh completed.");
            }
        } catch (RuntimeException e) {
            LOG.error("Async quote refresh job {} failed", job.jobId(), e);
            synchronized (result) {
                result.setStatus("failed");
                result.setComplete(true);
                result.setCurrentMessage(e.getMessage());
                result.setCompletedAtEpochMs(System.currentTimeMillis());
            }
        }
    }

    private void processTargets(
        User user,
        FinanceSecurityPriceRefreshResultDTO result,
        List<FinanceUserSecurity> targets,
        Map<String, RefreshTargetMetadata> refreshMetadataByTargetId,
        long delayMs,
        RefreshJob job,
        boolean autoApply
    ) {
        synchronized (result) {
            result.setCurrentMessage("Requesting AI quotes.");
        }
        Map<String, FinanceMarketDataService.MarketQuoteSnapshot> aiQuotesByTargetId = fetchAiPreviousCloses(
            user,
            targets,
            refreshMetadataByTargetId
        );

        Map<String, FinanceSecurityPriceRefreshItemDTO> itemsByTargetId = new LinkedHashMap<>();
        List<FinanceSecurityPriceRefreshItemDTO> items = result.getItems();
        synchronized (result) {
            items.clear();
            if (job != null) {
                job.itemByUserSecurityId().clear();
                job.itemStateByUserSecurityId().clear();
            }
            for (FinanceUserSecurity target : targets) {
                String targetId = target.getId() != null ? target.getId().toString() : null;
                RefreshTargetMetadata metadata = refreshMetadataByTargetId.get(targetId);
                FinanceSecurityPriceRefreshItemDTO item = createPendingItem(target, metadata, aiQuotesByTargetId.get(targetId));
                items.add(item);
                if (targetId != null) {
                    itemsByTargetId.put(targetId, item);
                }
                if (job != null && item.getUserSecurityId() != null) {
                    job.itemByUserSecurityId().put(item.getUserSecurityId(), item);
                    JobItemState state = new JobItemState();
                    state.setSelected(true);
                    state.setAiSnapshot(aiQuotesByTargetId.get(targetId));
                    job.itemStateByUserSecurityId().put(item.getUserSecurityId(), state);
                    item.setCanApply(resolveBestSnapshot(state) != null);
                }
            }
            recalculateApplyState(result);
            result.setCurrentMessage("AI quotes loaded. Waiting on Twelve Data confirmations.");
        }

        for (int i = 0; i < targets.size(); i++) {
            FinanceUserSecurity target = targets.get(i);
            String targetId = target.getId() != null ? target.getId().toString() : null;
            RefreshTargetMetadata metadata = refreshMetadataByTargetId.get(targetId);
            FinanceSecurityPriceRefreshItemDTO item = itemsByTargetId.get(targetId);
            if (item != null && shouldSkipSelectedItem(job, item.getUserSecurityId())) {
                synchronized (result) {
                    markUserSkipped(item);
                    result.setProcessedCount(result.getProcessedCount() + 1);
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    recalculateApplyState(result);
                    result.setCurrentMessage(
                        "Processed " + result.getProcessedCount() + " of " + result.getRequestedCount() + " holdings."
                    );
                }
                continue;
            }
            if (item != null && "skipped".equals(item.getStatus())) {
                synchronized (result) {
                    result.setProcessedCount(result.getProcessedCount() + 1);
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    recalculateApplyState(result);
                    result.setCurrentMessage(
                        "Processed " + result.getProcessedCount() + " of " + result.getRequestedCount() + " holdings."
                    );
                }
                continue;
            }
            synchronized (result) {
                result.setCurrentSymbol(resolveProviderSymbol(target, metadata));
                result.setCurrentMessage("Refreshing " + (result.getCurrentSymbol() != null ? result.getCurrentSymbol() : "holding") + ".");
            }

            item = refreshSingleTarget(target, metadata, aiQuotesByTargetId.get(targetId), item, job, autoApply);
            synchronized (result) {
                result.setProcessedCount(result.getProcessedCount() + 1);
                switch (item.getStatus()) {
                    case "refreshed" -> result.setRefreshedCount(result.getRefreshedCount() + 1);
                    case "failed" -> result.setFailedCount(result.getFailedCount() + 1);
                    default -> result.setSkippedCount(result.getSkippedCount() + 1);
                }
                recalculateApplyState(result);
                result.setCurrentMessage("Processed " + result.getProcessedCount() + " of " + result.getRequestedCount() + " holdings.");
            }

            if (delayMs > 0 && i < targets.size() - 1 && shouldThrottleAfter(item)) {
                sleep(delayMs);
            }
        }
    }

    private boolean shouldThrottleAfter(FinanceSecurityPriceRefreshItemDTO item) {
        return item.getRequestedSymbol() != null && !"skipped".equals(item.getStatus());
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Quote refresh interrupted.", e);
        }
    }

    private void markResultComplete(FinanceSecurityPriceRefreshResultDTO result, String status, String message) {
        result.setStatus(status);
        result.setComplete(true);
        result.setCurrentSymbol(null);
        result.setCurrentMessage(message);
        result.setCompletedAtEpochMs(System.currentTimeMillis());
    }

    private RefreshJob requireOwnedJob(User user, String jobId) {
        RefreshJob job = refreshJobs.get(jobId);
        if (job == null || !job.userGuid().equals(user.getGuid().toString())) {
            throw new IllegalArgumentException("Quote refresh job not found.");
        }
        return job;
    }

    private FinanceSecurityPriceRefreshResultDTO snapshotResult(RefreshJob job) {
        FinanceSecurityPriceRefreshResultDTO source = job.result();
        synchronized (source) {
            FinanceSecurityPriceRefreshResultDTO copy = new FinanceSecurityPriceRefreshResultDTO();
            copy.setJobId(source.getJobId());
            copy.setStatus(source.getStatus());
            copy.setCurrentSymbol(source.getCurrentSymbol());
            copy.setCurrentMessage(source.getCurrentMessage());
            copy.setComplete(source.isComplete());
            copy.setStartedAtEpochMs(source.getStartedAtEpochMs());
            copy.setCompletedAtEpochMs(source.getCompletedAtEpochMs());
            copy.setRequestedCount(source.getRequestedCount());
            copy.setProcessedCount(source.getProcessedCount());
            copy.setRefreshedCount(source.getRefreshedCount());
            copy.setSkippedCount(source.getSkippedCount());
            copy.setFailedCount(source.getFailedCount());
            copy.setAppliedCount(source.getAppliedCount());
            copy.setApplyRequested(source.isApplyRequested());

            List<FinanceSecurityPriceRefreshItemDTO> itemCopies = new ArrayList<>();
            for (FinanceSecurityPriceRefreshItemDTO item : source.getItems()) {
                itemCopies.add(copyItem(item));
            }
            copy.setItems(itemCopies);
            return copy;
        }
    }

    private FinanceSecurityPriceRefreshItemDTO copyItem(FinanceSecurityPriceRefreshItemDTO item) {
        FinanceSecurityPriceRefreshItemDTO copy = new FinanceSecurityPriceRefreshItemDTO();
        copy.setUserSecurityId(item.getUserSecurityId());
        copy.setSymbol(item.getSymbol());
        copy.setRequestedSymbol(item.getRequestedSymbol());
        copy.setRequestedExchangeMic(item.getRequestedExchangeMic());
        copy.setCurrencyCode(item.getCurrencyCode());
        copy.setSelected(item.isSelected());
        copy.setApplied(item.isApplied());
        copy.setCanApply(item.isCanApply());
        copy.setAppliedSource(item.getAppliedSource());
        copy.setStatus(item.getStatus());
        copy.setMessage(item.getMessage());
        copy.setPriceDate(item.getPriceDate());
        copy.setPrice(item.getPrice());
        copy.setAiPriceDate(item.getAiPriceDate());
        copy.setAiPrice(item.getAiPrice());
        copy.setAiMessage(item.getAiMessage());
        copy.setTwelveDataPriceDate(item.getTwelveDataPriceDate());
        copy.setTwelveDataPrice(item.getTwelveDataPrice());
        copy.setTwelveDataMessage(item.getTwelveDataMessage());
        copy.setPreviousPrice(item.getPreviousPrice());
        copy.setPriceDeltaValue(item.getPriceDeltaValue());
        copy.setPriceDeltaPercent(item.getPriceDeltaPercent());
        return copy;
    }

    private boolean shouldSkipSelectedItem(RefreshJob job, String userSecurityId) {
        if (job == null || userSecurityId == null) {
            return false;
        }
        synchronized (job.result()) {
            JobItemState state = job.itemStateByUserSecurityId().get(userSecurityId);
            return state != null && !state.isSelected();
        }
    }

    private void markUserSkipped(FinanceSecurityPriceRefreshItemDTO item) {
        item.setSelected(false);
        item.setCanApply(false);
        if (!item.isApplied()) {
            item.setStatus("skipped");
        }
        item.setMessage("Skipped by user.");
    }

    private void rememberAiSnapshot(
        RefreshJob job,
        FinanceSecurityPriceRefreshItemDTO item,
        FinanceMarketDataService.MarketQuoteSnapshot snapshot
    ) {
        if (job == null || item.getUserSecurityId() == null) {
            return;
        }
        JobItemState state = job.itemStateByUserSecurityId().computeIfAbsent(item.getUserSecurityId(), ignored -> new JobItemState());
        state.setAiSnapshot(snapshot);
    }

    private void rememberTwelveDataSnapshot(
        RefreshJob job,
        FinanceSecurityPriceRefreshItemDTO item,
        FinanceMarketDataService.MarketQuoteSnapshot snapshot
    ) {
        if (job == null || item.getUserSecurityId() == null) {
            return;
        }
        JobItemState state = job.itemStateByUserSecurityId().computeIfAbsent(item.getUserSecurityId(), ignored -> new JobItemState());
        state.setTwelveDataSnapshot(snapshot);
    }

    private FinanceMarketDataService.MarketQuoteSnapshot resolveBestSnapshot(RefreshJob job, String userSecurityId) {
        if (job == null || userSecurityId == null) {
            return null;
        }
        JobItemState state = job.itemStateByUserSecurityId().get(userSecurityId);
        return resolveBestSnapshot(state);
    }

    private FinanceMarketDataService.MarketQuoteSnapshot resolveBestSnapshot(JobItemState state) {
        if (state == null) {
            return null;
        }
        return state.getTwelveDataSnapshot() != null ? state.getTwelveDataSnapshot() : state.getAiSnapshot();
    }

    private void maybeApplyQuote(
        RefreshJob job,
        FinanceSecurityPriceRefreshItemDTO item,
        boolean autoApply,
        FinanceMarketDataService.MarketQuoteSnapshot snapshot,
        String source
    ) {
        if (snapshot == null || !item.isSelected()) {
            return;
        }
        if (autoApply) {
            applySnapshot(item, snapshot, source);
            return;
        }
        if (job == null || !job.isApplyRequested()) {
            return;
        }
        applySnapshot(item, snapshot, source);
    }

    private void applyAvailableSelections(RefreshJob job) {
        for (Map.Entry<String, FinanceSecurityPriceRefreshItemDTO> entry : job.itemByUserSecurityId().entrySet()) {
            FinanceSecurityPriceRefreshItemDTO item = entry.getValue();
            if (!item.isSelected()) {
                continue;
            }
            JobItemState state = job.itemStateByUserSecurityId().get(entry.getKey());
            FinanceMarketDataService.MarketQuoteSnapshot snapshot = resolveBestSnapshot(state);
            if (snapshot == null) {
                continue;
            }
            applySnapshot(item, snapshot, state != null && snapshot == state.getTwelveDataSnapshot() ? "Twelve Data" : "AI");
        }
        recalculateApplyState(job.result());
    }

    private void applySnapshot(
        FinanceSecurityPriceRefreshItemDTO item,
        FinanceMarketDataService.MarketQuoteSnapshot snapshot,
        String source
    ) {
        FinanceSecurityPrice savedPrice = upsertPrice(snapshot);
        item.setApplied(true);
        item.setAppliedSource(source);
        item.setCanApply(true);
        item.setPriceDate(savedPrice.getDate() != null ? savedPrice.getDate().toLocalDate() : item.getPriceDate());
        item.setPrice(savedPrice.getPrice());
    }

    private void recalculateApplyState(FinanceSecurityPriceRefreshResultDTO result) {
        int appliedCount = 0;
        for (FinanceSecurityPriceRefreshItemDTO item : result.getItems()) {
            if (item.isApplied()) {
                appliedCount++;
            }
        }
        result.setAppliedCount(appliedCount);
    }

    private FinanceSecurityPriceRefreshItemDTO createPendingItem(
        FinanceUserSecurity userSecurity,
        RefreshTargetMetadata metadata,
        FinanceMarketDataService.MarketQuoteSnapshot aiQuote
    ) {
        FinanceSecurityPriceRefreshItemDTO item = baseItem(userSecurity, metadata);
        String storageSymbol = resolveStorageSymbol(userSecurity, metadata);
        String providerSymbol = resolveProviderSymbol(userSecurity, metadata);
        FinanceSecurity providerSecurity = buildProviderSecurity(userSecurity, metadata, providerSymbol);
        item.setRequestedSymbol(providerSymbol);
        item.setRequestedExchangeMic(normalize(providerSecurity.getExchangeMic()));
        FinanceSecurityPrice latestPrice = resolveLatestPrice(storageSymbol);
        item.setPreviousPrice(latestPrice != null ? latestPrice.getPrice() : null);
        if (!isEligible(userSecurity, providerSymbol, storageSymbol)) {
            item.setStatus("skipped");
            item.setMessage(buildSkippedEligibilityMessage(userSecurity, providerSymbol, storageSymbol));
            return item;
        }
        if (isRecentlyRefreshed(latestPrice)) {
            item.setStatus("skipped");
            item.setSelected(false);
            item.setCanApply(false);
            item.setMessage("Skipped because this quote was refreshed less than 2 hours ago.");
            return item;
        }

        item.setStatus("pending");
        item.setMessage("Waiting on Twelve Data confirmation.");
        if (aiQuote != null) {
            item.setAiPriceDate(aiQuote.getPriceDate());
            item.setAiPrice(aiQuote.getPrice());
            item.setAiMessage("AI batch quote loaded.");
        } else {
            item.setAiMessage("No AI quote returned.");
        }
        return item;
    }

    private FinanceSecurityPriceRefreshItemDTO refreshSingleTarget(
        FinanceUserSecurity userSecurity,
        RefreshTargetMetadata metadata,
        FinanceMarketDataService.MarketQuoteSnapshot aiQuote,
        FinanceSecurityPriceRefreshItemDTO item,
        RefreshJob job,
        boolean autoApply
    ) {
        String storageSymbol = resolveStorageSymbol(userSecurity, metadata);
        String providerSymbol = resolveProviderSymbol(userSecurity, metadata);
        FinanceSecurity providerSecurity = buildProviderSecurity(userSecurity, metadata, providerSymbol);
        String providerExchangeMic = normalize(providerSecurity.getExchangeMic());
        if (!isEligible(userSecurity, providerSymbol, storageSymbol)) {
            item.setStatus("skipped");
            item.setMessage(buildSkippedEligibilityMessage(userSecurity, providerSymbol, storageSymbol));
            return item;
        }

        try {
            item.setRequestedSymbol(providerSymbol);
            item.setRequestedExchangeMic(providerExchangeMic);
            if (item.getPreviousPrice() == null) {
                item.setPreviousPrice(resolvePreviousPrice(storageSymbol));
            }

            boolean aiApplied = false;
            if (aiQuote != null) {
                FinanceMarketDataService.MarketQuoteSnapshot aiSnapshot = aiQuote;
                aiSnapshot.setSymbol(storageSymbol);
                rememberAiSnapshot(job, item, aiSnapshot);
                item.setStatus("refreshed");
                item.setPriceDate(aiSnapshot.getPriceDate());
                item.setPrice(aiSnapshot.getPrice());
                item.setAiPriceDate(aiSnapshot.getPriceDate());
                item.setAiPrice(aiSnapshot.getPrice());
                item.setAiMessage("AI batch quote loaded.");
                item.setMessage("AI batch quote loaded. Waiting on Twelve Data confirmation.");
                item.setCanApply(true);
                applyDelta(item);
                maybeApplyQuote(job, item, autoApply, aiSnapshot, "AI");
                aiApplied = true;
            } else {
                item.setAiMessage("No AI quote returned.");
            }

            Optional<FinanceMarketDataService.MarketQuoteSnapshot> maybeQuote = financeMarketDataService.fetchPreviousClose(
                providerSecurity
            );
            if (shouldSkipSelectedItem(job, item.getUserSecurityId())) {
                markUserSkipped(item);
                return item;
            }
            if (maybeQuote.isEmpty()) {
                item.setTwelveDataMessage("No end-of-day quote was returned.");
                if (aiApplied) {
                    applyDelta(item);
                    item.setMessage("AI batch quote refreshed. Twelve Data confirmation returned no end-of-day quote.");
                    return item;
                }
                item.setStatus("skipped");
                item.setMessage("No end-of-day quote was returned for requested code " + providerSymbol + ".");
                return item;
            }

            FinanceMarketDataService.MarketQuoteSnapshot quote = maybeQuote.get();
            quote.setSymbol(storageSymbol);
            rememberTwelveDataSnapshot(job, item, quote);
            String quoteSource = quote.getSource() != null && !quote.getSource().isBlank() ? quote.getSource() : "Twelve Data";

            item.setStatus("refreshed");
            item.setPriceDate(quote.getPriceDate());
            item.setPrice(quote.getPrice());
            item.setTwelveDataPriceDate(quote.getPriceDate());
            item.setTwelveDataPrice(quote.getPrice());
            item.setTwelveDataMessage(quoteSource + " confirmed the final quote.");
            item.setCanApply(true);
            applyDelta(item);
            maybeApplyQuote(job, item, autoApply, quote, quoteSource);
            item.setMessage(
                aiApplied ? "AI batch quote refreshed. " + quoteSource + " confirmed the final quote." : "Previous close refreshed."
            );
            return item;
        } catch (RuntimeException e) {
            if ("refreshed".equals(item.getStatus()) && item.getPrice() != null) {
                applyDelta(item);
                item.setTwelveDataMessage(e.getMessage());
                item.setMessage("AI batch quote refreshed. Twelve Data confirmation failed: " + e.getMessage());
                LOG.warn(
                    "Twelve Data confirmation failed after AI batch refresh for userSecurityId={} symbol={}",
                    userSecurity.getId(),
                    providerSymbol,
                    e
                );
                return item;
            }
            item.setStatus("failed");
            item.setTwelveDataMessage(e.getMessage());
            item.setMessage(buildFailedMessage(providerSymbol, e));
            LOG.warn("Quote refresh failed for userSecurityId={} symbol={}", userSecurity.getId(), providerSymbol, e);
            return item;
        }
    }

    private BigDecimal resolvePreviousPrice(String storageSymbol) {
        FinanceSecurityPrice existingPrice = resolveLatestPrice(storageSymbol);
        return existingPrice != null ? existingPrice.getPrice() : null;
    }

    private FinanceSecurityPrice resolveLatestPrice(String storageSymbol) {
        if (storageSymbol == null) {
            return null;
        }
        return financeSecurityPriceRepository.findLatestBySymbol(storageSymbol);
    }

    private boolean isRecentlyRefreshed(FinanceSecurityPrice price) {
        if (price == null || price.getSerialDateTime() == null) {
            return false;
        }
        ZonedDateTime cutoff = ZonedDateTime.now(ZoneOffset.UTC).minus(QUOTE_REFRESH_COOLDOWN);
        return price.getSerialDateTime().isAfter(cutoff);
    }

    private Map<String, RefreshTargetMetadata> resolveRefreshMetadata(User user, String accountId) {
        List<FinanceSecurityInvestmentSummary> summaries =
            accountId != null
                ? financeTransactionService.getFinanceSecurityInvestmentTransactionsForAccount(user, accountId, false)
                : financeTransactionService.getFinanceSecurityInvestmentTransactions(user, false, null);

        Map<String, RefreshTargetMetadata> metadataByTargetId = new LinkedHashMap<>();
        for (FinanceSecurityInvestmentSummary summary : summaries) {
            if (summary.getSecurityId() == null || summary.getSecurityId().isBlank()) {
                continue;
            }
            metadataByTargetId.put(
                summary.getSecurityId(),
                new RefreshTargetMetadata(
                    summary.getSymbol(),
                    summary.getUserSymbol(),
                    summary.getExchangeName(),
                    summary.getExchangeMic(),
                    summary.getCurrencyCode()
                )
            );
        }
        return metadataByTargetId;
    }

    private String buildSkippedEligibilityMessage(FinanceUserSecurity userSecurity, String providerSymbol, String storageSymbol) {
        String requestedCode = providerSymbol != null ? providerSymbol : storageSymbol;
        StringBuilder message = new StringBuilder();
        if (requestedCode != null) {
            message.append("Requested code ").append(requestedCode).append(" skipped. ");
        }
        message.append("Only AUD and USD holdings with a linked security symbol can be refreshed.");

        String currencyCode = resolveCurrency(userSecurity, null);
        if (currencyCode != null || providerSymbol == null) {
            message.append(" Details:");
            if (currencyCode != null) {
                message.append(" currency=").append(currencyCode).append('.');
            }
            if (providerSymbol == null) {
                message.append(currencyCode != null ? " " : " ");
                message.append("No provider code could be resolved.");
            }
        }

        return message.toString();
    }

    private String buildFailedMessage(String providerSymbol, RuntimeException e) {
        String detail = e.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = "Refreshing quote failed.";
        }
        if (providerSymbol == null || providerSymbol.isBlank()) {
            return detail;
        }
        return "Requested code " + providerSymbol + " failed: " + detail;
    }

    private FinanceSecurityPrice upsertPrice(FinanceMarketDataService.MarketQuoteSnapshot quote) {
        ZonedDateTime priceDateTime = quote.getPriceDate().atStartOfDay(ZoneOffset.UTC);
        FinanceSecurityPrice price = financeSecurityPriceRepository
            .findBySymbolAndDate(quote.getSymbol(), priceDateTime)
            .orElseGet(FinanceSecurityPrice::new);

        price.setSymbol(quote.getSymbol());
        price.setDate(priceDateTime);
        price.setSerialDateTime(quote.getFetchedAt() != null ? quote.getFetchedAt() : ZonedDateTime.now(ZoneOffset.UTC));
        price.setPrice(quote.getPrice());
        price.setClose(quote.getClose());
        price.setOpen(quote.getOpen());
        price.setHigh(quote.getHigh());
        price.setLow(quote.getLow());
        price.setVolume(quote.getVolume());
        price.setChange(quote.getChange());
        return financeSecurityPriceRepository.save(price);
    }

    private FinanceSecurityPriceRefreshItemDTO baseItem(FinanceUserSecurity userSecurity, RefreshTargetMetadata metadata) {
        FinanceSecurityPriceRefreshItemDTO item = new FinanceSecurityPriceRefreshItemDTO();
        item.setUserSecurityId(userSecurity.getId() != null ? userSecurity.getId().toString() : null);
        item.setSymbol(resolveStorageSymbol(userSecurity, metadata));
        item.setCurrencyCode(resolveCurrency(userSecurity, metadata));
        item.setSelected(true);
        return item;
    }

    private boolean isEligible(FinanceUserSecurity userSecurity, String providerSymbol, String storageSymbol) {
        String currencyCode = resolveCurrency(userSecurity, null);
        return providerSymbol != null && storageSymbol != null && ("AUD".equals(currencyCode) || "USD".equals(currencyCode));
    }

    private String resolveCurrency(FinanceUserSecurity userSecurity, RefreshTargetMetadata metadata) {
        if (userSecurity.getSecurity() != null && userSecurity.getSecurity().getCurrencyCode() != null) {
            return userSecurity.getSecurity().getCurrencyCode().trim().toUpperCase();
        }
        if (metadata != null && metadata.currencyCode() != null && !metadata.currencyCode().isBlank()) {
            return metadata.currencyCode().trim().toUpperCase();
        }
        if (userSecurity.getCurrencyCode() != null) {
            return userSecurity.getCurrencyCode().trim().toUpperCase();
        }
        return null;
    }

    private String resolveStorageSymbol(FinanceUserSecurity userSecurity, RefreshTargetMetadata metadata) {
        String linkedSymbol = normalizeMarketSymbol(metadata != null ? metadata.linkedSymbol() : null);
        if (linkedSymbol != null) {
            return linkedSymbol;
        }

        String securitySymbol = userSecurity.getSecurity() != null ? normalize(userSecurity.getSecurity().getSymbol()) : null;
        if (securitySymbol != null && !isSyntheticSymbol(securitySymbol)) {
            return securitySymbol;
        }

        String userSymbol = normalizeMarketSymbol(metadata != null ? metadata.userSymbol() : null);
        if (userSymbol != null) {
            return userSymbol;
        }

        return normalizeMarketSymbol(userSecurity.getSymbol());
    }

    private String resolveProviderSymbol(FinanceUserSecurity userSecurity, RefreshTargetMetadata metadata) {
        String securitySymbol = userSecurity.getSecurity() != null ? normalize(userSecurity.getSecurity().getSymbol()) : null;
        String userSymbol = normalizeMarketSymbol(
            metadata != null && metadata.userSymbol() != null ? metadata.userSymbol() : userSecurity.getSymbol()
        );
        String linkedSymbol = normalizeMarketSymbol(metadata != null ? metadata.linkedSymbol() : null);

        if (userSymbol != null && !isSyntheticSymbol(userSymbol)) {
            return userSymbol;
        }

        if (securitySymbol != null && !isSyntheticSymbol(securitySymbol)) {
            return securitySymbol;
        }
        if (linkedSymbol != null && !isSyntheticSymbol(linkedSymbol)) {
            return linkedSymbol;
        }
        return null;
    }

    private FinanceSecurity buildProviderSecurity(FinanceUserSecurity userSecurity, RefreshTargetMetadata metadata, String providerSymbol) {
        FinanceSecurity security = userSecurity.getSecurity();
        FinanceSecurity providerSecurity = new FinanceSecurity();
        providerSecurity.setSymbol(providerSymbol);
        if (security != null) {
            providerSecurity.setExchangeMic(security.getExchangeMic());
            providerSecurity.setExchangeName(security.getExchangeName());
            providerSecurity.setExchangeSuffix(security.getExchangeSuffix());
            providerSecurity.setCurrencyCode(security.getCurrencyCode());
            providerSecurity.setCountry(security.getCountry());
            providerSecurity.setName(security.getName());
        }
        if ((providerSecurity.getExchangeMic() == null || providerSecurity.getExchangeMic().isBlank()) && metadata != null) {
            providerSecurity.setExchangeMic(metadata.exchangeMic());
        }
        if ((providerSecurity.getExchangeName() == null || providerSecurity.getExchangeName().isBlank()) && metadata != null) {
            providerSecurity.setExchangeName(metadata.exchangeName());
        }
        if ((providerSecurity.getCurrencyCode() == null || providerSecurity.getCurrencyCode().isBlank()) && metadata != null) {
            providerSecurity.setCurrencyCode(metadata.currencyCode());
        }
        if ((providerSecurity.getName() == null || providerSecurity.getName().isBlank()) && userSecurity.getName() != null) {
            providerSecurity.setName(userSecurity.getName());
        }
        applyAustralianExchangeDefaults(providerSecurity);
        return providerSecurity;
    }

    private String resolveProviderExchangeMic(FinanceUserSecurity userSecurity, RefreshTargetMetadata metadata) {
        if (userSecurity.getSecurity() != null) {
            String securityMic = normalize(userSecurity.getSecurity().getExchangeMic());
            if (securityMic != null) {
                return securityMic;
            }
        }
        String metadataMic = metadata != null ? normalize(metadata.exchangeMic()) : null;
        if (metadataMic != null) {
            return metadataMic;
        }
        String securityCountry = userSecurity.getSecurity() != null ? normalize(userSecurity.getSecurity().getCountry()) : null;
        if (isAustralianSecurity(securityCountry, resolveCurrency(userSecurity, metadata))) {
            return "XASX";
        }
        return null;
    }

    private void applyAustralianExchangeDefaults(FinanceSecurity security) {
        if (security == null) {
            return;
        }
        if (!isAustralianSecurity(security.getCountry(), security.getCurrencyCode()) || normalize(security.getExchangeMic()) != null) {
            return;
        }
        security.setExchangeMic("XASX");
        if (normalize(security.getExchangeName()) == null) {
            security.setExchangeName("ASX");
        }
        if (normalize(security.getExchangeSuffix()) == null) {
            security.setExchangeSuffix("AX");
        }
    }

    private boolean isAustraliaCountry(String country) {
        return country != null && "AUSTRALIA".equalsIgnoreCase(country.trim());
    }

    private boolean isAustralianSecurity(String country, String currencyCode) {
        return isAustraliaCountry(country) || (currencyCode != null && "AUD".equalsIgnoreCase(currencyCode.trim()));
    }

    private void applyDelta(FinanceSecurityPriceRefreshItemDTO item) {
        if (item.getPrice() == null || item.getPreviousPrice() == null) {
            return;
        }

        item.setPriceDeltaValue(item.getPrice().subtract(item.getPreviousPrice()));
        if (item.getPreviousPrice().compareTo(java.math.BigDecimal.ZERO) != 0) {
            item.setPriceDeltaPercent(
                item
                    .getPriceDeltaValue()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .divide(item.getPreviousPrice(), 6, RoundingMode.HALF_UP)
            );
        }
    }

    private Map<String, FinanceMarketDataService.MarketQuoteSnapshot> fetchAiPreviousCloses(
        User user,
        List<FinanceUserSecurity> targets,
        Map<String, RefreshTargetMetadata> refreshMetadataByTargetId
    ) {
        List<AiQuoteRequest> requests = new ArrayList<>();
        for (FinanceUserSecurity target : targets) {
            String targetId = target.getId() != null ? target.getId().toString() : null;
            RefreshTargetMetadata metadata = refreshMetadataByTargetId.get(targetId);
            String storageSymbol = resolveStorageSymbol(target, metadata);
            String providerSymbol = resolveProviderSymbol(target, metadata);
            if (!isEligible(target, providerSymbol, storageSymbol) || targetId == null) {
                continue;
            }
            if (isRecentlyRefreshed(resolveLatestPrice(storageSymbol))) {
                continue;
            }
            requests.add(new AiQuoteRequest(targetId, buildProviderSecurity(target, metadata, providerSymbol)));
        }

        if (requests.isEmpty()) {
            return Map.of();
        }

        String prompt = buildAiQuoteBatchPrompt(requests);
        Optional<AIService.AiTextResponse> response = aiService.generateText(user, AiServiceCallType.MARKET_DATA, prompt);
        if (response.isEmpty() || response.get().text().isBlank()) {
            return Map.of();
        }

        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(response.get().text()));
            JsonNode quoteNodes = root.path("quotes");
            if ((quoteNodes.isMissingNode() || quoteNodes.isNull()) && root.isArray()) {
                quoteNodes = root;
            } else if (quoteNodes.isMissingNode() || quoteNodes.isNull()) {
                quoteNodes = root.path("items");
            }
            if (!quoteNodes.isArray()) {
                return Map.of();
            }

            Map<String, FinanceSecurity> requestsById = new LinkedHashMap<>();
            for (AiQuoteRequest request : requests) {
                requestsById.put(request.targetId(), request.security());
            }

            Map<String, FinanceMarketDataService.MarketQuoteSnapshot> snapshots = new LinkedHashMap<>();
            for (JsonNode quoteNode : quoteNodes) {
                String requestId = normalize(quoteNode.path("requestId").asText(null));
                if (requestId == null) {
                    continue;
                }
                FinanceSecurity security = requestsById.get(requestId);
                if (security == null) {
                    continue;
                }

                String returnedSymbol = normalizeMarketSymbol(quoteNode.path("symbol").asText(null));
                String requestedSymbol = normalizeMarketSymbol(security.getSymbol());
                if (requestedSymbol != null && returnedSymbol != null && !requestedSymbol.equalsIgnoreCase(returnedSymbol)) {
                    LOG.warn(
                        "Rejecting AI quote because symbol mismatched. requestedSymbol={} returnedSymbol={} requestedMic={}",
                        requestedSymbol,
                        returnedSymbol,
                        security.getExchangeMic()
                    );
                    continue;
                }

                String requestedMic = normalize(security.getExchangeMic());
                String returnedMic = normalize(quoteNode.path("exchangeMic").asText(null));
                if (requestedMic != null) {
                    if (returnedMic == null || !requestedMic.equalsIgnoreCase(returnedMic)) {
                        LOG.warn(
                            "Rejecting AI quote because exchange MIC mismatched. requestedSymbol={} requestedMic={} returnedMic={}",
                            requestedSymbol,
                            requestedMic,
                            returnedMic
                        );
                        continue;
                    }
                }

                BigDecimal price = decimalOrNull(quoteNode.path("price").asText(null));
                LocalDate priceDate = parseDate(quoteNode.path("priceDate").asText(null));
                if (price == null || priceDate == null) {
                    continue;
                }

                FinanceMarketDataService.MarketQuoteSnapshot snapshot = new FinanceMarketDataService.MarketQuoteSnapshot();
                snapshot.setSymbol(security.getSymbol());
                snapshot.setPriceDate(priceDate);
                snapshot.setPrice(price);
                snapshot.setClose(price);
                snapshot.setFetchedAt(ZonedDateTime.now(ZoneOffset.UTC));
                snapshots.put(requestId, snapshot);
            }
            return snapshots;
        } catch (Exception e) {
            LOG.warn("AI batched quote response could not be parsed for {} securities", requests.size(), e);
            return Map.of();
        }
    }

    private String buildAiQuoteBatchPrompt(List<AiQuoteRequest> requests) {
        StringBuilder securities = new StringBuilder();
        for (AiQuoteRequest request : requests) {
            FinanceSecurity security = request.security();
            securities
                .append("- requestId: ")
                .append(request.targetId())
                .append('\n')
                .append("  symbol: ")
                .append(security.getSymbol())
                .append('\n')
                .append("  exchangeMic: ")
                .append(security.getExchangeMic())
                .append('\n')
                .append("  exchangeName: ")
                .append(security.getExchangeName())
                .append('\n')
                .append("  currencyCode: ")
                .append(security.getCurrencyCode())
                .append('\n');
        }

        return """
        Return only JSON. Do not include markdown fences.
        Find the latest available market quote for each exact listed security below.
        Securities:
        %s

        Rules:
        - Use the exact exchange MIC provided for each security.
        - Do not substitute another market, another exchange, ADR, or US listing.
        - If you cannot confirm the quote specifically for a symbol on that MIC, omit that item from the response.
        - Echo the exact requestId, symbol, and exchangeMic you used in the response JSON.
        - Return as many valid quotes as you can in one response.

        Respond exactly as:
        {
          "quotes": [
            {
              "requestId": "string",
              "symbol": "string",
              "exchangeMic": "string",
              "price": 0.0,
              "priceDate": "YYYY-MM-DD"
            }
          ]
        }

        If you are not confident about any of them, return:
        { "quotes": [] }
        """.formatted(securities);
    }

    private String stripCodeFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private BigDecimal decimalOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.trim().substring(0, 10));
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
        if (normalized == null) {
            return true;
        }
        return normalized.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:.+");
    }

    private String buildTargetKey(FinanceUserSecurity userSecurity) {
        FinanceSecurity security = userSecurity.getSecurity();
        String symbol = resolveStorageSymbol(userSecurity, null);
        String mic = security != null ? normalize(security.getExchangeMic()) : null;
        return (symbol == null ? "" : symbol) + "|" + (mic == null ? "" : mic);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record RefreshTargetMetadata(
        String linkedSymbol,
        String userSymbol,
        String exchangeName,
        String exchangeMic,
        String currencyCode
    ) {}

    private record PreparedRefresh(List<FinanceUserSecurity> targets, Map<String, RefreshTargetMetadata> refreshMetadataByTargetId) {}

    private record AiQuoteRequest(String targetId, FinanceSecurity security) {}

    private static final class JobItemState {

        private boolean selected = true;
        private FinanceMarketDataService.MarketQuoteSnapshot aiSnapshot;
        private FinanceMarketDataService.MarketQuoteSnapshot twelveDataSnapshot;

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public FinanceMarketDataService.MarketQuoteSnapshot getAiSnapshot() {
            return aiSnapshot;
        }

        public void setAiSnapshot(FinanceMarketDataService.MarketQuoteSnapshot aiSnapshot) {
            this.aiSnapshot = aiSnapshot;
        }

        public FinanceMarketDataService.MarketQuoteSnapshot getTwelveDataSnapshot() {
            return twelveDataSnapshot;
        }

        public void setTwelveDataSnapshot(FinanceMarketDataService.MarketQuoteSnapshot twelveDataSnapshot) {
            this.twelveDataSnapshot = twelveDataSnapshot;
        }
    }

    private static final class RefreshJob {

        private final String jobId;
        private final String userGuid;
        private final User user;
        private final FinanceSecurityPriceRefreshResultDTO result;
        private final Map<String, FinanceSecurityPriceRefreshItemDTO> itemByUserSecurityId = new LinkedHashMap<>();
        private final Map<String, JobItemState> itemStateByUserSecurityId = new LinkedHashMap<>();
        private boolean applyRequested;

        private RefreshJob(String jobId, String userGuid, User user, FinanceSecurityPriceRefreshResultDTO result) {
            this.jobId = jobId;
            this.userGuid = userGuid;
            this.user = user;
            this.result = result;
        }

        public String jobId() {
            return jobId;
        }

        public String userGuid() {
            return userGuid;
        }

        public User user() {
            return user;
        }

        public FinanceSecurityPriceRefreshResultDTO result() {
            return result;
        }

        public Map<String, FinanceSecurityPriceRefreshItemDTO> itemByUserSecurityId() {
            return itemByUserSecurityId;
        }

        public Map<String, JobItemState> itemStateByUserSecurityId() {
            return itemStateByUserSecurityId;
        }

        public boolean isApplyRequested() {
            return applyRequested;
        }

        public void setApplyRequested(boolean applyRequested) {
            this.applyRequested = applyRequested;
        }
    }
}
