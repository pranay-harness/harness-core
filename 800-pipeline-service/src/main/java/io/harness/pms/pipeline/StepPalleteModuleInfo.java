package io.harness.pms.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class StepPalleteModuleInfo {
  String module;
  String category;
  boolean shouldShowCommonSteps;
  String commonStepCategory;
}
