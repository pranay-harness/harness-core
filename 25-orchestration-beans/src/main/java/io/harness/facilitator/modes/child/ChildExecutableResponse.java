package io.harness.facilitator.modes.child;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.state.io.StepTransput;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class ChildExecutableResponse implements ExecutableResponse {
  String childNodeId;
  @Singular List<StepTransput> additionalInputs;
}
