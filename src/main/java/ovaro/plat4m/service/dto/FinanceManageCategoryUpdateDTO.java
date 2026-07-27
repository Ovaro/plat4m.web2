package ovaro.plat4m.service.dto;

public class FinanceManageCategoryUpdateDTO {

    private String name;
    private String parentId;
    private Integer classificationId;
    private String comment;
    private String knownCategoryKey;
    private Boolean hidden;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getClassificationId() {
        return classificationId;
    }

    public void setClassificationId(Integer classificationId) {
        this.classificationId = classificationId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getKnownCategoryKey() {
        return knownCategoryKey;
    }

    public void setKnownCategoryKey(String knownCategoryKey) {
        this.knownCategoryKey = knownCategoryKey;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }
}
