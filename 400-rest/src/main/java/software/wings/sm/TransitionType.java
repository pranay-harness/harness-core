/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.sm;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * List of transision types between states.
 *
 * @author Rishi
 */
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public enum TransitionType {
  /**
   * Success transition type.
   */
  SUCCESS,
  /**
   * Failure transition type.
   */
  FAILURE,
  /**
   * Abort transition type.
   */
  ABORT,
  /**
   * Repeat transition type.
   */
  REPEAT,
  /**
   * Fork transition type.
   */
  FORK,
  /**
   * Conditional transition type.
   */
  CONDITIONAL;
}
