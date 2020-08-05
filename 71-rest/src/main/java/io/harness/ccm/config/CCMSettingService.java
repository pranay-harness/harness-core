package io.harness.ccm.config;

import io.harness.ccm.cluster.entities.ClusterRecord;
import software.wings.beans.SettingAttribute;

import java.util.List;

public interface CCMSettingService {
  boolean isCloudCostEnabled(String accountId);
  boolean isCloudCostEnabled(SettingAttribute settingAttribute);
  void maskCCMConfig(SettingAttribute settingAttribute);
  boolean isCloudCostEnabled(ClusterRecord clusterRecord);
  boolean isCeK8sEventCollectionEnabled(String accountId);
  boolean isCeK8sEventCollectionEnabled(SettingAttribute settingAttribute);
  boolean isCeK8sEventCollectionEnabled(ClusterRecord clusterRecord);
  List<SettingAttribute> listCeCloudAccounts(String accountId);
}
