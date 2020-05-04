package io.harness.facilitator;

import io.harness.annotations.Redesign;

import java.time.Duration;

@Redesign
public interface FacilitatorParameters {
  Duration getWaitDurationSeconds();
}
