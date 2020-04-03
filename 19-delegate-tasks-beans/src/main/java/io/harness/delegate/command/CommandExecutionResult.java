package io.harness.delegate.command;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
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
    QUEUED,

    /*
     *  Skipped execution status
     * */
    SKIPPED;
  }
}
