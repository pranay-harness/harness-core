/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.command;

import io.harness.logging.CommandExecutionStatus;

public class CleanupPowerShellCommandUnit extends AbstractCommandUnit {
  public static final String CLEANUP_POWERSHELL_UNIT_NAME = "Cleanup";

  public CleanupPowerShellCommandUnit() {
    super(CommandUnitType.EXEC);
    setName(CLEANUP_POWERSHELL_UNIT_NAME);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    return CommandExecutionStatus.SUCCESS;
  }
}
