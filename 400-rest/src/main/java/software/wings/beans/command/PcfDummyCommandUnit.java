package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.command.CommandUnitType.PCF_DUMMY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;

import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(CDP)
public class PcfDummyCommandUnit extends AbstractCommandUnit {
  public PcfDummyCommandUnit(String name) {
    super(PCF_DUMMY);
    setName(name);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
