package io.harness.ccm;

import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.GenericEntityFilter.FilterType.ALL;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.ccm.config.CCMConfig;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Delegate;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.service.impl.DelegateObserver;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.UsageRestrictions;

@Slf4j
public class KubernetesClusterHandler implements DelegateObserver {
  @Inject SettingsService settingsService;

  @Override
  public void onAdded(Delegate delegate) {
    createKubernetes(delegate);
  }

  private void createKubernetes(Delegate delegate) {
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder()
            .accountId(delegate.getAccountId())
            .useKubernetesDelegate(true)
            .delegateName(delegate.getDelegateName())
            .skipValidation(true)
            .ccmConfig(CCMConfig.builder().cloudCostEnabled(true).skipK8sEventCollection(false).build())
            .build();
    settingsService.save(
        SettingAttribute.Builder.aSettingAttribute()
            .withAccountId(delegate.getAccountId())
            .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
            .withValue(kubernetesClusterConfig)
            .withName(delegate.getDelegateName())
            .withUsageRestrictions(UsageRestrictions.builder()
                                       .appEnvRestrictions(ImmutableSet.of(
                                           UsageRestrictions.AppEnvRestriction.builder()
                                               .appFilter(GenericEntityFilter.builder().filterType(ALL).build())
                                               .envFilter(new EnvFilter(null, Sets.newHashSet(PROD)))
                                               .build(),
                                           UsageRestrictions.AppEnvRestriction.builder()
                                               .appFilter(GenericEntityFilter.builder().filterType(ALL).build())
                                               .envFilter(new EnvFilter(null, Sets.newHashSet(NON_PROD)))
                                               .build()))
                                       .build())
            .build());
    logger.info("Created a Kubernetes cloud provider based on the in-cluster Delegate {}", delegate.getDelegateName());
  }
}
