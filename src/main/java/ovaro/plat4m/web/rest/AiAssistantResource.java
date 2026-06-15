package ovaro.plat4m.web.rest;

import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ovaro.plat4m.service.AiAssistantService;
import ovaro.plat4m.service.dto.AiAssistantQueryDTO;
import ovaro.plat4m.service.dto.AiAssistantResponseDTO;

@RestController
@RequestMapping("/api/ai-assistant")
public class AiAssistantResource {

    private final AiAssistantService aiAssistantService;

    public AiAssistantResource(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/query")
    public AiAssistantResponseDTO query(@Valid @RequestBody AiAssistantQueryDTO query) throws IOException {
        return aiAssistantService.query(query);
    }
}
