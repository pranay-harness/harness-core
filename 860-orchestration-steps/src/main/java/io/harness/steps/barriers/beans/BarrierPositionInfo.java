/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.barriers.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "BarrierPositionInfoKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PIPELINE)
public class BarrierPositionInfo {
  String planExecutionId;
  List<BarrierPosition> barrierPositionList;

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "BarrierPositionKeys")
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class BarrierPosition {
    String pipelineSetupId;
    String pipelineRuntimeId;

    String stageSetupId;
    String stageRuntimeId;

    String stepGroupSetupId;
    String stepGroupRuntimeId;

    String stepSetupId;
    String stepRuntimeId;

    boolean stepGroupRollback;

    public enum BarrierPositionType { STAGE, STEP_GROUP, STEP }
  }
}
