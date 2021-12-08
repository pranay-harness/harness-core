package io.harness.telemetry;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class NGTelemetryPublisher {
  public void recordTelemetry() {
    Long MILLISECONDS_IN_A_DAY = 86400000L;
    Long activeServicesCount = 0L;
    Long deloymentCount = 0L;
  }
}