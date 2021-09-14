/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.command;

import static software.wings.beans.command.CommandUnitType.K8S_DUMMY;

import io.harness.logging.CommandExecutionStatus;

import org.apache.commons.lang3.NotImplementedException;

public class K8sDummyCommandUnit extends AbstractCommandUnit {
  public K8sDummyCommandUnit(String name) {
    super(K8S_DUMMY);
    setName(name);
  }

  public K8sDummyCommandUnit() {
    super(K8S_DUMMY);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
