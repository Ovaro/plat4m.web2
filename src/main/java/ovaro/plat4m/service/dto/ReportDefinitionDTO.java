package ovaro.plat4m.service.dto;

public class ReportDefinitionDTO {

    private String key;
    private String title;
    private String description;
    private ReportConfigDTO defaultConfig;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReportConfigDTO getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(ReportConfigDTO defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
}
