package io.harness.delegate.beans.executioncapability;

import static io.harness.delegate.beans.executioncapability.CapabilityType.KUSTOMIZE;

import io.harness.data.structure.HarnessStringUtils;
import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

@Data
@Builder
public class KustomizeCapability implements ExecutionCapability {
  private KustomizeConfig kustomizeConfig;
  @Override
  public CapabilityType getCapabilityType() {
    return KUSTOMIZE;
  }

  @Override
  public String fetchCapabilityBasis() {
    return HarnessStringUtils.join(":", "kustomizePluginDir", kustomizeConfig.getPluginRootDir());
  }
}
