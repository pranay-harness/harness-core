/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.sm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * The Enum StateTypeScope.
 *
 * @author Rishi
 */
@OwnedBy(HarnessTeam.CDC)
public enum StateTypeScope {
  /**
   * Pipeline stencils state type scope.
   */
  PIPELINE_STENCILS,
  /**
   * Orchestration stencils state type scope.
   */
  ORCHESTRATION_STENCILS,
  /**
   * Command stencils state type scope.
   */
  COMMAND_STENCILS,
  /**
   * None stencils state type scope.
   */
  COMMON,
  /**
   * Deployment state type scope.
   */
  NONE;
}
