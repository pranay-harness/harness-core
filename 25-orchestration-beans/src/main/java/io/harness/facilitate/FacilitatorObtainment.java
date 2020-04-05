package io.harness.facilitate;

import io.harness.annotations.Redesign;
import io.harness.facilitate.io.FacilitatorParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class FacilitatorObtainment {
  FacilitatorType type;
  FacilitatorParameters facilitatorParameters;
}
