package ovaro.plat4m.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.domain.UserAiSettings;
import ovaro.plat4m.repository.UserAiSettingsRepository;
import ovaro.plat4m.repository.UserRepository;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.dto.AiSettingsDTO;
import ovaro.plat4m.service.dto.AiSettingsUpdateDTO;
import ovaro.plat4m.web.rest.errors.BadRequestAlertException;

@Service
@Transactional
public class AiSettingsService {

    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {};

    private final UserAiSettingsRepository userAiSettingsRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AiSettingsService(UserAiSettingsRepository userAiSettingsRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.userAiSettingsRepository = userAiSettingsRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public AiSettingsDTO getCurrentUserSettings() {
        UserAiSettings settings = getOrCreateCurrentUserSettings();
        return toDto(settings);
    }

    public AiSettingsDTO updateCurrentUserSettings(AiSettingsUpdateDTO updateDTO) {
        UserAiSettings settings = getOrCreateCurrentUserSettings();
        settings.setDefaultModel(updateDTO.getDefaultModel().trim());

        if (updateDTO.isClearApiKey()) {
            settings.setGeminiApiKey(null);
        } else if (updateDTO.getApiKey() != null && !updateDTO.getApiKey().trim().isEmpty()) {
            settings.setGeminiApiKey(updateDTO.getApiKey().trim());
        }

        settings.setModelOverridesJson(writeOverrides(updateDTO.getModelOverrides()));
        return toDto(userAiSettingsRepository.save(settings));
    }

    public UserAiSettings getCurrentUserSettingsForUse() {
        return getOrCreateCurrentUserSettings();
    }

    @Transactional(readOnly = true)
    public Map<String, String> readModelOverrides(UserAiSettings settings) {
        return readOverrides(settings.getModelOverridesJson());
    }

    private UserAiSettings getOrCreateCurrentUserSettings() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user login not found", "aiSettings", "currentusernotfound")
        );

        return userAiSettingsRepository.findOneByUserLogin(login).orElseGet(() -> createDefaultSettings(login));
    }

    private UserAiSettings createDefaultSettings(String login) {
        User user = userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", "aiSettings", "currentusernotfound"));

        UserAiSettings settings = new UserAiSettings();
        settings.setId(UUID.randomUUID());
        settings.setUser(user);
        settings.setProvider(UserAiSettings.PROVIDER_GEMINI);
        settings.setDefaultModel(UserAiSettings.DEFAULT_GEMINI_MODEL);
        settings.setModelOverridesJson(writeOverrides(Map.of()));
        return userAiSettingsRepository.save(settings);
    }

    private AiSettingsDTO toDto(UserAiSettings settings) {
        AiSettingsDTO dto = new AiSettingsDTO();
        dto.setProvider(settings.getProvider());
        dto.setDefaultModel(settings.getDefaultModel());
        dto.setHasApiKey(settings.getGeminiApiKey() != null && !settings.getGeminiApiKey().isBlank());
        dto.setMaskedApiKey(maskApiKey(settings.getGeminiApiKey()));
        dto.setModelOverrides(readOverrides(settings.getModelOverridesJson()));
        return dto;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        if (apiKey.length() <= 8) {
            return "••••••••";
        }
        return apiKey.substring(0, 4) + "••••••••" + apiKey.substring(apiKey.length() - 4);
    }

    private Map<String, String> readOverrides(String modelOverridesJson) {
        if (modelOverridesJson == null || modelOverridesJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(modelOverridesJson, STRING_MAP_TYPE);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String writeOverrides(Map<String, String> modelOverrides) {
        try {
            return objectMapper.writeValueAsString(modelOverrides == null ? Map.of() : modelOverrides);
        } catch (Exception e) {
            throw new BadRequestAlertException("Model overrides could not be saved", "aiSettings", "invalidoverrides");
        }
    }
}
