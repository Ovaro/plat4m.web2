package ovaro.plat4m.service.dto;

import java.io.Serial;
import java.io.Serializable;

public class UserApiKeyCreatedDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UserApiKeyDTO apiKey;
    private String token;

    public UserApiKeyCreatedDTO() {}

    public UserApiKeyCreatedDTO(UserApiKeyDTO apiKey, String token) {
        this.apiKey = apiKey;
        this.token = token;
    }

    public UserApiKeyDTO getApiKey() {
        return apiKey;
    }

    public void setApiKey(UserApiKeyDTO apiKey) {
        this.apiKey = apiKey;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
