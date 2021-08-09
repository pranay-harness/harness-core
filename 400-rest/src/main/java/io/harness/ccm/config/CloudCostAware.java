package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
@OwnedBy(CE)
public interface CloudCostAware {
  void setCcmConfig(CCMConfig ccmConfig);
  CCMConfig getCcmConfig();
  default boolean cloudCostEnabled() {
    return getCcmConfig() != null && getCcmConfig().isCloudCostEnabled();
  }
}
