package ovaro.plat4m.service;

import static ovaro.plat4m.security.SecurityUtils.API_KEY_ID_CLAIM;
import static ovaro.plat4m.security.SecurityUtils.AUTHORITIES_CLAIM;
import static ovaro.plat4m.security.SecurityUtils.JWT_ALGORITHM;
import static ovaro.plat4m.security.SecurityUtils.TOKEN_TYPE_API_KEY;
import static ovaro.plat4m.security.SecurityUtils.TOKEN_TYPE_CLAIM;
import static ovaro.plat4m.security.SecurityUtils.USER_ID_CLAIM;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.domain.UserApiKey;
import ovaro.plat4m.repository.UserApiKeyRepository;
import ovaro.plat4m.repository.UserRepository;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.dto.UserApiKeyCreateDTO;
import ovaro.plat4m.service.dto.UserApiKeyCreatedDTO;
import ovaro.plat4m.service.dto.UserApiKeyDTO;
import ovaro.plat4m.web.rest.errors.BadRequestAlertException;

@Service
@Transactional
public class UserApiKeyService {

    static final long DEFAULT_EXPIRY_MONTHS = 3L;

    private final UserApiKeyRepository userApiKeyRepository;
    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;

    public UserApiKeyService(UserApiKeyRepository userApiKeyRepository, UserRepository userRepository, JwtEncoder jwtEncoder) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserApiKeyDTO> listCurrentUserApiKeys() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user login not found", "userApiKey", "currentusernotfound")
        );
        return userApiKeyRepository
            .findAllByUserLoginAndRevokedAtIsNullOrderByCreatedDateDesc(login)
            .stream()
            .map(UserApiKeyDTO::new)
            .toList();
    }

    public UserApiKeyCreatedDTO createCurrentUserApiKey(UserApiKeyCreateDTO createDTO) {
        User user = SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneWithAuthoritiesByLogin)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", "userApiKey", "currentusernotfound"));

        Instant now = Instant.now();
        Instant expiresAt =
            createDTO.getExpiresAt() != null ? createDTO.getExpiresAt() : now.plus(DEFAULT_EXPIRY_MONTHS, ChronoUnit.MONTHS);
        if (!expiresAt.isAfter(now)) {
            throw new BadRequestAlertException("Expiry date must be in the future", "userApiKey", "invalidexpiry");
        }

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setId(UUID.randomUUID());
        userApiKey.setName(createDTO.getName().trim());
        userApiKey.setExpiresAt(expiresAt);
        userApiKey.setUser(user);
        userApiKey = userApiKeyRepository.save(userApiKey);

        return new UserApiKeyCreatedDTO(new UserApiKeyDTO(userApiKey), generateToken(userApiKey, user));
    }

    public void revokeCurrentUserApiKey(String keyId) {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user login not found", "userApiKey", "currentusernotfound")
        );
        UUID parsedKeyId = parseKeyId(keyId);
        UserApiKey userApiKey = userApiKeyRepository
            .findOneByIdAndUserLoginAndRevokedAtIsNull(parsedKeyId, login)
            .orElseThrow(() -> new BadRequestAlertException("API key not found", "userApiKey", "notfound"));
        userApiKey.setRevokedAt(Instant.now());
        userApiKeyRepository.save(userApiKey);
    }

    @Transactional(readOnly = true)
    public boolean isApiKeyTokenValid(String keyId, String login, Long userId) {
        UUID parsedKeyId = parseKeyId(keyId);
        return userApiKeyRepository
            .findOneById(parsedKeyId)
            .filter(apiKey -> apiKey.getRevokedAt() == null)
            .filter(apiKey -> apiKey.getExpiresAt().isAfter(Instant.now()))
            .filter(apiKey -> apiKey.getUser().getLogin().equals(login))
            .filter(apiKey -> apiKey.getUser().getId().equals(userId))
            .isPresent();
    }

    public void revokeExpiredApiKeys() {
        userApiKeyRepository
            .findAllByRevokedAtIsNullAndExpiresAtBefore(Instant.now())
            .forEach(apiKey -> apiKey.setRevokedAt(Instant.now()));
    }

    private String generateToken(UserApiKey userApiKey, User user) {
        String authorities = user
            .getAuthorities()
            .stream()
            .map(authority -> authority.getName())
            .collect(Collectors.joining(" "));
        Instant now = Instant.now();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(userApiKey.getExpiresAt())
            .subject(user.getLogin())
            .claim(AUTHORITIES_CLAIM, authorities)
            .claim(USER_ID_CLAIM, user.getId())
            .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_API_KEY)
            .claim(API_KEY_ID_CLAIM, userApiKey.getId().toString())
            .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    private UUID parseKeyId(String keyId) {
        try {
            return UUID.fromString(keyId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException("Invalid API key id", "userApiKey", "invalidid");
        }
    }
}
