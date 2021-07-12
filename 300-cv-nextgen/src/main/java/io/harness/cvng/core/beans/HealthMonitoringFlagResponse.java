package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HealthMonitoringFlagResponse {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
  boolean healthMonitoringEnabled;
}
