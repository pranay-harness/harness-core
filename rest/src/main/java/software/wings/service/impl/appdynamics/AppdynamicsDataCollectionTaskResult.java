package software.wings.service.impl.appdynamics;

import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by rsingh on 5/18/17.
 */
public class AppdynamicsDataCollectionTaskResult implements NotifyResponseData {
  private AppdynamicsDataCollectionTaskStatus status;
  private String errorMessage;

  public AppdynamicsDataCollectionTaskStatus getStatus() {
    return status;
  }

  public void setStatus(AppdynamicsDataCollectionTaskStatus status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String toString() {
    return "AppdynamicsDataCollectionTaskResult{"
        + "status=" + status + ", errorMessage='" + errorMessage + '\'' + '}';
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public static final class Builder {
    private AppdynamicsDataCollectionTaskStatus status;
    private String errorMessage;

    private Builder() {}

    /**
     * A command execution result builder.
     *
     * @return the builder
     */
    public static AppdynamicsDataCollectionTaskResult.Builder aAppdynamicsDataCollectionTaskResult() {
      return new AppdynamicsDataCollectionTaskResult.Builder();
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public AppdynamicsDataCollectionTaskResult.Builder withStatus(AppdynamicsDataCollectionTaskStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With error message builder.
     *
     * @param errorMessage the error message
     * @return the builder
     */
    public AppdynamicsDataCollectionTaskResult.Builder withErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    /**
     * Build command execution result.
     *
     * @return the command execution result
     */
    public AppdynamicsDataCollectionTaskResult build() {
      AppdynamicsDataCollectionTaskResult appdynamicsDataCollectionTaskResult =
          new AppdynamicsDataCollectionTaskResult();
      appdynamicsDataCollectionTaskResult.setStatus(status);
      appdynamicsDataCollectionTaskResult.setErrorMessage(errorMessage);
      return appdynamicsDataCollectionTaskResult;
    }
  }

  public enum AppdynamicsDataCollectionTaskStatus {
    /**
     * Success execution status.
     */
    SUCCESS, /**
              * Failure execution status.
              */
    FAILURE;
  }
}
