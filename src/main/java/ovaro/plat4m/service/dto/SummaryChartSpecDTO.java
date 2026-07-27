package ovaro.plat4m.service.dto;

public class SummaryChartSpecDTO {

    private Boolean enabled;
    private String chartType;
    private Boolean combineSeries;
    private Boolean smooth;
    private Boolean showFill;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String chartType) {
        this.chartType = chartType;
    }

    public Boolean getCombineSeries() {
        return combineSeries;
    }

    public void setCombineSeries(Boolean combineSeries) {
        this.combineSeries = combineSeries;
    }

    public Boolean getSmooth() {
        return smooth;
    }

    public void setSmooth(Boolean smooth) {
        this.smooth = smooth;
    }

    public Boolean getShowFill() {
        return showFill;
    }

    public void setShowFill(Boolean showFill) {
        this.showFill = showFill;
    }
}
