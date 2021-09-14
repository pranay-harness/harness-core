/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.ecs.response;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class EcsServiceSetupResponse extends EcsCommandResponse {
  private boolean isBlueGreen;
  private ContainerSetupCommandUnitExecutionData setupData;

  @Builder
  public EcsServiceSetupResponse(CommandExecutionStatus commandExecutionStatus, String output, boolean isBlueGreen,
      ContainerSetupCommandUnitExecutionData setupData, boolean timeoutFailure) {
    super(commandExecutionStatus, output, timeoutFailure);
    this.isBlueGreen = isBlueGreen;
    this.setupData = setupData;
  }
}
