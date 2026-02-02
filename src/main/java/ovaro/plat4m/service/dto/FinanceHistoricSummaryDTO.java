package ovaro.plat4m.service.dto;

import java.time.LocalDate;
import java.util.Map;

public class FinanceHistoricSummaryDTO {

    Map<LocalDate, FinanceSnapshot> investments;

    public Map<LocalDate, FinanceSnapshot> getInvestments() {
        return investments;
    }

    public void setInvestments(Map<LocalDate, FinanceSnapshot> investments) {
        this.investments = investments;
    }
}
///
/// {
///     date: 'xxxxx',
///     investments: {[
///         id : 'xxx',
///         detail1: x,
///         detail...: ...,
///         value: x
///     ]},
///     assets: {
///         id : 'xxx',
///         detail1: x,
///         detail...: ...,
///         value: x
///     }
/// }
