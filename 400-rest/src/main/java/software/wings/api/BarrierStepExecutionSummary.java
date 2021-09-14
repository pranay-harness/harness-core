/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import software.wings.sm.StepExecutionSummary;

import lombok.Getter;
import lombok.Setter;

public class BarrierStepExecutionSummary extends StepExecutionSummary {
  @Getter @Setter private String identifier;
}
