package ovaro.plat4m.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AiSettingsUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 100)
    private String defaultModel;

    @Size(max = 512)
    private String apiKey;

    private boolean clearApiKey;

    private Map<String, String> modelOverrides = new HashMap<>();

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isClearApiKey() {
        return clearApiKey;
    }

    public void setClearApiKey(boolean clearApiKey) {
        this.clearApiKey = clearApiKey;
    }

    public Map<String, String> getModelOverrides() {
        return modelOverrides;
    }

    public void setModelOverrides(Map<String, String> modelOverrides) {
        this.modelOverrides = modelOverrides;
    }
}
