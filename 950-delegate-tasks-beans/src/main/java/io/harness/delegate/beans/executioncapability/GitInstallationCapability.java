package io.harness.delegate.beans.executioncapability;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitInstallationCapability implements ExecutionCapability {
  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  CapabilityType capabilityType = CapabilityType.GIT_INSTALLATION;

  @Override
  public String fetchCapabilityBasis() {
    return capabilityType.name();
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
