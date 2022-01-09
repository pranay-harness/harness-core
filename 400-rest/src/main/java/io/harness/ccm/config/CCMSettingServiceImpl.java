/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;

import static java.util.Objects.isNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class CCMSettingServiceImpl implements CCMSettingService {
  private AccountService accountService;
  private SettingsService settingsService;
  private WingsPersistence wingsPersistence;

  @Inject
  public CCMSettingServiceImpl(
      AccountService accountService, SettingsService settingsService, WingsPersistence wingsPersistence) {
    this.accountService = accountService;
    this.settingsService = settingsService;
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public boolean isCloudCostEnabled(String accountId) {
    Account account = accountService.get(accountId);
    return account.isCloudCostEnabled();
  }

  @Override
  public boolean isCeK8sEventCollectionEnabled(String accountId) {
    return accountService.get(accountId).isCeAutoCollectK8sEvents();
  }

  @Override
  public boolean isCloudCostEnabled(SettingAttribute settingAttribute) {
    if (isCloudCostEnabled(settingAttribute.getAccountId())) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      CCMConfig ccmConfig = value.getCcmConfig();
      if (null != ccmConfig) {
        return ccmConfig.isCloudCostEnabled();
      }
    }
    return false;
  }

  @Override
  public boolean isCeK8sEventCollectionEnabled(SettingAttribute settingAttribute) {
    if (isCeK8sEventCollectionEnabled(settingAttribute.getAccountId())) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      CCMConfig ccmConfig = value.getCcmConfig();
      if (null != ccmConfig) {
        return settingAttribute.getValue().getType().equals(SettingVariableTypes.KUBERNETES_CLUSTER.name())
            && !ccmConfig.isSkipK8sEventCollection();
      }
    } else {
      return isCloudCostEnabled(settingAttribute);
    }
    return false;
  }

  @Override
  public boolean isCloudCostEnabled(ClusterRecord clusterRecord) {
    String cloudProviderId = clusterRecord.getCluster().getCloudProviderId();
    SettingAttribute settingAttribute = settingsService.get(cloudProviderId);
    if (isNull(settingAttribute)) {
      log.error("Failed to find the Cloud Provider associated with the Cluster with id={}", clusterRecord.getUuid());
      return false;
    }
    if (settingAttribute.getValue() instanceof CloudCostAware) {
      return isCloudCostEnabled(settingAttribute);
    }
    return false;
  }

  @Override
  public boolean isCeK8sEventCollectionEnabled(ClusterRecord clusterRecord) {
    String cloudProviderId = clusterRecord.getCluster().getCloudProviderId();
    SettingAttribute settingAttribute = settingsService.get(cloudProviderId);
    if (isNull(settingAttribute)) {
      log.error("Failed to find the Cloud Provider associated with the Cluster with id={}", clusterRecord.getUuid());
      return false;
    }

    if (settingAttribute.getValue() instanceof CloudCostAware) {
      return isCeK8sEventCollectionEnabled(settingAttribute);
    }
    return false;
  }

  @Override
  public void maskCCMConfig(SettingAttribute settingAttribute) {
    Account account = accountService.get(settingAttribute.getAccountId());
    if (!account.isCloudCostEnabled()) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      value.setCcmConfig(null);
      settingAttribute.setValue((SettingValue) value);
    }
  }

  @Override
  public List<SettingAttribute> listCeCloudAccounts(String accountId) {
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    try (HIterator<SettingAttribute> iterator = new HIterator(wingsPersistence.createQuery(SettingAttribute.class)
                                                                  .filter(SettingAttributeKeys.accountId, accountId)
                                                                  .filter(SettingAttributeKeys.category, CE_CONNECTOR)
                                                                  .fetch())) {
      while (iterator.hasNext()) {
        settingAttributes.add(iterator.next());
      }
    }
    return settingAttributes;
  }
}
