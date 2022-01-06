/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * List of transision types between states.
 *
 * @author Rishi
 */
@OwnedBy(HarnessTeam.CDC)
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
