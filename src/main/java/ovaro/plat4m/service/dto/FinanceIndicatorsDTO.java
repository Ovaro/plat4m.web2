package ovaro.plat4m.service.dto;

import java.util.ArrayList;
import java.util.List;

public class FinanceIndicatorsDTO {

    private List<FinanceIndicatorDTO> indicators;

    public List<FinanceIndicatorDTO> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<FinanceIndicatorDTO> indicators) {
        this.indicators = indicators;
    }

    public void addIndicator(FinanceIndicatorDTO indicator) {
        if (indicators == null) {
            indicators = new ArrayList<FinanceIndicatorDTO>();
        }

        indicators.add(indicator);
    }
}
