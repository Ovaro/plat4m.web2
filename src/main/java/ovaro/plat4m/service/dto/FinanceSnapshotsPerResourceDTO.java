package ovaro.plat4m.service.dto;

import java.util.ArrayList;
import java.util.List;

public class FinanceSnapshotsPerResourceDTO extends FinanceResourceDTO {

    private List<FinanceSnapshot> snapshots;
    private List<FinanceDateAnnotationDTO> annotations;

    public List<FinanceSnapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<FinanceSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public void addSnapshot(FinanceSnapshot snapshot) {
        if (this.snapshots == null) {
            this.snapshots = new ArrayList<FinanceSnapshot>();
        }
        this.snapshots.add(snapshot);
    }

    public List<FinanceDateAnnotationDTO> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<FinanceDateAnnotationDTO> annotations) {
        this.annotations = annotations;
    }
}
