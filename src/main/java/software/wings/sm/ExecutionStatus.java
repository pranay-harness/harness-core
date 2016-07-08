package software.wings.sm;

import software.wings.waitnotify.NotifyResponseData;

import java.util.Objects;

/**
 * Describes possible execution statuses for a state.
 *
 * @author Rishi
 */
public enum ExecutionStatus {
  /**
   * New execution status.
   */
  NEW, /**
        * Running execution status.
        */
  RUNNING, /**
            * Success execution status.
            */
  SUCCESS, /**
            * Aborted execution status.
            */
  ABORTED, /**
            * Failed execution status.
            */
  FAILED,
  QUEUED,
  /**
   * Scheduled execution status.
   */
  SCHEDULED, /**
              * Error execution status.
              */
  ERROR;

  /**
   * The type Execution status data.
   */
  public static class ExecutionStatusData implements NotifyResponseData {
    private ExecutionStatus executionStatus;

    /**
     * Gets execution status.
     *
     * @return the execution status
     */
    public ExecutionStatus getExecutionStatus() {
      return executionStatus;
    }

    /**
     * Sets execution status.
     *
     * @param executionStatus the execution status
     */
    public void setExecutionStatus(ExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
    }

    @Override
    public int hashCode() {
      return Objects.hash(executionStatus);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final ExecutionStatusData other = (ExecutionStatusData) obj;
      return Objects.equals(this.executionStatus, other.executionStatus);
    }

    /**
     * The type Builder.
     */
    public static final class Builder {
      private ExecutionStatus executionStatus;

      private Builder() {}

      /**
       * An execution status data builder.
       *
       * @return the builder
       */
      public static Builder anExecutionStatusData() {
        return new Builder();
      }

      /**
       * With execution status builder.
       *
       * @param executionStatus the execution status
       * @return the builder
       */
      public Builder withExecutionStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
        return this;
      }

      /**
       * But builder.
       *
       * @return the builder
       */
      public Builder but() {
        return anExecutionStatusData().withExecutionStatus(executionStatus);
      }

      /**
       * Build execution status data.
       *
       * @return the execution status data
       */
      public ExecutionStatusData build() {
        ExecutionStatusData executionStatusData = new ExecutionStatusData();
        executionStatusData.setExecutionStatus(executionStatus);
        return executionStatusData;
      }
    }
  }
}
