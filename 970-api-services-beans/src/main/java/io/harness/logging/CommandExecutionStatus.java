package io.harness.logging;

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

  public static boolean isTerminalStatus(CommandExecutionStatus commandExecutionStatus) {
    return commandExecutionStatus == SUCCESS || commandExecutionStatus == FAILURE;
  }
}
