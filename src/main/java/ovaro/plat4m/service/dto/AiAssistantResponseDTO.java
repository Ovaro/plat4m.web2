package ovaro.plat4m.service.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AiAssistantResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String answer;
    private String model;
    private boolean aiConfigured;
    private List<String> contextSources = new ArrayList<>();

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isAiConfigured() {
        return aiConfigured;
    }

    public void setAiConfigured(boolean aiConfigured) {
        this.aiConfigured = aiConfigured;
    }

    public List<String> getContextSources() {
        return contextSources;
    }

    public void setContextSources(List<String> contextSources) {
        this.contextSources = contextSources;
    }
}
