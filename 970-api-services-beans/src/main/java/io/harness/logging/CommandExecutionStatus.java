package io.harness.logging;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * The Enum CommandExecutionStatus.
 */
@OwnedBy(HarnessTeam.CDP)
public enum CommandExecutionStatus {
  /**
   * Success execution status.
   */
  SUCCESS(UnitStatus.SUCCESS),
  /**
   * Failure execution status.
   */
  FAILURE(UnitStatus.FAILURE),
  /**
   * Running execution status.
   */
  RUNNING(UnitStatus.RUNNING),

  /**
   * Queued execution status.
   */
  QUEUED(UnitStatus.QUEUED),

  /*
   *  Skipped execution status
   * */
  SKIPPED(UnitStatus.SKIPPED);

  private UnitStatus unitStatus;

  CommandExecutionStatus(UnitStatus unitStatus) {
    this.unitStatus = unitStatus;
  }

  public static boolean isTerminalStatus(CommandExecutionStatus commandExecutionStatus) {
    return commandExecutionStatus == SUCCESS || commandExecutionStatus == FAILURE;
  }

  public UnitStatus getUnitStatus() {
    return unitStatus;
  }

  public static CommandExecutionStatus getCommandExecutionStatus(UnitStatus unitStatus) {
    for (CommandExecutionStatus commandExecutionStatus : CommandExecutionStatus.values()) {
      if (commandExecutionStatus.getUnitStatus().equals(unitStatus)) {
        return commandExecutionStatus;
      }
    }

    return null;
  }
}
