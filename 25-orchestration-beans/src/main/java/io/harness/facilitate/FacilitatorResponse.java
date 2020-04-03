package io.harness.facilitate;

import io.harness.annotations.Redesign;
import io.harness.facilitate.modes.ExecutionMode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class FacilitatorResponse {
  ExecutionMode executionMode;
  PassThroughData passThroughData;
}
