/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

/**
 *
 */

package software.wings.sm;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * The Enum StateTypeScope.
 *
 * @author Rishi
 */
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
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
