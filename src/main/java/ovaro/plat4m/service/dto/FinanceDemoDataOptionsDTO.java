package ovaro.plat4m.service.dto;

import java.util.List;

public class FinanceDemoDataOptionsDTO {

    private List<String> personas;
    private List<String> variants;
    private List<String> accountTypes;
    private List<Integer> yearOptions;

    public FinanceDemoDataOptionsDTO(List<String> personas, List<String> variants, List<String> accountTypes, List<Integer> yearOptions) {
        this.personas = personas;
        this.variants = variants;
        this.accountTypes = accountTypes;
        this.yearOptions = yearOptions;
    }

    public List<String> getPersonas() {
        return personas;
    }

    public List<String> getVariants() {
        return variants;
    }

    public List<String> getAccountTypes() {
        return accountTypes;
    }

    public List<Integer> getYearOptions() {
        return yearOptions;
    }
}
