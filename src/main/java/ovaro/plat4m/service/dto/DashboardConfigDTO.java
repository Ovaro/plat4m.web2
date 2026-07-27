package ovaro.plat4m.service.dto;

import java.util.List;
import java.util.UUID;

public class DashboardConfigDTO {

    private UUID id;
    private String name;
    private boolean active;
    private List<DashboardWidgetInstanceDTO> widgets;
    private List<DashboardPanelDefinitionDTO> panelDefinitions;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<DashboardWidgetInstanceDTO> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<DashboardWidgetInstanceDTO> widgets) {
        this.widgets = widgets;
    }

    public List<DashboardPanelDefinitionDTO> getPanelDefinitions() {
        return panelDefinitions;
    }

    public void setPanelDefinitions(List<DashboardPanelDefinitionDTO> panelDefinitions) {
        this.panelDefinitions = panelDefinitions;
    }
}
