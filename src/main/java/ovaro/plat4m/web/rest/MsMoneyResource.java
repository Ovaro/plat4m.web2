package ovaro.plat4m.web.rest;

import jakarta.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.MsMoneyService;
import ovaro.plat4m.service.UserService;
import ovaro.plat4m.service.dto.MsMoneyImportDTO;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api")
public class MsMoneyResource {

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final Logger log = LoggerFactory.getLogger(MsMoneyResource.class);
    private MsMoneyService msMoneyService;
    private final UserService userService;

    public MsMoneyResource(MsMoneyService msMoneyService, UserService userService) {
        this.msMoneyService = msMoneyService;
        this.userService = userService;
    }

    /**
     * Upload a Microsoft Money file for importating.
     * @param file The file (multipart)
     * @param password Password for the money file.
     * @param redirectAttributes
     * @return Import status.
     * @throws IOException
     */
    @PostMapping(path = "/importFile", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<String> handleFileUpload(
        @RequestPart(name = "file") MultipartFile file,
        @RequestParam(value = "password", required = false) String password,
        RedirectAttributes redirectAttributes
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        log.info("File Upload Requested for: " + file.getOriginalFilename());
        Path savedFile = store(file);

        MsMoneyImportDTO monitor = new MsMoneyImportDTO("NOT SET");
        importFunction(monitor, u.get(), savedFile.toFile().getAbsolutePath(), password);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createAlert(applicationName, "import.completed", file.getOriginalFilename()))
            .build();
    }

    private Path store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file " + file.getOriginalFilename());
            }

            //String path = System.getProperty("java.io.tmpdir");
            Path tempDir = Files.createTempDirectory("import");
            Path temp = tempDir.resolve(UUID.randomUUID().toString());
            long s = Files.copy(file.getInputStream(), temp);

            log.info("Temp storage for: '" + file.getOriginalFilename() + "' was at: '" + temp.toString() + "''. Size=" + s + " bytes");
            return temp;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Import using a local file on the server. NOT TO BE USED BY PUBLIC.
     * @param moneyFile Path to money file.
     * @param password Password for the file.
     * @return
     * @throws IOException
     */
    @GetMapping("/import")
    public ResponseEntity<String> importMny(
        @RequestParam(value = "moneyFile") String moneyFile,
        @RequestParam(value = "password") String password
    ) throws IOException {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        if (!u.isPresent()) {
            throw new SecurityException("Couldn't find user: " + userLogin);
        }

        MsMoneyImportDTO monitor = new MsMoneyImportDTO("NOT SET");
        importFunction(monitor, u.get(), moneyFile, password);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createAlert(applicationName, "import.completed", moneyFile))
            .build();
    }

    private CompletableFuture<MsMoneyImportDTO> importFunction(MsMoneyImportDTO monitor, User user, String moneyFile, String password)
        throws FileNotFoundException, IOException {
        log.info("importMny: " + moneyFile + " password: " + (password != null ? "PROVIDED" : "NOT PROVIDED"));

        CompletableFuture<MsMoneyImportDTO> res = msMoneyService.startImport(user, moneyFile, password, monitor);
        log.info("Import started asynchronously: {}", monitor);
        return res;
    }

    /**
     * Import file and stream results (via SSE emitter) of the import.
     * @param moneyFile Path to money file.
     * @param password Password for the file.
     * @return
     */
    @GetMapping("/importAsync")
    @CrossOrigin
    public SseEmitter streamImport(
        @RequestParam(value = "moneyFile") String moneyFile,
        @RequestParam(value = "password", required = false) String password
    ) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new SecurityException("Current user login not found"));

        Optional<User> u = userService.getUserWithAuthoritiesByLogin(userLogin);

        if (!u.isPresent()) {
            throw new SecurityException("Couldn't find user: " + userLogin);
        }

        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);

        sseEmitter.onCompletion(() -> log.info("SseEmitter is completed"));

        sseEmitter.onTimeout(() -> log.info("SseEmitter is timed out"));

        sseEmitter.onError(ex -> log.info("SseEmitter got error:", ex));

        executor.execute(() -> {
            MsMoneyImportDTO monitor = new MsMoneyImportDTO("NOT SET");
            try {
                CompletableFuture<MsMoneyImportDTO> res = importFunction(monitor, u.get(), moneyFile, password);
                while (!res.isDone()) {
                    sseEmitter.send(
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss")) + " - " + monitor.getCurrentTask()
                    );
                    sleepMs(500, sseEmitter);
                }
                sseEmitter.complete();
            } catch (FileNotFoundException e1) {
                sseEmitter.completeWithError(e1);
                e1.printStackTrace();
            } catch (IOException e1) {
                sseEmitter.completeWithError(e1);
                e1.printStackTrace();
            }
        });

        log.info("streamImport exit");
        return sseEmitter;
    }

    ////////

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                executor.shutdown();
                try {
                    executor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.error(e.toString());
                }
            })
        );
    }

    private void sleepMs(int ms, SseEmitter sseEmitter) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
            sseEmitter.completeWithError(e);
        }
    }
}
