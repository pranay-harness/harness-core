package software.wings.beans.command;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.DelegateMetaInfo;
import io.harness.delegate.task.protocol.DelegateTaskNotifyResponseData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommandExecutionResult implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private CommandExecutionStatus status;
  private CommandExecutionData commandExecutionData;
  private String errorMessage;

  /**
   * The Enum CommandExecutionStatus.
   */
  public enum CommandExecutionStatus {
    /**
     * Success execution status.
     */
    SUCCESS,
    /**
     * Failure execution status.
     */
    FAILURE,
    /**
     * Running execution status.
     */
    RUNNING,

    /**
     * Queued execution status.
     */
    QUEUED;

    public static CommandExecutionStatus translateExecutionStatus(ExecutionStatus executionStatus) {
      switch (executionStatus) {
        case SUCCESS:
          return SUCCESS;
        case FAILED:
          return FAILURE;
        case RUNNING:
          return RUNNING;
        case QUEUED:
          return QUEUED;
        default:
          throw new IllegalArgumentException("invalid status: " + executionStatus);
      }
    }
  }
}
