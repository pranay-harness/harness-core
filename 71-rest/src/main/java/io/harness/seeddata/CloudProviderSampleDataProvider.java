package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.K8S_CLOUD_PROVIDER_NAME;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.UsageRestrictionsUtil.getAllAppAllEnvUsageRestrictions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

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
                           .delegateName(SampleDataProviderConstants
                                             .K8S_DELEGATE_NAME) // Cluster name should match with the delegate name
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();

    SettingAttribute existing = settingsService.fetchSettingAttributeByName(
        accountId, K8S_CLOUD_PROVIDER_NAME, SettingVariableTypes.KUBERNETES_CLUSTER);
    if (existing != null) {
      return existing;
    }
    return settingsService.forceSave(kubeCluster);
  }
}
