package io.harness.delegate.beans.executioncapability;

import java.time.Duration;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class SystemEnvCheckerCapability implements ExecutionCapability {
  @NotNull private String comparate;
  @NotNull private String systemPropertyName;

  @Default private final CapabilityType capabilityType = CapabilityType.SYSTEM_ENV;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public CapabilityType getCapabilityType() {
    return capabilityType;
  }

  @Override
  public String fetchCapabilityBasis() {
    return systemPropertyName + ":" + comparate;
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
