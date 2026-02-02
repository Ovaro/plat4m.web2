package ovaro.plat4m.service.dto;

import java.util.ArrayList;
import java.util.List;

public class FinanceNestedResourceDTO extends FinanceResourceDTO {

    public FinanceNestedResourceDTO() {}

    public FinanceNestedResourceDTO(String resourceId, String resourceName) {
        super(resourceId, resourceName);
    }

    public FinanceNestedResourceDTO(
        String resourceId,
        String resourceName,
        String resourceSymbol,
        String resourceCurrencyCode,
        String indicatorType
    ) {
        super(resourceId, resourceName, resourceSymbol, resourceCurrencyCode, indicatorType);
    }

    private List<FinanceNestedResourceDTO> children;

    public List<FinanceNestedResourceDTO> getChildren() {
        return children;
    }

    public void setChildren(List<FinanceNestedResourceDTO> children) {
        this.children = children;
    }

    public void addChild(FinanceNestedResourceDTO child) {
        if (this.children == null) {
            this.children = new ArrayList<FinanceNestedResourceDTO>();
        }
        this.children.add(child);
    }
}
