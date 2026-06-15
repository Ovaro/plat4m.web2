package ovaro.plat4m.service.dto;

public class FinanceManagePayeeDTO {

    private String id;
    private String name;
    private String parentId;
    private Boolean hidden;

    public FinanceManagePayeeDTO() {}

    public FinanceManagePayeeDTO(String id, String name, String parentId, Boolean hidden) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }
}
