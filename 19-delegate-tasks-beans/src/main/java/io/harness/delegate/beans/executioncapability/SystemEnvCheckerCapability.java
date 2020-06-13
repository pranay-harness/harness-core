package io.harness.delegate.beans.executioncapability;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class SystemEnvCheckerCapability implements ExecutionCapability {
  @NotNull private String comparate;
  @NotNull private String systemPropertyName;

  @Default private final CapabilityType capabilityType = CapabilityType.SYSTEM_ENV;

  @Override
  public CapabilityType getCapabilityType() {
    return capabilityType;
  }

  @Override
  public String fetchCapabilityBasis() {
    return systemPropertyName + ":" + comparate;
  }
}
