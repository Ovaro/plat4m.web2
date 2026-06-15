package ovaro.plat4m.service.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import ovaro.plat4m.domain.UserApiKey;

public class UserApiKeyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private Instant createdDate;
    private Instant expiresAt;

    public UserApiKeyDTO() {}

    public UserApiKeyDTO(UserApiKey userApiKey) {
        this.id = userApiKey.getId().toString();
        this.name = userApiKey.getName();
        this.createdDate = userApiKey.getCreatedDate();
        this.expiresAt = userApiKey.getExpiresAt();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
