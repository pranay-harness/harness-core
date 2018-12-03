package io.harness.seeddata;

import static io.harness.seeddata.SeedDataProviderConstants.KUBE_CLUSTER_NAME;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.UsageRestrictionsUtil.getAllAppAllEnvUsageRestrictions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.service.intfc.SettingsService;

@Singleton
public class CloudProviderSeedDataProvider {
  @Inject private SettingsService settingsService;

  public SettingAttribute createKubernetesClusterConfig(String accountId) {
    SettingAttribute kubeCluster =
        aSettingAttribute()
            .withCategory(Category.CLOUD_PROVIDER)
            .withName(KUBE_CLUSTER_NAME)
            .withAccountId(accountId)
            .withValue(KubernetesClusterConfig.builder()
                           .accountId(accountId)
                           .useKubernetesDelegate(true)
                           .skipValidation(true)
                           .delegateName(KUBE_CLUSTER_NAME) // Cluster name should match with the delegate name
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return settingsService.forceSave(kubeCluster);
  }
}
