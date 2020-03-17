package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PcfAutoScalarCapability implements ExecutionCapability {
  private final CapabilityType capabilityType = CapabilityType.PCF_AUTO_SCALAR;

  @Override
  public String fetchCapabilityBasis() {
    return "cf_appautoscalar";
  }
}
