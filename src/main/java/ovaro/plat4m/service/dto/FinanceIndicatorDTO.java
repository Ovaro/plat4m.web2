package ovaro.plat4m.service.dto;

public class FinanceIndicatorDTO extends FinanceResourceDTO {

    private FinanceSnapshotWithComparison snapshot;

    public FinanceSnapshotWithComparison getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(FinanceSnapshotWithComparison snapshot) {
        this.snapshot = snapshot;
    }
}
