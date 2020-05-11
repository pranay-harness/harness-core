package io.harness.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class FacilitatorObtainment {
  @NonNull FacilitatorType type;
  @Builder.Default FacilitatorParameters parameters = DefaultFacilitatorParams.builder().build();
}
