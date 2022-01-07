/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
