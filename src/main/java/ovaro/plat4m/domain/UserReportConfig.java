package ovaro.plat4m.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(
    name = "user_report_config",
    uniqueConstraints = {
        @UniqueConstraint(name = "ux_user_report_config_user_report_name", columnNames = { "user_id", "report_key", "name" }),
    }
)
public class UserReportConfig extends AbstractAuditingEntity<UUID> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "report_key", nullable = false, length = 100)
    private String reportKey;

    @NotNull
    @Size(min = 1, max = 120)
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "config_json", nullable = false)
    private String configJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReportKey() {
        return reportKey;
    }

    public void setReportKey(String reportKey) {
        this.reportKey = reportKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
