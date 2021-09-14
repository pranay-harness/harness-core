/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.AppDynamicsDSConfig;
import io.harness.cvng.core.beans.AppDynamicsDSConfig.AppdynamicsAppConfig;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppDynamicsCVConfigTransformer implements CVConfigTransformer<AppDynamicsCVConfig, AppDynamicsDSConfig> {
  @Override
  public AppDynamicsDSConfig transformToDSConfig(List<AppDynamicsCVConfig> cvConfigs) {
    Map<String, List<AppDynamicsCVConfig>> groupByApp =
        cvConfigs.stream().collect(Collectors.groupingBy(AppDynamicsCVConfig::getApplicationName, Collectors.toList()));
    AppDynamicsDSConfig appDynamicsConfig = new AppDynamicsDSConfig();
    appDynamicsConfig.populateCommonFields(cvConfigs.get(0));
    appDynamicsConfig.setAppConfigs(Lists.newArrayList());
    groupByApp.forEach((appName, appDynamicsCVConfigs) -> {
      appDynamicsConfig.getAppConfigs().add(
          AppdynamicsAppConfig.builder()
              .applicationName(appName)
              .envIdentifier(appDynamicsCVConfigs.get(0).getEnvIdentifier())
              .metricPacks(new HashSet<>(
                  appDynamicsCVConfigs.stream().map(cv -> cv.getMetricPack()).collect(Collectors.toList())))
              .serviceMappings(new HashSet<>(appDynamicsCVConfigs.stream()
                                                 .map(cv
                                                     -> AppDynamicsDSConfig.ServiceMapping.builder()
                                                            .serviceIdentifier(cv.getServiceIdentifier())
                                                            .tierName(cv.getTierName())
                                                            .build())
                                                 .collect(Collectors.toList())))
              .build());
    });

    return appDynamicsConfig;
  }
}
