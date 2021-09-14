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

public class ResourceConstraintStepExecutionSummary extends StepExecutionSummary {
  @Getter @Setter private String resourceConstraintName;
  @Getter @Setter private int resourceConstraintCapacity;
  @Getter @Setter private String unit;
  @Getter @Setter private int usage;
  @Getter @Setter private int alreadyAcquiredPermits;
}
