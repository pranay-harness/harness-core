package io.harness.cdng.pipeline.beans;

import io.harness.state.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class RollbackOptionalChildChainStepParameters implements StepParameters {
  @Singular List<RollbackNode> childNodes;
}
