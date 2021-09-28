package io.harness.enforcement.client.services.impl;

import io.harness.enforcement.client.RestrictionUsageRegisterConfiguration;
import io.harness.enforcement.client.services.EnforcementSdkRegisterService;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.enforcement.constants.FeatureRestrictionName;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class EnforcementSdkRegisterServiceImpl implements EnforcementSdkRegisterService {
  private final Injector injector;
  private Map<FeatureRestrictionName, RestrictionUsageInterface> featureUsageMap;

  @Inject
  public EnforcementSdkRegisterServiceImpl(Injector injector) {
    this.featureUsageMap = new HashMap<>();
    this.injector = injector;
  }

  @Override
  public void initialize(RestrictionUsageRegisterConfiguration restrictionUsageRegisterConfiguration) {
    Map<FeatureRestrictionName, Class<? extends RestrictionUsageInterface>> restrictionNameClassMap =
        restrictionUsageRegisterConfiguration.getRestrictionNameClassMap();
    for (Map.Entry<FeatureRestrictionName, Class<? extends RestrictionUsageInterface>> entry :
        restrictionNameClassMap.entrySet()) {
      RestrictionUsageInterface instance = injector.getInstance(entry.getValue());
      featureUsageMap.put(entry.getKey(), instance);
    }
  }

  @Override
  public RestrictionUsageInterface get(FeatureRestrictionName featureRestrictionName) {
    return featureUsageMap.get(featureRestrictionName);
  }
}
