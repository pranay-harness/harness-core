/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.K8S_CLOUD_PROVIDER_NAME;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;

@Singleton
public class CloudProviderSampleDataProvider {
  @Inject private SettingsService settingsService;

  public SettingAttribute createKubernetesClusterConfig(String accountId) {
    SettingAttribute kubeCluster =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(K8S_CLOUD_PROVIDER_NAME)
            .withAccountId(accountId)
            .withValue(KubernetesClusterConfig.builder()
                           .accountId(accountId)
                           .useKubernetesDelegate(true)
                           .skipValidation(true)
                           .delegateSelectors(new HashSet<>(Collections.singletonList(
                               SampleDataProviderConstants
                                   .K8S_DELEGATE_NAME))) // Cluster name should match with the delegate name
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .withSample(true)
            .build();

    SettingAttribute existing = settingsService.fetchSettingAttributeByName(
        accountId, K8S_CLOUD_PROVIDER_NAME, SettingVariableTypes.KUBERNETES_CLUSTER);
    if (existing != null) {
      return existing;
    }
    return settingsService.forceSave(kubeCluster);
  }
}
