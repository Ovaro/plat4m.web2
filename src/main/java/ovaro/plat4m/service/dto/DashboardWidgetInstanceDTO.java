package ovaro.plat4m.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class DashboardWidgetInstanceDTO {

    @NotNull
    private UUID id;

    @NotNull
    private UUID panelDefinitionId;

    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;
    private String section;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPanelDefinitionId() {
        return panelDefinitionId;
    }

    public void setPanelDefinitionId(UUID panelDefinitionId) {
        this.panelDefinitionId = panelDefinitionId;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getW() {
        return w;
    }

    public void setW(Integer w) {
        this.w = w;
    }

    public Integer getH() {
        return h;
    }

    public void setH(Integer h) {
        this.h = h;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
