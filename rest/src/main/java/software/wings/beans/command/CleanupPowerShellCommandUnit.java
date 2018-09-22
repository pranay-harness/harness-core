package software.wings.beans.command;

import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

public class CleanupPowerShellCommandUnit extends AbstractCommandUnit {
  public static final String CLEANUP_POWERSHELL_UNIT_NAME = "Cleanup";

  public CleanupPowerShellCommandUnit() {
    super(CommandUnitType.EXEC);
    setName(CLEANUP_POWERSHELL_UNIT_NAME);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    return CommandExecutionStatus.SUCCESS;
  }
}
