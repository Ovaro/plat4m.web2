package ovaro.plat4m.web.rest.vm;

/**
 * View Model returned after public user registration.
 */
public class RegistrationResponseVM {

    private boolean activationEmailSent;

    private String message;

    public RegistrationResponseVM(boolean activationEmailSent, String message) {
        this.activationEmailSent = activationEmailSent;
        this.message = message;
    }

    public boolean isActivationEmailSent() {
        return activationEmailSent;
    }

    public void setActivationEmailSent(boolean activationEmailSent) {
        this.activationEmailSent = activationEmailSent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
