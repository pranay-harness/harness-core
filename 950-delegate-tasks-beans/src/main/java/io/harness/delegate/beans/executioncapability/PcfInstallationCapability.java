package io.harness.delegate.beans.executioncapability;

import io.harness.pcf.model.PcfCliVersion;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PcfInstallationCapability implements ExecutionCapability {
  PcfCliVersion version;
  String criteria;
  CapabilityType capabilityType = CapabilityType.PCF_INSTALL;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public CapabilityType getCapabilityType() {
    return CapabilityType.PCF_INSTALL;
  }

  @Override
  public String fetchCapabilityBasis() {
    return criteria;
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
