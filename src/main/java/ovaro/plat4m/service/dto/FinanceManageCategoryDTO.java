package ovaro.plat4m.service.dto;

public class FinanceManageCategoryDTO {

    private String id;
    private String name;
    private String displayName;
    private String parentId;
    private String parentName;
    private Integer classificationId;
    private Integer level;
    private String comment;
    private Boolean hasChildren;
    private Boolean systemCategory;
    private String knownCategoryKey;
    private Boolean platformDefaultCategory;
    private Boolean templateCategory;
    private Boolean hidden;

    public FinanceManageCategoryDTO() {}

    public FinanceManageCategoryDTO(
        String id,
        String name,
        String displayName,
        String parentId,
        String parentName,
        Integer classificationId,
        Integer level,
        String comment,
        Boolean hasChildren,
        Boolean systemCategory,
        String knownCategoryKey,
        Boolean platformDefaultCategory,
        Boolean templateCategory,
        Boolean hidden
    ) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.parentId = parentId;
        this.parentName = parentName;
        this.classificationId = classificationId;
        this.level = level;
        this.comment = comment;
        this.hasChildren = hasChildren;
        this.systemCategory = systemCategory;
        this.knownCategoryKey = knownCategoryKey;
        this.platformDefaultCategory = platformDefaultCategory;
        this.templateCategory = templateCategory;
        this.hidden = hidden;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(Boolean hasChildren) {
        this.hasChildren = hasChildren;
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
}
