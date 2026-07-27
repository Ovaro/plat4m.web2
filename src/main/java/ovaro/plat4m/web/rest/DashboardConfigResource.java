package ovaro.plat4m.web.rest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import ovaro.plat4m.service.DashboardConfigService;
import ovaro.plat4m.service.dto.DashboardConfigDTO;
import ovaro.plat4m.service.dto.DashboardLayoutUpdateDTO;
import ovaro.plat4m.service.dto.DashboardPanelDefinitionDTO;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardConfigResource {

    private final DashboardConfigService dashboardConfigService;

    public DashboardConfigResource(DashboardConfigService dashboardConfigService) {
        this.dashboardConfigService = dashboardConfigService;
    }

    @GetMapping("/config")
    public DashboardConfigDTO getConfig() {
        return dashboardConfigService.getConfig();
    }

    @PutMapping("/config/layout")
    public DashboardConfigDTO updateLayout(@Valid @RequestBody DashboardLayoutUpdateDTO update) {
        return dashboardConfigService.updateLayout(update);
    }

    @GetMapping("/panels")
    public List<DashboardPanelDefinitionDTO> getPanels() {
        return dashboardConfigService.getPanels();
    }

    @PostMapping("/panels")
    public DashboardPanelDefinitionDTO createPanel(@Valid @RequestBody DashboardPanelDefinitionDTO dto) {
        return dashboardConfigService.createPanel(dto);
    }

    @PutMapping("/panels/{id}")
    public DashboardPanelDefinitionDTO updatePanel(
        @PathVariable(name = "id") UUID id,
        @Valid @RequestBody DashboardPanelDefinitionDTO dto
    ) {
        return dashboardConfigService.updatePanel(id, dto);
    }
}
