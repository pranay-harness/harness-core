package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Builder;
import lombok.Value;
import software.wings.helpers.ext.helm.HelmConstants;

@Value
@Builder
public class HelmInstallationCapability implements ExecutionCapability {
  HelmConstants.HelmVersion version;
  String criteria;
  CapabilityType capabilityType = CapabilityType.HELM_INSTALL;

  @Override
  public String fetchCapabilityBasis() {
    return criteria;
  }
}
