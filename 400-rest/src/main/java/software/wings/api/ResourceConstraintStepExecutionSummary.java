/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
