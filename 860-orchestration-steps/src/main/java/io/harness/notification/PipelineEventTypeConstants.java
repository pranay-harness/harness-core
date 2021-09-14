/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineEventTypeConstants {
  String ALL_EVENTS = "AllEvents";
  String PIPELINE_START = "PipelineStart";
  String PIPELINE_SUCCESS = "PipelineSuccess";
  String PIPELINE_FAILED = "PipelineFailed";
  String PIPELINE_PAUSED = "PipelinePaused";
  String STAGE_SUCCESS = "StageSuccess";
  String STAGE_START = "StageStart";
  String STAGE_FAILED = "StageFailed";
  String STEP_FAILED = "StepFailed";
  String PIPELINE_END = "PipelineEnd";
}
