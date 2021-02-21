package software.wings.beans.command;

import io.harness.logging.CommandExecutionStatus;

import org.apache.commons.lang3.NotImplementedException;

public class AzureARMCommandUnit extends AbstractCommandUnit {
  public AzureARMCommandUnit(String name) {
    super(CommandUnitType.AZURE_ARM);
    setName(name);
  }

  public AzureARMCommandUnit() {
    super(CommandUnitType.AZURE_ARM);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
