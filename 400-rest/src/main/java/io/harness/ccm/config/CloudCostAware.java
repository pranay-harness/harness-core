package io.harness.ccm.config;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._490_CE_COMMONS)
public interface CloudCostAware {
  void setCcmConfig(CCMConfig ccmConfig);
  CCMConfig getCcmConfig();
  default boolean cloudCostEnabled() {
    return getCcmConfig() != null && getCcmConfig().isCloudCostEnabled();
  }
}
