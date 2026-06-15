package ovaro.plat4m.web.rest;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import ovaro.plat4m.service.AIService;
import ovaro.plat4m.service.AiSettingsService;
import ovaro.plat4m.service.dto.AiModelOptionDTO;
import ovaro.plat4m.service.dto.AiSettingsDTO;
import ovaro.plat4m.service.dto.AiSettingsUpdateDTO;

@RestController
@RequestMapping("/api/account/ai-settings")
public class AiSettingsResource {

    private final AiSettingsService aiSettingsService;
    private final AIService aiService;

    public AiSettingsResource(AiSettingsService aiSettingsService, AIService aiService) {
        this.aiSettingsService = aiSettingsService;
        this.aiService = aiService;
    }

    @GetMapping
    public AiSettingsDTO getCurrentUserSettings() {
        return aiSettingsService.getCurrentUserSettings();
    }

    @GetMapping("/models")
    public List<AiModelOptionDTO> getAvailableModels() {
        return aiService.getAvailableModels(aiSettingsService.getCurrentUserSettingsForUse());
    }

    @PostMapping
    public AiSettingsDTO updateCurrentUserSettings(@Valid @RequestBody AiSettingsUpdateDTO updateDTO) {
        return aiSettingsService.updateCurrentUserSettings(updateDTO);
    }
}
