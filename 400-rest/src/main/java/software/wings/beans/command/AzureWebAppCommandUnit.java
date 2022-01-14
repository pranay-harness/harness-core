package software.wings.beans.command;

import io.harness.logging.CommandExecutionStatus;

import org.apache.commons.lang3.NotImplementedException;

public class AzureWebAppCommandUnit extends AbstractCommandUnit {
  public AzureWebAppCommandUnit(String name) {
    super(CommandUnitType.AZURE_WEBAPP);
    setName(name);
  }

  public AzureWebAppCommandUnit() {
    super(CommandUnitType.AZURE_WEBAPP);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
