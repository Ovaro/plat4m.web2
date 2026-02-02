package ovaro.plat4m.service.dto;

public class MsMoneyImportDTO {

    private String currentTask;

    public MsMoneyImportDTO() {}

    public MsMoneyImportDTO(String currentTask) {
        this.currentTask = currentTask;
    }

    public String getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(String currentTask) {
        this.currentTask = currentTask;
    }

    @Override
    public String toString() {
        return "MsMoneyImportDTO [currentTask=" + currentTask + "]";
    }
}
