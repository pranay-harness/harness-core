package io.harness.facilitator;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@Redesign
public class FacilitatorObtainment {
  @NonNull FacilitatorType type;
  @Builder.Default FacilitatorParameters parameters = DefaultFacilitatorParams.builder().build();
}
