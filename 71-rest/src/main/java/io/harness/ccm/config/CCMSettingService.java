package io.harness.ccm.config;

import io.harness.ccm.cluster.entities.ClusterRecord;
import software.wings.beans.SettingAttribute;

public interface CCMSettingService {
  boolean isCloudCostEnabled(String accountId);
  boolean isCloudCostEnabled(SettingAttribute settingAttribute);
  void maskCCMConfig(SettingAttribute settingAttribute);
  boolean isCloudCostEnabled(ClusterRecord clusterRecord);
}
