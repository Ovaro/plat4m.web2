package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.FinanceDemoDataService;
import ovaro.plat4m.service.UserService;
import ovaro.plat4m.service.dto.FinanceDemoDataBatchDTO;
import ovaro.plat4m.service.dto.FinanceDemoDataDeleteResultDTO;
import ovaro.plat4m.service.dto.FinanceDemoDataOptionsDTO;
import ovaro.plat4m.service.dto.FinanceDemoDataRequestDTO;
import ovaro.plat4m.service.dto.FinanceDemoDataResultDTO;

@RestController
@RequestMapping("/api/demo-data")
public class FinanceDemoDataResource {

    private final FinanceDemoDataService financeDemoDataService;
    private final UserService userService;

    public FinanceDemoDataResource(FinanceDemoDataService financeDemoDataService, UserService userService) {
        this.financeDemoDataService = financeDemoDataService;
        this.userService = userService;
    }

    @GetMapping("/options")
    public ResponseEntity<FinanceDemoDataOptionsDTO> options() {
        return new ResponseEntity<>(financeDemoDataService.options(), HttpStatus.OK);
    }

    @GetMapping("/batches")
    public ResponseEntity<List<FinanceDemoDataBatchDTO>> batches() {
        return new ResponseEntity<>(financeDemoDataService.batches(getCurrentUser()), HttpStatus.OK);
    }

    @PostMapping("/preview")
    public ResponseEntity<FinanceDemoDataResultDTO> preview(@RequestBody FinanceDemoDataRequestDTO request) throws IOException {
        try {
            return new ResponseEntity<>(financeDemoDataService.preview(getCurrentUser(), request), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<FinanceDemoDataResultDTO> generate(@RequestBody FinanceDemoDataRequestDTO request) throws IOException {
        try {
            return new ResponseEntity<>(financeDemoDataService.generate(getCurrentUser(), request), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @DeleteMapping("/batches/{batchId}")
    public ResponseEntity<FinanceDemoDataDeleteResultDTO> deleteBatch(@PathVariable UUID batchId) {
        try {
            return new ResponseEntity<>(financeDemoDataService.deleteBatch(getCurrentUser(), batchId), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    private User getCurrentUser() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));
        Optional<User> user = userService.getUserWithAuthoritiesByLogin(userLogin);
        return user.orElseThrow(() -> new SecurityException("Current user not found"));
    }
}
