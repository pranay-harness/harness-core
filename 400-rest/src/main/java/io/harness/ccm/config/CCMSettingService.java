/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.cluster.entities.ClusterRecord;

import software.wings.beans.SettingAttribute;

import java.util.List;

@OwnedBy(CE)
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
