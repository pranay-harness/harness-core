package io.harness.ccm;

public interface CloudCostAware {
  void setCcmConfig(CCMConfig ccmConfig);
  CCMConfig getCcmConfig();
  default boolean cloudCostEnabled() {
    return getCcmConfig() != null && getCcmConfig().isCloudCostEnabled();
  }
}
