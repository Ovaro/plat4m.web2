package ovaro.plat4m.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "dashboard_panel_definition")
public class DashboardPanelDefinition extends AbstractAuditingEntity<UUID> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String SCOPE_GLOBAL = "GLOBAL";
    public static final String SCOPE_USER = "USER";

    @Id
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @NotNull
    @Size(min = 1, max = 120)
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @NotNull
    @Size(min = 1, max = 40)
    @Column(name = "panel_type", nullable = false, length = 40)
    private String panelType;

    @Column(name = "config_json", nullable = false)
    private String configJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

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

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
