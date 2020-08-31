package io.harness.state.core.section.chain;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RollbackOptionalChildChainStepParameters implements StepParameters {
  @Singular List<Node> childNodes;

  @Value
  @Builder
  public static class Node {
    String nodeId;
    String dependentNodeIdentifier;
  }
}
