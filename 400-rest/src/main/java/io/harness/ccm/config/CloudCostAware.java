package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CE)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public interface CloudCostAware {
  void setCcmConfig(CCMConfig ccmConfig);
  CCMConfig getCcmConfig();
  default boolean cloudCostEnabled() {
    return getCcmConfig() != null && getCcmConfig().isCloudCostEnabled();
  }
}
