package ovaro.plat4m.service.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AiSettingsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String provider;
    private String defaultModel;
    private boolean hasApiKey;
    private String maskedApiKey;
    private Map<String, String> modelOverrides = new HashMap<>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public boolean isHasApiKey() {
        return hasApiKey;
    }

    public void setHasApiKey(boolean hasApiKey) {
        this.hasApiKey = hasApiKey;
    }

    public String getMaskedApiKey() {
        return maskedApiKey;
    }

    public void setMaskedApiKey(String maskedApiKey) {
        this.maskedApiKey = maskedApiKey;
    }

    public Map<String, String> getModelOverrides() {
        return modelOverrides;
    }

    public void setModelOverrides(Map<String, String> modelOverrides) {
        this.modelOverrides = modelOverrides;
    }
}
