package ovaro.plat4m.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "fin_category")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String userGuid;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_category_id", referencedColumnName = "id")
    @Fetch(FetchMode.JOIN)
    private FinanceCategory parent;

    @Transient
    private Integer sourceParentId; // Just here to make loading easier

    private String name;
    private Integer classificationId;
    private Integer level;
    private String comment;
    private Boolean systemCategory = false;
    private String knownCategoryKey;
    private Boolean platformDefaultCategory = false;
    private Boolean templateCategory = false;
    private Boolean hidden = false;
    private UUID templateCategoryId;
    private boolean generated = false;
    private UUID generatedBatchId;
    private String generatedLabel;

    public FinanceCategory() {}

    public FinanceCategory(UUID id) {}

    public FinanceCategory(String id) {
        this.id = UUID.fromString(id);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getSystemCategory() {
        return systemCategory;
    }

    public void setSystemCategory(Boolean systemCategory) {
        this.systemCategory = systemCategory;
    }

    public String getKnownCategoryKey() {
        return knownCategoryKey;
    }

    public void setKnownCategoryKey(String knownCategoryKey) {
        this.knownCategoryKey = knownCategoryKey;
    }

    public Boolean getPlatformDefaultCategory() {
        return platformDefaultCategory;
    }

    public void setPlatformDefaultCategory(Boolean platformDefaultCategory) {
        this.platformDefaultCategory = platformDefaultCategory;
    }

    public Boolean getTemplateCategory() {
        return templateCategory;
    }

    public void setTemplateCategory(Boolean templateCategory) {
        this.templateCategory = templateCategory;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public UUID getTemplateCategoryId() {
        return templateCategoryId;
    }

    public void setTemplateCategoryId(UUID templateCategoryId) {
        this.templateCategoryId = templateCategoryId;
    }

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

    public FinanceCategory getParent() {
        return parent;
    }

    public void setParent(FinanceCategory parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getClassificationId() {
        return classificationId;
    }

    public void setClassificationId(Integer classificationId) {
        this.classificationId = classificationId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getSourceParentId() {
        return sourceParentId;
    }

    public void setSourceParentId(Integer sourceParentId) {
        this.sourceParentId = sourceParentId;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public UUID getGeneratedBatchId() {
        return generatedBatchId;
    }

    public void setGeneratedBatchId(UUID generatedBatchId) {
        this.generatedBatchId = generatedBatchId;
    }

    public String getGeneratedLabel() {
        return generatedLabel;
    }

    public void setGeneratedLabel(String generatedLabel) {
        this.generatedLabel = generatedLabel;
    }
}
