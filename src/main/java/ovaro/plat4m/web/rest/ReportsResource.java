package ovaro.plat4m.web.rest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import ovaro.plat4m.service.ReportsService;
import ovaro.plat4m.service.dto.FinanceLotViewDTO;
import ovaro.plat4m.service.dto.IncomeExpenseReportDrilldownDTO;
import ovaro.plat4m.service.dto.IncomeExpenseReportDrilldownRequestDTO;
import ovaro.plat4m.service.dto.IncomeExpenseReportResultDTO;
import ovaro.plat4m.service.dto.ReportConfigDTO;
import ovaro.plat4m.service.dto.ReportDefinitionDTO;

@RestController
@RequestMapping("/api/reports")
public class ReportsResource {

    private final ReportsService reportsService;

    public ReportsResource(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping
    public List<ReportDefinitionDTO> getDefinitions() {
        return reportsService.getDefinitions();
    }

    @GetMapping("/configs")
    public List<ReportConfigDTO> getConfigs(@RequestParam(name = "reportKey") String reportKey) {
        return reportsService.getConfigs(reportKey);
    }

    @PostMapping("/configs")
    public ReportConfigDTO createConfig(@Valid @RequestBody ReportConfigDTO dto) {
        return reportsService.createConfig(dto);
    }

    @PutMapping("/configs/{id}")
    public ReportConfigDTO updateConfig(@PathVariable(name = "id") UUID id, @Valid @RequestBody ReportConfigDTO dto) {
        return reportsService.updateConfig(id, dto);
    }

    @PostMapping("/income-expenses/run")
    public IncomeExpenseReportResultDTO runIncomeExpenseReport(@Valid @RequestBody ReportConfigDTO dto) {
        return reportsService.runIncomeExpenseReport(dto);
    }

    @PostMapping("/income-expenses/drilldown")
    public IncomeExpenseReportDrilldownDTO getIncomeExpenseReportDrilldown(@Valid @RequestBody IncomeExpenseReportDrilldownRequestDTO dto) {
        return reportsService.getIncomeExpenseDrilldown(dto);
    }

    @GetMapping("/income-expenses/transactions/{id}/lots")
    public List<FinanceLotViewDTO> getIncomeExpenseReportTransactionLots(@PathVariable(name = "id") UUID id) {
        return reportsService.getIncomeExpenseReportTransactionLots(id);
    }
}
