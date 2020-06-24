package io.harness.state.io;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StepInputPackage {
  @Singular List<ResolvedRefInput> inputs;
  @Singular List<StepTransput> additionalInputs;
}
