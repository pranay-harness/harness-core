package io.harness.delegate.beans.executioncapability;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChartMuseumCapability implements ExecutionCapability {
  CapabilityType capabilityType = CapabilityType.CHART_MUSEUM;

  @Override
  public String fetchCapabilityBasis() {
    return capabilityType.name();
  }
}
