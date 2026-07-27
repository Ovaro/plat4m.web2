package ovaro.plat4m.service.dto;

public class DetailPanelSpecDTO {

    private String type;
    private String chartType;
    private Boolean combineSeries;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
