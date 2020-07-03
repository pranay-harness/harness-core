package io.harness.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@OwnedBy(CDC)
@Value
@Builder
public class DefaultFacilitatorParams implements FacilitatorParameters {
  @Builder.Default Duration waitDurationSeconds = Duration.ofSeconds(0);
}
