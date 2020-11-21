package software.wings.beans.command;

import static software.wings.beans.command.CommandUnitType.SPOTINST_DUMMY;

import io.harness.logging.CommandExecutionStatus;

import org.apache.commons.lang3.NotImplementedException;

public class SpotinstDummyCommandUnit extends AbstractCommandUnit {
  public SpotinstDummyCommandUnit(String name) {
    super(SPOTINST_DUMMY);
    setName(name);
  }

  public SpotinstDummyCommandUnit() {
    super(SPOTINST_DUMMY);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
