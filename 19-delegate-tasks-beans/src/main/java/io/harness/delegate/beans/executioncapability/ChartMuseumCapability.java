package io.harness.delegate.beans.executioncapability;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class ChartMuseumCapability implements ExecutionCapability {
  private String chartMuseumCommand;
  @Default private final CapabilityType capabilityType = CapabilityType.CHART_MUSEUM;

  @Override
  public CapabilityType getCapabilityType() {
    return capabilityType;
  }

  @Override
  public String fetchCapabilityBasis() {
    return chartMuseumCommand;
  }
}
