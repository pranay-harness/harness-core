package io.harness.feature.client.services.impl;

import io.harness.feature.client.PlanFeatureRegisterConfiguration;
import io.harness.feature.client.services.PlanFeatureRegisterService;
import io.harness.feature.client.usage.PlanFeatureUsageInterface;
import io.harness.feature.constants.FeatureRestriction;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class PlanFeatureRegisterServiceImpl implements PlanFeatureRegisterService {
  private final Injector injector;
  private Map<FeatureRestriction, PlanFeatureUsageInterface> featureUsageMap;

  @Inject
  public PlanFeatureRegisterServiceImpl(Injector injector) {
    this.featureUsageMap = new HashMap<>();
    this.injector = injector;
  }

  @Override
  public void initialize(PlanFeatureRegisterConfiguration planFeatureClientConfiguration) {
    Map<FeatureRestriction, Class<? extends PlanFeatureUsageInterface>> usageImplRegistrars =
        planFeatureClientConfiguration.getUsageImplRegistrars();
    for (Map.Entry<FeatureRestriction, Class<? extends PlanFeatureUsageInterface>> entry :
        usageImplRegistrars.entrySet()) {
      PlanFeatureUsageInterface instance = injector.getInstance(entry.getValue());
      featureUsageMap.put(entry.getKey(), instance);
    }
  }

  @Override
  public PlanFeatureUsageInterface get(FeatureRestriction featureName) {
    return featureUsageMap.get(featureName);
  }
}
