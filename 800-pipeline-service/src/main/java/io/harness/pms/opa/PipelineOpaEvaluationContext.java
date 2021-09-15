package io.harness.pms.opa;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineOpaEvaluationContext {
  Object pipeline;
  UserOpaEvaluationContext user;
  String action;
}
