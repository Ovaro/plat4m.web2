package ovaro.plat4m.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class DataTypeSpecDTO {

    @NotNull
    @Size(min = 1, max = 120)
    private String title;

    @NotNull
    private List<String> dataType;

    private String compositeDataTypeOperation;

    private Boolean negativeAsPositive;

    private Boolean expandGroups;

    private DetailPanelSpecDTO detailedPanelSpec;

    private SummaryChartSpecDTO summaryChartSpec;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getDataType() {
        return dataType;
    }

    public void setDataType(List<String> dataType) {
        this.dataType = dataType;
    }

    public String getCompositeDataTypeOperation() {
        return compositeDataTypeOperation;
    }

    public void setCompositeDataTypeOperation(String compositeDataTypeOperation) {
        this.compositeDataTypeOperation = compositeDataTypeOperation;
    }

    public Boolean getNegativeAsPositive() {
        return negativeAsPositive;
    }

    public void setNegativeAsPositive(Boolean negativeAsPositive) {
        this.negativeAsPositive = negativeAsPositive;
    }

    public Boolean getExpandGroups() {
        return expandGroups;
    }

    public void setExpandGroups(Boolean expandGroups) {
        this.expandGroups = expandGroups;
    }

    public DetailPanelSpecDTO getDetailedPanelSpec() {
        return detailedPanelSpec;
    }

    public void setDetailedPanelSpec(DetailPanelSpecDTO detailedPanelSpec) {
        this.detailedPanelSpec = detailedPanelSpec;
    }

    public SummaryChartSpecDTO getSummaryChartSpec() {
        return summaryChartSpec;
    }

    public void setSummaryChartSpec(SummaryChartSpecDTO summaryChartSpec) {
        this.summaryChartSpec = summaryChartSpec;
    }
}
