package io.harness.cdng.pipeline.beans;

import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class RollbackOptionalChildrenParameters implements StepParameters {
  @Singular List<RollbackNode> parallelNodes;
}
