package ovaro.plat4m.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class DashboardLayoutUpdateDTO {

    @NotNull
    private List<DashboardWidgetInstanceDTO> widgets;

    public List<DashboardWidgetInstanceDTO> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<DashboardWidgetInstanceDTO> widgets) {
        this.widgets = widgets;
    }
}
