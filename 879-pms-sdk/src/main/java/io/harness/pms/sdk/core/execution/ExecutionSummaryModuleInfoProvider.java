/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.execution;

import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;

public interface ExecutionSummaryModuleInfoProvider {
  /**
   * Returns the module info to be stored at the pipeline level.
   * @param event
   * @return
   */
  PipelineModuleInfo getPipelineLevelModuleInfo(OrchestrationEvent event);

  /**
   * Returns the module info which should be published at the stage level.
   * @param event
   * @return
   */
  StageModuleInfo getStageLevelModuleInfo(OrchestrationEvent event);

  /**
   * Returns a boolean to check if the moduleInfoUpdate should run or not
   * @param event
   * @return
   */
  boolean shouldRun(OrchestrationEvent event);
}
