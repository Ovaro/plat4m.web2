package ovaro.plat4m.security;

import static ovaro.plat4m.security.SecurityUtils.API_KEY_ID_CLAIM;
import static ovaro.plat4m.security.SecurityUtils.TOKEN_TYPE_API_KEY;
import static ovaro.plat4m.security.SecurityUtils.TOKEN_TYPE_CLAIM;
import static ovaro.plat4m.security.SecurityUtils.USER_ID_CLAIM;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ovaro.plat4m.service.UserApiKeyService;

@Component
public class ApiKeyJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_API_KEY = new OAuth2Error("invalid_token", "The API key is invalid or revoked", null);

    private final UserApiKeyService userApiKeyService;

    public ApiKeyJwtValidator(UserApiKeyService userApiKeyService) {
        this.userApiKeyService = userApiKeyService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tokenType = token.getClaimAsString(TOKEN_TYPE_CLAIM);
        if (!TOKEN_TYPE_API_KEY.equals(tokenType)) {
            return OAuth2TokenValidatorResult.success();
        }

        String apiKeyId = token.getClaimAsString(API_KEY_ID_CLAIM);
        Long userId = token.getClaim(USER_ID_CLAIM);
        String login = token.getSubject();

        if (apiKeyId == null || userId == null || login == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_API_KEY);
        }

        return userApiKeyService.isApiKeyTokenValid(apiKeyId, login, userId)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(INVALID_API_KEY);
    }
}
