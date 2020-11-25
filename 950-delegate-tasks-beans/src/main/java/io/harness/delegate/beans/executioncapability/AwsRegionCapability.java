package io.harness.delegate.beans.executioncapability;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class AwsRegionCapability implements ExecutionCapability {
  private String region;
  @Default private final CapabilityType capabilityType = CapabilityType.AWS_REGION;

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
    return region;
  }
}
