package software.wings.beans.command;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;

public class HelmDummyCommandUnit extends AbstractCommandUnit {
  public static final String Init = "Initialize";
  public static final String Prepare = "Prepare";
  public static final String InstallUpgrade = "Install / Upgrade";
  public static final String WaitForSteadyState = "Wait For Steady State";
  public static final String WrapUp = "Wrap Up";
  public static final String Rollback = "Rollback";

  public HelmDummyCommandUnit(String name) {
    this.setName(name);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    return null;
  }
}
