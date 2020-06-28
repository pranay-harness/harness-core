package io.harness.facilitator.modes.chain.child;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.state.io.StepTransput;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class ChildChainResponse implements ExecutableResponse {
  @NonNull String nextChildId;
  String previousChildId;
  PassThroughData passThroughData;
  boolean lastLink;
  @Singular List<StepTransput> additionalInputs;
}
