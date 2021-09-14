/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import software.wings.sm.StepExecutionSummary;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ScriptStateExecutionSummary extends StepExecutionSummary {
  private String activityId;
  private Map<String, String> sweepingOutputEnvVariables;
}
