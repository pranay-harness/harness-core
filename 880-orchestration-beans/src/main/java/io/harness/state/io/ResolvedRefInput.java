package io.harness.state.io;

import io.harness.pms.refobjects.RefObject;
import io.harness.pms.sdk.core.data.StepTransput;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ResolvedRefInput {
  StepTransput transput;
  @NonNull RefObject refObject;
}
