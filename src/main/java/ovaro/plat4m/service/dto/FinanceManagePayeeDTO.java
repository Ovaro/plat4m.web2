package ovaro.plat4m.service.dto;

import java.util.List;

public class FinanceManagePayeeDTO {

    private String id;
    private String name;
    private String parentId;
    private Boolean hidden;
    private int childCount;
    private List<String> childNames;

    public FinanceManagePayeeDTO() {}

    public FinanceManagePayeeDTO(String id, String name, String parentId, Boolean hidden, int childCount, List<String> childNames) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.hidden = hidden;
        this.childCount = childCount;
        this.childNames = childNames;
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

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public List<String> getChildNames() {
        return childNames;
    }

    public void setChildNames(List<String> childNames) {
        this.childNames = childNames;
    }
}
