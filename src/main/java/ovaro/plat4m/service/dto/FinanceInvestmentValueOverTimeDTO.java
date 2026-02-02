package ovaro.plat4m.service.dto;

import java.util.List;
import ovaro.plat4m.domain.FinanceUserSecurity;

public class FinanceInvestmentValueOverTimeDTO {

    FinanceUserSecurity security;
    List<FinanceSnapshot> valueOverTimePerSecurity;
    List<FinanceDateAnnotationDTO> annotationsPerSecurity;

    public FinanceInvestmentValueOverTimeDTO() {}

    public FinanceInvestmentValueOverTimeDTO(
        FinanceUserSecurity security,
        List<FinanceSnapshot> valueOverTimePerSecurity,
        List<FinanceDateAnnotationDTO> annotationsPerSecurity
    ) {
        this.security = security;
        this.valueOverTimePerSecurity = valueOverTimePerSecurity;
        this.annotationsPerSecurity = annotationsPerSecurity;
    }

    public FinanceUserSecurity getSecurity() {
        return security;
    }

    public void setSecurity(FinanceUserSecurity security) {
        this.security = security;
    }

    public List<FinanceSnapshot> getValueOverTimePerSecurity() {
        return valueOverTimePerSecurity;
    }

    public void setValueOverTimePerSecurity(List<FinanceSnapshot> valueOverTimePerSecurity) {
        this.valueOverTimePerSecurity = valueOverTimePerSecurity;
    }

    public List<FinanceDateAnnotationDTO> getAnnotationsPerSecurity() {
        return annotationsPerSecurity;
    }

    public void setAnnotationsPerSecurity(List<FinanceDateAnnotationDTO> annotationsPerSecurity) {
        this.annotationsPerSecurity = annotationsPerSecurity;
    }
}
