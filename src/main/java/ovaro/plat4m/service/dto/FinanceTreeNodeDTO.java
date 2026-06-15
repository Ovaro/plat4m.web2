package ovaro.plat4m.service.dto;

import java.util.ArrayList;
import java.util.List;

public class FinanceTreeNodeDTO {

    private String key;
    private String label;
    private boolean selectable = true;
    private boolean leaf = false;
    private List<FinanceTreeNodeDTO> children = new ArrayList<>();

    public FinanceTreeNodeDTO() {}

    public FinanceTreeNodeDTO(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    public List<FinanceTreeNodeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<FinanceTreeNodeDTO> children) {
        this.children = children;
    }
}
