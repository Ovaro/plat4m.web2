package ovaro.plat4m.service.dto;

import java.util.List;

public class FinanceTransactionEditorOptionsDTO {

    private List<FinanceResourceDTO> categories;
    private List<FinanceResourceDTO> payees;

    public FinanceTransactionEditorOptionsDTO() {}

    public FinanceTransactionEditorOptionsDTO(List<FinanceResourceDTO> categories, List<FinanceResourceDTO> payees) {
        this.categories = categories;
        this.payees = payees;
    }

    public List<FinanceResourceDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<FinanceResourceDTO> categories) {
        this.categories = categories;
    }

    public List<FinanceResourceDTO> getPayees() {
        return payees;
    }

    public void setPayees(List<FinanceResourceDTO> payees) {
        this.payees = payees;
    }
}
