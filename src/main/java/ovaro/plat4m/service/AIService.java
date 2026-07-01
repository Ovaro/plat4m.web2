package ovaro.plat4m.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.domain.UserAiSettings;
import ovaro.plat4m.service.dto.AiModelOptionDTO;

@Service
@Transactional(readOnly = true)
public class AIService {

    private static final Logger LOG = LoggerFactory.getLogger(AIService.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String GEMINI_MODEL_PREFIX = "models/";

    private final AiSettingsService aiSettingsService;
    private final ObjectMapper objectMapper;

    public AIService(AiSettingsService aiSettingsService, ObjectMapper objectMapper) {
        this.aiSettingsService = aiSettingsService;
        this.objectMapper = objectMapper;
    }

    public Optional<AiClientContext> getCurrentUserClientContext(AiServiceCallType callType) {
        UserAiSettings settings = aiSettingsService.getCurrentUserSettingsForUse();
        if (settings.getGeminiApiKey() == null || settings.getGeminiApiKey().isBlank()) {
            return Optional.empty();
        }

        String model = resolveModel(settings, callType);
        Object client = Map.of("provider", "gemini-rest", "apiKeyConfigured", Boolean.TRUE);
        return Optional.of(new AiClientContext(AiProvider.GEMINI, model, client));
    }

    public Object buildFunctionCallingConfig(List<Method> methods) {
        return methods == null ? List.of() : List.copyOf(methods);
    }

    public Optional<AiTextResponse> generateText(AiServiceCallType callType, String prompt) {
        UserAiSettings settings = aiSettingsService.getCurrentUserSettingsForUse();
        return generateText(settings, callType, prompt);
    }

    public Optional<AiTextResponse> generateText(User user, AiServiceCallType callType, String prompt) {
        Optional<UserAiSettings> settings = aiSettingsService.findSettingsForUser(user);
        if (settings.isEmpty()) {
            return Optional.empty();
        }
        return generateText(settings.get(), callType, prompt);
    }

    private Optional<AiTextResponse> generateText(UserAiSettings settings, AiServiceCallType callType, String prompt) {
        if (settings.getGeminiApiKey() == null || settings.getGeminiApiKey().isBlank()) {
            return Optional.empty();
        }

        String model = resolveModel(settings, callType);
        try {
            JsonNode response = postGenerateContent(settings.getGeminiApiKey(), model, prompt);
            String text = extractGeneratedText(response);
            return Optional.of(new AiTextResponse(model, text == null ? "" : text.trim()));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.error("Gemini text generation failed for model {}", model, ex);
            throw new IllegalStateException("Gemini request failed. Check the backend logs for details.", ex);
        }
    }

    public List<AiModelOptionDTO> getAvailableModels(UserAiSettings settings) {
        LinkedHashMap<String, AiModelOptionDTO> modelsById = new LinkedHashMap<>();
        modelsById.put(
            UserAiSettings.DEFAULT_GEMINI_MODEL,
            new AiModelOptionDTO(
                UserAiSettings.DEFAULT_GEMINI_MODEL,
                "Gemini Flash Latest",
                "Rolling alias for the latest Gemini Flash model."
            )
        );

        if (settings.getDefaultModel() != null && !settings.getDefaultModel().isBlank()) {
            modelsById.putIfAbsent(
                settings.getDefaultModel(),
                new AiModelOptionDTO(settings.getDefaultModel(), settings.getDefaultModel(), null)
            );
        }

        if (settings.getGeminiApiKey() == null || settings.getGeminiApiKey().isBlank()) {
            return new ArrayList<>(modelsById.values());
        }

        try {
            JsonNode response = listModels(settings.getGeminiApiKey());
            List<AiModelOptionDTO> discoveredModels = new ArrayList<>();
            for (JsonNode model : response.path("models")) {
                String modelId = normalizeModelId(model.path("name").asText(null));
                if (modelId == null || !supportsGenerateContent(model)) {
                    continue;
                }
                discoveredModels.add(new AiModelOptionDTO(modelId, buildModelLabel(modelId, model), textOrNull(model.path("description"))));
            }

            discoveredModels
                .stream()
                .sorted(Comparator.comparing(AiModelOptionDTO::getLabel, String.CASE_INSENSITIVE_ORDER))
                .forEach(model -> modelsById.putIfAbsent(model.getId(), model));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warn("Gemini model discovery failed. Falling back to the default model list.", ex);
            // Fall back to the default alias when model discovery is unavailable.
        }

        return new ArrayList<>(modelsById.values());
    }

    private JsonNode postGenerateContent(String apiKey, String model, String prompt) throws IOException, InterruptedException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        content.putArray("parts").addObject().put("text", prompt);
        requestBody.putObject("generationConfig").put("temperature", 0.2);

        HttpRequest request = HttpRequest.newBuilder(buildGeminiUri("/" + toModelResourceName(model) + ":generateContent", apiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return parseGeminiResponse(response, "generateContent", model);
    }

    private JsonNode listModels(String apiKey) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(buildGeminiUri("/models?pageSize=100", apiKey)).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return parseGeminiResponse(response, "listModels", null);
    }

    public String resolveModel(UserAiSettings settings, AiServiceCallType callType) {
        Map<String, String> overrides = aiSettingsService.readModelOverrides(settings);
        if (overrides.containsKey(callType.name()) && overrides.get(callType.name()) != null && !overrides.get(callType.name()).isBlank()) {
            return overrides.get(callType.name());
        }
        return settings.getDefaultModel();
    }

    private JsonNode parseGeminiResponse(HttpResponse<String> response, String operation, String model) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String modelText = model == null ? "" : " for model " + model;
            LOG.error(
                "Gemini {} failed{} with status {} and body: {}",
                operation,
                modelText,
                response.statusCode(),
                abbreviate(response.body())
            );
            throw new IllegalStateException("Gemini " + operation + " failed with HTTP " + response.statusCode() + ".");
        }
        return objectMapper.readTree(response.body());
    }

    private String extractGeneratedText(JsonNode response) {
        ArrayNode candidates =
            response.has("candidates") && response.get("candidates").isArray() ? (ArrayNode) response.get("candidates") : null;
        if (candidates == null || candidates.isEmpty()) {
            JsonNode promptFeedback = response.path("promptFeedback");
            String blockReason = textOrNull(promptFeedback.path("blockReason"));
            if (blockReason != null) {
                throw new IllegalStateException("Gemini did not return a candidate: " + blockReason + ".");
            }
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (JsonNode part : candidates.get(0).path("content").path("parts")) {
            String text = textOrNull(part.path("text"));
            if (text != null) {
                parts.add(text);
            }
        }
        return String.join("\n", parts);
    }

    private String normalizeModelId(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.startsWith(GEMINI_MODEL_PREFIX) ? name.substring(GEMINI_MODEL_PREFIX.length()) : name;
    }

    private boolean supportsGenerateContent(JsonNode model) {
        return (
            containsIgnoreCase(model.path("supportedActions"), "generateContent") ||
            containsIgnoreCase(model.path("supportedGenerationMethods"), "generateContent")
        );
    }

    private String buildModelLabel(String modelId, JsonNode model) {
        String displayName = textOrNull(model.path("displayName"));
        return displayName == null ? modelId : displayName;
    }

    private boolean containsIgnoreCase(JsonNode node, String expected) {
        if (!node.isArray()) {
            return false;
        }
        for (JsonNode item : node) {
            if (expected.equalsIgnoreCase(item.asText())) {
                return true;
            }
        }
        return false;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String toModelResourceName(String model) {
        return model.startsWith(GEMINI_MODEL_PREFIX) ? model : GEMINI_MODEL_PREFIX + model;
    }

    private URI buildGeminiUri(String pathWithQuery, String apiKey) {
        String separator = pathWithQuery.contains("?") ? "&" : "?";
        return URI.create(GEMINI_API_BASE_URL + pathWithQuery + separator + "key=" + urlEncode(apiKey));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 1_000 ? text : text.substring(0, 1_000) + "...";
    }

    public enum AiProvider {
        GEMINI,
    }

    public enum AiServiceCallType {
        DEFAULT,
        AI_ASSISTANT,
        TRANSACTION_IMPORT,
        MARKET_DATA,
    }

    public record AiClientContext(AiProvider provider, String model, Object client) {}

    public record AiTextResponse(String model, String text) {}
}
