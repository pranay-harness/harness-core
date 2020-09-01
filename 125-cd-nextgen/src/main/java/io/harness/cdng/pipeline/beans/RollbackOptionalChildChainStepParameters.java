package io.harness.cdng.pipeline.beans;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RollbackOptionalChildChainStepParameters implements StepParameters {
  @Singular List<RollbackNode> childNodes;
}
