/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

/**
 *
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * The enum Execution strategy.
 *
 * @author Rishi
 */
@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDC)
public enum ExecutionStrategy {
  /**
   * Serial execution strategy.
   */
  SERIAL,
  /**
   * Parallel execution strategy.
   */
  PARALLEL;
}
