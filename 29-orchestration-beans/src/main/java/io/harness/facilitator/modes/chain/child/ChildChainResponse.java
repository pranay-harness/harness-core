package io.harness.facilitator.modes.chain.child;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.state.io.StepTransput;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder(buildMethodName = "internalBuild")
public class ChildChainResponse implements ExecutableResponse {
  String nextChildId;
  String previousChildId;
  PassThroughData passThroughData;
  boolean lastLink;
  @Singular List<StepTransput> additionalInputs;
  boolean suspend;

  public static class ChildChainResponseBuilder {
    public ChildChainResponse build() {
      if (nextChildId == null && !suspend) {
        throw new InvalidRequestException("If not Suspended nextChildId cant be null");
      }
      return internalBuild();
    }
  }
}
