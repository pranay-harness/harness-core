package io.harness.telemetry;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class NGTelemetryPublisher {
  public void recordTelemetry() {
    // Any method here will be called every 24 hours.
  }
}
