package ovaro.plat4m.web.rest.errors;

public class SecurityException extends RuntimeException {

    private SecurityException(String message) {
        super(message);
    }
}
