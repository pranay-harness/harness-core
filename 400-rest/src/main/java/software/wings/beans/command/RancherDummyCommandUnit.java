package software.wings.beans.command;

import io.harness.logging.CommandExecutionStatus;
import org.apache.commons.lang3.NotImplementedException;

import static software.wings.beans.command.CommandUnitType.K8S_DUMMY;
import static software.wings.beans.command.CommandUnitType.RANCHER_DUMMY;

public class RancherDummyCommandUnit extends AbstractCommandUnit {
  public RancherDummyCommandUnit(String name) {
    super(RANCHER_DUMMY);
    setName(name);
  }

  public RancherDummyCommandUnit() {
    super(RANCHER_DUMMY);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}