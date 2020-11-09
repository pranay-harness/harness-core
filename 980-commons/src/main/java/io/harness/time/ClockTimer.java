package io.harness.time;

import java.time.Instant;

public class ClockTimer implements Timer {
  @Override
  public Instant now() {
    return Instant.now();
  }
}