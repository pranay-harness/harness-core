package io.harness.telemetry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * Common config definition for telemetry module
 */
@OwnedBy(HarnessTeam.GTM)
public interface TelemetryConfiguration {
  boolean isEnabled();
  String getUrl();
  String getApiKey();
  boolean isCertValidationRequired();
}
