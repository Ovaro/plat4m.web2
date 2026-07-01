package ovaro.plat4m.service.dto;

public class FinanceTransactionImportCommitRequestDTO {

    private Boolean autoAcceptUnhandled;

    public Boolean getAutoAcceptUnhandled() {
        return autoAcceptUnhandled;
    }

    public void setAutoAcceptUnhandled(Boolean autoAcceptUnhandled) {
        this.autoAcceptUnhandled = autoAcceptUnhandled;
    }
}
