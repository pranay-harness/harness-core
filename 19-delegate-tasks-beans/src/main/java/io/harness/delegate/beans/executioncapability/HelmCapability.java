package io.harness.delegate.beans.executioncapability;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class HelmCapability implements ExecutionCapability {
  private String helmCommand;
  @Default private final CapabilityType capabilityType = CapabilityType.HELM;

  @Override
  public CapabilityType getCapabilityType() {
    return capabilityType;
  }

  @Override
  public String fetchCapabilityBasis() {
    return helmCommand;
  }
}
