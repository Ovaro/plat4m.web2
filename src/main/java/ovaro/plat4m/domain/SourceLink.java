package ovaro.plat4m.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(
    name = "glb_source_link",
    indexes = {
        @Index(columnList = "userGuid"),
        @Index(columnList = "sourceId"),
        @Index(columnList = "localId"),
        @Index(columnList = "sourceEntity"),
        @Index(columnList = "sourceTypeId"),
    },
    uniqueConstraints = { @UniqueConstraint(columnNames = { "userGuid", "sourceTypeId", "sourceGuid", "localId" }) }
)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class SourceLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sourceLinkIdSequence")
    @SequenceGenerator(name = "sourceLinkIdSequence")
    private Long id;

    @NotNull
    private String userGuid;

    @NotNull
    protected String sourceTypeId;

    @NotNull
    protected String sourceId;

    protected String sourceGuid;
    protected String sourceEntity;

    @NotNull
    protected String localId;

    protected boolean master;
    protected String originalInfo1;

    public String getUserGuid() {
        return userGuid;
    }

    public void setUserGuid(String userGuid) {
        this.userGuid = userGuid;
    }

    public String getSourceTypeId() {
        return sourceTypeId;
    }

    public void setSourceTypeId(String sourceTypeId) {
        this.sourceTypeId = sourceTypeId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceGuid() {
        return sourceGuid;
    }

    public void setSourceGuid(String sourceGuid) {
        this.sourceGuid = sourceGuid;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalInfo1() {
        return originalInfo1;
    }

    public void setOriginalInfo1(String originalInfo1) {
        this.originalInfo1 = originalInfo1;
    }

    public String getSourceEntity() {
        return sourceEntity;
    }

    public void setSourceEntity(String sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    @Override
    public String toString() {
        return (
            "SourceLink [id=" +
            id +
            ", localId=" +
            localId +
            ", master=" +
            master +
            ", originalInfo1=" +
            originalInfo1 +
            ", sourceEntity=" +
            sourceEntity +
            ", sourceGuid=" +
            sourceGuid +
            ", sourceId=" +
            sourceId +
            ", sourceTypeId=" +
            sourceTypeId +
            "]"
        );
    }
}
