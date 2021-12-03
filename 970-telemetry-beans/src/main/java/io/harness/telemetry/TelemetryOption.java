package io.harness.telemetry;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TelemetryOption {
  private boolean sendForCommunity;
}
