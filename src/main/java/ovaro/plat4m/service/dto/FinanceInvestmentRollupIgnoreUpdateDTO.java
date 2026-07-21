package ovaro.plat4m.service.dto;

public class FinanceInvestmentRollupIgnoreUpdateDTO {

    private boolean ignoredForRollup;
    private String ignoredForRollupReason;

    public boolean isIgnoredForRollup() {
        return ignoredForRollup;
    }

    public void setIgnoredForRollup(boolean ignoredForRollup) {
        this.ignoredForRollup = ignoredForRollup;
    }

    public String getIgnoredForRollupReason() {
        return ignoredForRollupReason;
    }

    public void setIgnoredForRollupReason(String ignoredForRollupReason) {
        this.ignoredForRollupReason = ignoredForRollupReason;
    }
}
