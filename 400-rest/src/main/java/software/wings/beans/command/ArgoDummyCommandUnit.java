package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.command.CommandUnitType.ARGO_DUMMY;
import static software.wings.beans.command.CommandUnitType.SPOTINST_DUMMY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;

import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(CDP)
public class ArgoDummyCommandUnit extends AbstractCommandUnit {
  public ArgoDummyCommandUnit(String name) {
    super(ARGO_DUMMY);
    setName(name);
  }

  public ArgoDummyCommandUnit() {
    super(ARGO_DUMMY);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
