package ovaro.plat4m.web.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ovaro.plat4m.domain.FinanceFX;
import ovaro.plat4m.service.FinanceFXService;
import ovaro.plat4m.service.dto.FinanceFXImportRequestDTO;
import ovaro.plat4m.service.dto.FinanceFXImportResultDTO;
import ovaro.plat4m.service.dto.FinanceFXUpdateDTO;

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

    @GetMapping("/fx/history")
    public ResponseEntity<List<FinanceFX>> listFXHistory(@RequestParam String from, @RequestParam String to) throws IOException {
        try {
            return new ResponseEntity<>(this.financeFXService.getFXHistory(from, to), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/fx")
    public ResponseEntity<FinanceFX> createFX(@RequestBody FinanceFXUpdateDTO update) throws IOException {
        try {
            return new ResponseEntity<>(this.financeFXService.createFX(update), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/fx/{fxId}")
    public ResponseEntity<FinanceFX> updateFX(@PathVariable(name = "fxId") UUID fxId, @RequestBody FinanceFXUpdateDTO update)
        throws IOException {
        try {
            return new ResponseEntity<>(this.financeFXService.updateFX(fxId, update), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @DeleteMapping("/fx/{fxId}")
    public ResponseEntity<Void> deleteFX(@PathVariable(name = "fxId") UUID fxId) throws IOException {
        try {
            this.financeFXService.deleteFX(fxId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/fx/import/frankfurter")
    public ResponseEntity<FinanceFXImportResultDTO> importFrankfurterRates(@RequestBody FinanceFXImportRequestDTO request)
        throws IOException {
        try {
            return new ResponseEntity<>(this.financeFXService.importFrankfurterRates(request), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @GetMapping("/fx2")
    public ResponseEntity<FinanceFX> listFX(@RequestParam String from, @RequestParam String to) throws IOException {
        FinanceFX fx = this.financeFXService.getLatestFX(from, to, LocalDate.now());

        return new ResponseEntity<>(fx, HttpStatus.OK);
    }
}
