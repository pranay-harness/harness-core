package io.harness.facilitator;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
public class DefaultFacilitatorParams implements FacilitatorParameters {
  @Builder.Default Duration waitDurationSeconds = Duration.ofSeconds(0);
}
