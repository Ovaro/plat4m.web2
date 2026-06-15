package ovaro.plat4m.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "user_ai_settings")
public class UserAiSettings extends AbstractAuditingEntity<UUID> implements Serializable {

    public static final String PROVIDER_GEMINI = "gemini";
    public static final String DEFAULT_GEMINI_MODEL = "gemini-flash-latest";

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "provider", nullable = false, length = 30)
    private String provider = PROVIDER_GEMINI;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "default_model", nullable = false, length = 100)
    private String defaultModel = DEFAULT_GEMINI_MODEL;

    @Size(max = 512)
    @Column(name = "gemini_api_key", length = 512)
    private String geminiApiKey;

    @Column(name = "model_overrides_json")
    private String modelOverridesJson;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getModelOverridesJson() {
        return modelOverridesJson;
    }

    public void setModelOverridesJson(String modelOverridesJson) {
        this.modelOverridesJson = modelOverridesJson;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
