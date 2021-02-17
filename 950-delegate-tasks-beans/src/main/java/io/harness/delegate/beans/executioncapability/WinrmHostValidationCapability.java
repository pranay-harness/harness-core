package io.harness.delegate.beans.executioncapability;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WinrmHostValidationCapability implements ExecutionCapability {
  String hostname;
  int port;
  boolean useSSL;
  String domain;

  @Builder.Default private final CapabilityType capabilityType = CapabilityType.WINRM_HOST_CONNECTION;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return hostname;
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
