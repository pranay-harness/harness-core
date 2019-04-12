package io.harness.delegate.beans.executioncapability;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class IgnoreValidationCapability implements ExecutionCapability {
  @Default private final CapabilityType capabilityType = CapabilityType.ALWAYS_TRUE;
  @Override
  public CapabilityType getCapabilityType() {
    return capabilityType;
  }

  @Override
  public String fetchCapabilityBasis() {
    return "ALWAYS_ALLOW";
  }
}
