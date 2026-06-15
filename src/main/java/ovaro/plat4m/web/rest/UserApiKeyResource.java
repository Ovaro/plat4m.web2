package ovaro.plat4m.web.rest;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ovaro.plat4m.service.UserApiKeyService;
import ovaro.plat4m.service.dto.UserApiKeyCreateDTO;
import ovaro.plat4m.service.dto.UserApiKeyCreatedDTO;
import ovaro.plat4m.service.dto.UserApiKeyDTO;

@RestController
@RequestMapping("/api/account/api-keys")
public class UserApiKeyResource {

    private final UserApiKeyService userApiKeyService;

    public UserApiKeyResource(UserApiKeyService userApiKeyService) {
        this.userApiKeyService = userApiKeyService;
    }

    @GetMapping
    public List<UserApiKeyDTO> listCurrentUserApiKeys() {
        return userApiKeyService.listCurrentUserApiKeys();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserApiKeyCreatedDTO createCurrentUserApiKey(@Valid @RequestBody UserApiKeyCreateDTO createDTO) {
        return userApiKeyService.createCurrentUserApiKey(createDTO);
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeCurrentUserApiKey(@PathVariable String keyId) {
        userApiKeyService.revokeCurrentUserApiKey(keyId);
    }
}
