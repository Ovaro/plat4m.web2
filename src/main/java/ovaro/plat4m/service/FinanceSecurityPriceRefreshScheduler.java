package ovaro.plat4m.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FinanceSecurityPriceRefreshScheduler {

    private final FinanceSecurityPriceRefreshService financeSecurityPriceRefreshService;

    public FinanceSecurityPriceRefreshScheduler(FinanceSecurityPriceRefreshService financeSecurityPriceRefreshService) {
        this.financeSecurityPriceRefreshService = financeSecurityPriceRefreshService;
    }

    @Scheduled(
        cron = "${application.market-data.quote-refresh.cron:0 0 8 * * MON-FRI}",
        zone = "${application.market-data.quote-refresh.zone:Australia/Brisbane}"
    )
    public void refreshQuotes() {
        this.financeSecurityPriceRefreshService.refreshQuotesForAllUsers();
    }
}
