/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.plan.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class SetupAbstractionKeys {
  public final String accountId = "accountId";
  public final String orgIdentifier = "orgIdentifier";
  public final String projectIdentifier = "projectIdentifier";
  public final String inputSetYaml = "inputSetYaml";
  public final String pipelineIdentifier = "pipelineIdentifier";
  public final String eventPayload = "eventPayload";
  public final String triggerInfo = "triggerInfo";
  public final String runSequence = "runSequence";
  public final String trigger = "trigger";
}
