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

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class EcsRunTaskDeployResponse extends EcsCommandResponse {
  private List<String> previousRegisteredRunTaskDefinitions;
  private List<String> previousRunTaskArns;
  private List<String> newRegisteredRunTaskDefinitions;
  private List<String> newRunTaskArns;

  @Builder
  public EcsRunTaskDeployResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<String> newRunTaskArns, List<String> newRegisteredRunTaskDefinitions, List<String> previousRunTaskArns,
      List<String> previousRegisteredRunTaskDefinitions, boolean timeoutFailure) {
    super(commandExecutionStatus, output, timeoutFailure);
    this.newRegisteredRunTaskDefinitions = newRegisteredRunTaskDefinitions;
    this.previousRegisteredRunTaskDefinitions = previousRegisteredRunTaskDefinitions;
    this.newRunTaskArns = newRunTaskArns;
    this.previousRunTaskArns = previousRunTaskArns;
  }
}
