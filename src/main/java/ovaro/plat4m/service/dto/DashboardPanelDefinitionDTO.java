package ovaro.plat4m.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public class DashboardPanelDefinitionDTO {

    private UUID id;

    @NotNull
    @Size(min = 1, max = 20)
    private String scope;

    @NotNull
    @Size(min = 1, max = 120)
    private String name;

    @NotNull
    @Size(min = 1, max = 40)
    private String panelType;

    @NotNull
    private List<DataTypeSpecDTO> dataTypeSpecs;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPanelType() {
        return panelType;
    }

    public void setPanelType(String panelType) {
        this.panelType = panelType;
    }

    public List<DataTypeSpecDTO> getDataTypeSpecs() {
        return dataTypeSpecs;
    }

    public void setDataTypeSpecs(List<DataTypeSpecDTO> dataTypeSpecs) {
        this.dataTypeSpecs = dataTypeSpecs;
    }
}
