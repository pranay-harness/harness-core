package io.harness.state.io;

import io.harness.references.RefObject;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ResolvedRefInput {
  StepTransput transput;
  @NonNull RefObject refObject;
}
