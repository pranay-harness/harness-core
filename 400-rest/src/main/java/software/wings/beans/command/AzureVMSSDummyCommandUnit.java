/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.command;

import static software.wings.beans.command.CommandUnitType.AZURE_VMSS_DUMMY;

import io.harness.logging.CommandExecutionStatus;

import org.apache.commons.lang3.NotImplementedException;

public class AzureVMSSDummyCommandUnit extends AbstractCommandUnit {
  public AzureVMSSDummyCommandUnit(String name) {
    super(AZURE_VMSS_DUMMY);
    setName(name);
  }

  public AzureVMSSDummyCommandUnit() {
    super(AZURE_VMSS_DUMMY);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
