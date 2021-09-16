package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class HelmDummyCommandUnit extends AbstractCommandUnit {
  public static final String FetchFiles = "Fetch Files";
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
