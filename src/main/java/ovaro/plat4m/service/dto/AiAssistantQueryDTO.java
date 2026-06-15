package ovaro.plat4m.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AiAssistantQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 2000)
    private String message;

    private List<AiAssistantMessageDTO> history = new ArrayList<>();

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AiAssistantMessageDTO> getHistory() {
        return history;
    }

    public void setHistory(List<AiAssistantMessageDTO> history) {
        this.history = history;
    }
}
