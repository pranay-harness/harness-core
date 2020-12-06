package io.harness.ccm;

import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.GenericEntityFilter.FilterType.ALL;

import io.harness.ccm.config.CCMConfig;
import io.harness.event.handler.impl.segment.SegmentHelper;

import software.wings.beans.Delegate;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.UsageRestrictions;
import software.wings.service.impl.DelegateObserver;
import software.wings.service.intfc.SettingsService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.segment.analytics.messages.TrackMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubernetesClusterHandler implements DelegateObserver {
  @Inject SettingsService settingsService;
  @Inject SegmentHelper segmentHelper;

  @Override
  public void onAdded(Delegate delegate) {
    createKubernetes(delegate);
    segmentHelper.enqueue(TrackMessage.builder("Delegate Connected")
                              .properties(ImmutableMap.<String, Object>builder()
                                              .put("accountId", delegate.getAccountId())
                                              .put("delegateId", delegate.getUuid())
                                              .put("delegateName", delegate.getDelegateName())
                                              .put("product", "Continuous Efficiency")
                                              .build())
                              .anonymousId(delegate.getAccountId()));
  }

  @Override
  public void onDisconnected(String accountId, String delegateId) {
    // do nothing
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
    log.info("Created a Kubernetes cloud provider based on the in-cluster Delegate {}", delegate.getDelegateName());
  }
}
