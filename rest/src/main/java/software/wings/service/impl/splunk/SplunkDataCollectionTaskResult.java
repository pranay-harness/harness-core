package software.wings.service.impl.splunk;

import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by rsingh on 5/18/17.
 */
public class SplunkDataCollectionTaskResult implements NotifyResponseData {
  private SplunkDataCollectionTaskStatus status;
  private String errorMessage;

  public SplunkDataCollectionTaskStatus getStatus() {
    return status;
  }

  public void setStatus(SplunkDataCollectionTaskStatus status) {
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
    return "SplunkDataCollectionTaskResult{"
        + "status=" + status + ", errorMessage='" + errorMessage + '\'' + '}';
  }

  public static final class Builder {
    private SplunkDataCollectionTaskStatus status;
    private String errorMessage;

    private Builder() {}

    /**
     * A command execution result builder.
     *
     * @return the builder
     */
    public static SplunkDataCollectionTaskResult.Builder aSplunkDataCollectionTaskResult() {
      return new SplunkDataCollectionTaskResult.Builder();
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public SplunkDataCollectionTaskResult.Builder withStatus(SplunkDataCollectionTaskStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With error message builder.
     *
     * @param errorMessage the error message
     * @return the builder
     */
    public SplunkDataCollectionTaskResult.Builder withErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    /**
     * Build command execution result.
     *
     * @return the command execution result
     */
    public SplunkDataCollectionTaskResult build() {
      SplunkDataCollectionTaskResult splunkDataCollectionTaskResult = new SplunkDataCollectionTaskResult();
      splunkDataCollectionTaskResult.setStatus(status);
      splunkDataCollectionTaskResult.setErrorMessage(errorMessage);
      return splunkDataCollectionTaskResult;
    }
  }

  public enum SplunkDataCollectionTaskStatus {
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
