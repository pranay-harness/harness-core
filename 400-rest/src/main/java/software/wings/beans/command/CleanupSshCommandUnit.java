/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.command;

import io.harness.logging.CommandExecutionStatus;

import java.io.File;

/**
 * Created by peeyushaggarwal on 7/26/16.
 */
public class CleanupSshCommandUnit extends SshCommandUnit {
  /**
   * The constant CLEANUP_UNIT.
   */
  public static final String CLEANUP_UNIT = "Cleanup";

  /**
   * Instantiates a new Init command unit.
   */
  public CleanupSshCommandUnit() {
    super(CommandUnitType.EXEC);
    setName(CLEANUP_UNIT);
  }

  @Override
  public CommandExecutionStatus executeInternal(ShellCommandExecutionContext context) {
    return context.executeCommandString("rm -rf " + new File("/tmp", context.getActivityId()).getAbsolutePath());
  }
}
