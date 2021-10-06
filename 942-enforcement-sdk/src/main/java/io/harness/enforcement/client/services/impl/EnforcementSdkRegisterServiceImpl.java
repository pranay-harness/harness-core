package io.harness.enforcement.client.services.impl;

import io.harness.enforcement.client.CustomRestrictionRegisterConfiguration;
import io.harness.enforcement.client.RestrictionUsageRegisterConfiguration;
import io.harness.enforcement.client.custom.CustomRestrictionInterface;
import io.harness.enforcement.client.services.EnforcementSdkRegisterService;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.enforcement.constants.FeatureRestrictionName;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Singleton
public class EnforcementSdkRegisterServiceImpl implements EnforcementSdkRegisterService {
  private final Injector injector;
  private Map<FeatureRestrictionName, RestrictionUsageInterface> featureUsageMap;
  private Map<FeatureRestrictionName, CustomRestrictionInterface> customFeatureMap;

  @Inject
  public EnforcementSdkRegisterServiceImpl(Injector injector) {
    this.featureUsageMap = new HashMap<>();
    this.customFeatureMap = new HashMap<>();
    this.injector = injector;
  }

  @Override
  public void initialize(@NotNull RestrictionUsageRegisterConfiguration restrictionUsageRegisterConfig,
      @NotNull CustomRestrictionRegisterConfiguration customRestrictionRegisterConfig) {
    Map<FeatureRestrictionName, Class<? extends RestrictionUsageInterface>> restrictionNameClassMap =
        restrictionUsageRegisterConfig.getRestrictionNameClassMap();
    if (restrictionNameClassMap != null) {
      for (Map.Entry<FeatureRestrictionName, Class<? extends RestrictionUsageInterface>> entry :
          restrictionNameClassMap.entrySet()) {
        RestrictionUsageInterface instance = injector.getInstance(entry.getValue());
        featureUsageMap.put(entry.getKey(), instance);
      }
    }

    Map<FeatureRestrictionName, Class<? extends CustomRestrictionInterface>> customRestrictionMap =
        customRestrictionRegisterConfig.getCustomRestrictionMap();
    if (customRestrictionMap != null) {
      for (Map.Entry<FeatureRestrictionName, Class<? extends CustomRestrictionInterface>> entry :
          customRestrictionMap.entrySet()) {
        CustomRestrictionInterface instance = injector.getInstance(entry.getValue());
        customFeatureMap.put(entry.getKey(), instance);
      }
    }
  }

  @Override
  public RestrictionUsageInterface getRestrictionUsageInterface(FeatureRestrictionName featureRestrictionName) {
    return featureUsageMap.get(featureRestrictionName);
  }

  @Override
  public CustomRestrictionInterface getCustomRestrictionInterface(FeatureRestrictionName featureRestrictionName) {
    return customFeatureMap.get(featureRestrictionName);
  }
}
