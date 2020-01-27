package io.harness.ccm;

import io.harness.ccm.cluster.entities.ClusterRecord;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ValidationResult;

public interface CCMSettingService {
  boolean isCloudCostEnabled(SettingAttribute settingAttribute);
  SettingAttribute maskCCMConfig(SettingAttribute settingAttribute);
  boolean isCloudCostEnabled(ClusterRecord clusterRecord);
  ValidationResult validateS3SyncConfig(AwsS3SyncConfig awsS3SyncConfig, String accountId, String settingId);
}
