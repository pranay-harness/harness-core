package io.harness.facilitate.io;

import io.harness.annotations.Redesign;

import java.time.Duration;

@Redesign
public interface FacilitatorParameters {
  Duration getWaitDurationSeconds();
}
