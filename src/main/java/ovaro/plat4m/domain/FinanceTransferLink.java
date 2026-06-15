package ovaro.plat4m.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "fin_xfer")
public class FinanceTransferLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_guid", nullable = false)
    private String userGuid;

    @Column(name = "from_id", nullable = false)
    private UUID fromId;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserGuid() {
        return userGuid;
    }

    public void setUserGuid(String userGuid) {
        this.userGuid = userGuid;
    }

    public UUID getFromId() {
        return fromId;
    }

    public void setFromId(UUID fromId) {
        this.fromId = fromId;
    }

    public UUID getLinkId() {
        return linkId;
    }

    public void setLinkId(UUID linkId) {
        this.linkId = linkId;
    }
}
