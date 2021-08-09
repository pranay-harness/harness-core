package io.harness.pms.sdk.core.steps.io;

import io.harness.pms.contracts.refobjects.RefObject;
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
