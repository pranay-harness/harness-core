package io.harness.facilitator.modes.chain.child;

import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.state.io.StepTransput;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ChildChainResponse implements ExecutableResponse {
  @NonNull String childNodeId;
  PassThroughData passThroughData;
  boolean chainEnd;
  @Singular List<StepTransput> additionalInputs;
}
