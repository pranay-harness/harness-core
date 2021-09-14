/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface OrchestrationConstants {
  String STAGE_SUCCESS = "OnStageSuccess";
  String STAGE_FAILURE = "OnStageFailure";
  String PIPELINE_SUCCESS = "OnPipelineSuccess";
  String PIPELINE_FAILURE = "OnPipelineFailure";
  String ALWAYS = "Always";
  String CURRENT_STATUS = "currentStatus";
}
