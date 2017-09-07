package software.wings.service.impl.analysis;

import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by rsingh on 5/18/17.
 */
public class LogDataCollectionTaskResult implements NotifyResponseData {
  private LogDataCollectionTaskStatus status;
  private String errorMessage;

  public LogDataCollectionTaskStatus getStatus() {
    return status;
  }

  public void setStatus(LogDataCollectionTaskStatus status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public String toString() {
    return "LogDataCollectionTaskResult{"
        + "status=" + status + ", errorMessage='" + errorMessage + '\'' + '}';
  }

  public static final class Builder {
    private LogDataCollectionTaskStatus status;
    private String errorMessage;

    private Builder() {}

    /**
     * A command execution result builder.
     *
     * @return the builder
     */
    public static LogDataCollectionTaskResult.Builder aLogDataCollectionTaskResult() {
      return new LogDataCollectionTaskResult.Builder();
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public LogDataCollectionTaskResult.Builder withStatus(LogDataCollectionTaskStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With error message builder.
     *
     * @param errorMessage the error message
     * @return the builder
     */
    public LogDataCollectionTaskResult.Builder withErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    /**
     * Build command execution result.
     *
     * @return the command execution result
     */
    public LogDataCollectionTaskResult build() {
      LogDataCollectionTaskResult logDataCollectionTaskResult = new LogDataCollectionTaskResult();
      logDataCollectionTaskResult.setStatus(status);
      logDataCollectionTaskResult.setErrorMessage(errorMessage);
      return logDataCollectionTaskResult;
    }
  }

  public enum LogDataCollectionTaskStatus {
    /**
     * Success execution status.
     */
    SUCCESS,
    /**
     * Failure execution status.
     */
    FAILURE;
  }
}
