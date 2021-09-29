package io.harness.cdng.pipeline.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@RecasterAlias("io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters")
public class RollbackOptionalChildrenParameters implements StepParameters {
  @Singular List<RollbackNode> parallelNodes;
}
