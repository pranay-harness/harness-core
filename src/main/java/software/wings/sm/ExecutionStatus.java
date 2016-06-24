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
  FAILED, /**
           * Error execution status.
           */
  ERROR;

  public static class ExecutionStatusData implements NotifyResponseData {
    private ExecutionStatus executionStatus;

    public ExecutionStatus getExecutionStatus() {
      return executionStatus;
    }

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

    public static final class Builder {
      private ExecutionStatus executionStatus;

      private Builder() {}

      public static Builder anExecutionStatusData() {
        return new Builder();
      }

      public Builder withExecutionStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
        return this;
      }

      public Builder but() {
        return anExecutionStatusData().withExecutionStatus(executionStatus);
      }

      public ExecutionStatusData build() {
        ExecutionStatusData executionStatusData = new ExecutionStatusData();
        executionStatusData.setExecutionStatus(executionStatus);
        return executionStatusData;
      }
    }
  }
}
