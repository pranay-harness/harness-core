package io.harness.delegate.beans.executioncapability;

import java.time.Duration;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class AlwaysFalseValidationCapability implements ExecutionCapability {
  @Default private final CapabilityType capabilityType = CapabilityType.ALWAYS_FALSE;

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
    return "ALWAYS_FALSE";
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return null;
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return null;
  }
}
