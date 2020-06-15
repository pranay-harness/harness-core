package io.harness.cvng.core.services.impl;

import com.google.inject.Singleton;

import io.harness.cvng.beans.AppDynamicsDSConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.entities.AppDynamicsCVConfig;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class AppDynamicsCVConfigTransformer implements CVConfigTransformer<AppDynamicsCVConfig, AppDynamicsDSConfig> {
  @Override
  public AppDynamicsDSConfig transformToDSConfig(List<AppDynamicsCVConfig> cvConfigGroup) {
    AppDynamicsCVConfig cvConfig = cvConfigGroup.get(0);
    AppDynamicsDSConfig appDynamicsConfig = new AppDynamicsDSConfig();
    appDynamicsConfig.setApplicationName(cvConfig.getApplicationName());
    appDynamicsConfig.populateCommonFields(cvConfig);
    appDynamicsConfig.setMetricPacks(
        new HashSet<>(cvConfigGroup.stream().map(cv -> cv.getMetricPack()).collect(Collectors.toList())));
    appDynamicsConfig.setServiceMappings(new HashSet<>(cvConfigGroup.stream()
                                                           .map(cv
                                                               -> AppDynamicsDSConfig.ServiceMapping.builder()
                                                                      .serviceIdentifier(cv.getServiceIdentifier())
                                                                      .tierName(cv.getTierName())
                                                                      .build())
                                                           .collect(Collectors.toList())));

    return appDynamicsConfig;
  }
}
