package io.harness.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class DefaultFacilitatorParams {
  @Builder.Default Duration waitDurationSeconds = Duration.ofSeconds(0);
}
