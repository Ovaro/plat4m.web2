package ovaro.plat4m.domain;

import java.time.Instant;

/**
 * Importing
 */
public class FinanceImportStatus {

    String id;
    int taskNumber = 0;
    String taskName = "unnamed";
    boolean taskFinished = false;
    boolean importFinished = false;
    String error = null;
    int numInput = 0;
    int numCreated = 0;
    int numDeleted = 0;
    int numUpdated = 0;
    Long duration;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTaskNumber() {
        return taskNumber;
    }

    public void setTaskNumber(int taskNumber) {
        this.taskNumber = taskNumber;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public boolean isTaskFinished() {
        return taskFinished;
    }

    public void setTaskFinished(boolean taskFinished) {
        this.taskFinished = taskFinished;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getNumInput() {
        return numInput;
    }

    public void setNumInput(int numInput) {
        this.numInput = numInput;
    }

    public int getNumCreated() {
        return numCreated;
    }

    public void setNumCreated(int numCreated) {
        this.numCreated = numCreated;
    }

    public int getNumDeleted() {
        return numDeleted;
    }

    public void setNumDeleted(int numDeleted) {
        this.numDeleted = numDeleted;
    }

    public int getNumUpdated() {
        return numUpdated;
    }

    public void setNumUpdated(int numUpdated) {
        this.numUpdated = numUpdated;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public boolean isImportFinished() {
        return importFinished;
    }

    public void setImportFinished(boolean importFinished) {
        this.importFinished = importFinished;
    }
}
