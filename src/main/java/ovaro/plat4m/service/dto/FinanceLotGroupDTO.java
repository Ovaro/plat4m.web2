package ovaro.plat4m.service.dto;

import java.util.ArrayList;
import java.util.List;

public class FinanceLotGroupDTO {

    private FinanceLotViewDTO originalLot;
    private FinanceLotViewDTO remainingLot;
    private List<FinanceLotViewDTO> lots = new ArrayList<>();

    public FinanceLotViewDTO getOriginalLot() {
        return originalLot;
    }

    public void setOriginalLot(FinanceLotViewDTO originalLot) {
        this.originalLot = originalLot;
    }

    public FinanceLotViewDTO getRemainingLot() {
        return remainingLot;
    }

    public void setRemainingLot(FinanceLotViewDTO remainingLot) {
        this.remainingLot = remainingLot;
    }

    public List<FinanceLotViewDTO> getLots() {
        return lots;
    }

    public void setLots(List<FinanceLotViewDTO> lots) {
        this.lots = lots;
    }
}
