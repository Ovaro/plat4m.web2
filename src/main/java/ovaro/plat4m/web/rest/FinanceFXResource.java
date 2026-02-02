package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.service.FinanceFXService;

@RestController
@RequestMapping("/api")
public class FinanceFXResource {

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final Logger log = LoggerFactory.getLogger(FinanceFXResource.class);
    private FinanceFXService financeFXService;

    public FinanceFXResource(FinanceFXService financeFXService) {
        this.financeFXService = financeFXService;
    }

    @GetMapping("/fx")
    public ResponseEntity<List<FinanceFX>> listFX() throws IOException {
        List<FinanceFX> fxs = this.financeFXService.getLatestFXAll();

        return new ResponseEntity<>(fxs, HttpStatus.OK);
    }

    @GetMapping("/fx2")
    public ResponseEntity<FinanceFX> listFX(@RequestParam String from, @RequestParam String to) throws IOException {
        FinanceFX fx = this.financeFXService.getLatestFX(from, to, LocalDate.now());

        return new ResponseEntity<>(fx, HttpStatus.OK);
    }
}
