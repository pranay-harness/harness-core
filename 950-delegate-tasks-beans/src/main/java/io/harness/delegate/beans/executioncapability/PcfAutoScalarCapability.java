package io.harness.delegate.beans.executioncapability;

import io.harness.pcf.model.CfCliVersion;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PcfAutoScalarCapability implements ExecutionCapability {
  CfCliVersion version;
  private final CapabilityType capabilityType = CapabilityType.PCF_AUTO_SCALAR;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return "cf_appautoscalar";
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
