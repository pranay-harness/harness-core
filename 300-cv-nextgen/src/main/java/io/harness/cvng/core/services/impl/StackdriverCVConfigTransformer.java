/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.RiskProfile;
import io.harness.cvng.core.beans.StackdriverDSConfig;
import io.harness.cvng.core.beans.StackdriverDSConfig.StackdriverConfiguration;
import io.harness.cvng.core.beans.StackdriverDefinition;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.serializer.JsonUtils;

import java.util.ArrayList;
import java.util.List;

public class StackdriverCVConfigTransformer implements CVConfigTransformer<StackdriverCVConfig, StackdriverDSConfig> {
  @Override
  public StackdriverDSConfig transformToDSConfig(List<StackdriverCVConfig> cvConfigGroup) {
    StackdriverDSConfig dsConfig = new StackdriverDSConfig();
    List<StackdriverConfiguration> configurationList = new ArrayList<>();
    dsConfig.populateCommonFields(cvConfigGroup.get(0));
    if (cvConfigGroup != null) {
      cvConfigGroup.forEach(cvConfig -> {
        cvConfig.getMetricInfoList().forEach(metricInfo -> {
          StackdriverConfiguration configuration =
              StackdriverConfiguration.builder()
                  .envIdentifier(cvConfig.getEnvIdentifier())
                  .serviceIdentifier(cvConfig.getServiceIdentifier())
                  .metricDefinition(
                      StackdriverDefinition.builder()
                          .metricTags(metricInfo.getTags())
                          .dashboardName(cvConfig.getDashboardName())
                          .metricName(metricInfo.getMetricName())
                          .jsonMetricDefinition(JsonUtils.asObject(metricInfo.getJsonMetricDefinition(), Object.class))
                          .isManualQuery(metricInfo.isManualQuery())
                          .dashboardPath(cvConfig.getDashboardPath())
                          .riskProfile(RiskProfile.builder()
                                           .category(cvConfig.getMetricPack().getCategory())
                                           .metricType(metricInfo.getMetricType())
                                           .thresholdTypes(
                                               cvConfig.getThresholdTypeOfMetric(metricInfo.getMetricName(), cvConfig))
                                           .build())
                          .build())
                  .build();
          configurationList.add(configuration);
        });
      });
      dsConfig.setMetricConfigurations(configurationList);
    }
    return dsConfig;
  }
}
