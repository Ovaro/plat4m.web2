package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ovaro.plat4m.domain.Source;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.SourceService;
import ovaro.plat4m.service.UserService;

@RestController
@RequestMapping("/api")
public class SourceResource {

    private final Logger log = LoggerFactory.getLogger(SourceResource.class);
    private SourceService sourceService;
    private final UserService userService;

    public SourceResource(SourceService sourceService, UserService userService) {
        this.sourceService = sourceService;
        this.userService = userService;
    }

    @GetMapping("/source")
    public ResponseEntity<List<Source>> listAll() throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        if (!u.isPresent()) {
            throw new SecurityException("Couldn't find user: " + userLogin);
        }
        List<Source> res = this.sourceService.listAllForUser(u.get());

        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
