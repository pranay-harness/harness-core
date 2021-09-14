/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
