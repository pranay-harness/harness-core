/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.utils.monitoredService;

import io.harness.cvng.core.beans.RiskProfile;
import io.harness.cvng.core.beans.StackdriverDefinition;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.StackdriverMetricHealthSourceSpec;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.serializer.JsonUtils;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class StackdriverMetricHealthSourceSpecTransformer
    implements CVConfigToHealthSourceTransformer<StackdriverCVConfig, StackdriverMetricHealthSourceSpec> {
  @Override
  public StackdriverMetricHealthSourceSpec transformToHealthSourceConfig(List<StackdriverCVConfig> cvConfigs) {
    Preconditions.checkNotNull(cvConfigs);
    Preconditions.checkArgument(
        cvConfigs.stream().map(StackdriverCVConfig::getConnectorIdentifier).distinct().count() == 1,
        "ConnectorRef should be same for all the configs in the list.");
    List<StackdriverDefinition> metricDefinitions = new ArrayList<>();
    cvConfigs.forEach(cvConfig -> {
      cvConfig.getMetricInfoList().forEach(metricInfo -> {
        StackdriverDefinition metricDefinition =
            StackdriverDefinition.builder()
                .metricTags(metricInfo.getTags())
                .dashboardName(cvConfig.getDashboardName())
                .metricName(metricInfo.getMetricName())
                .serviceInstanceField(metricInfo.getServiceInstanceField())
                .jsonMetricDefinition(JsonUtils.asObject(metricInfo.getJsonMetricDefinition(), Object.class))
                .isManualQuery(metricInfo.isManualQuery())
                .dashboardPath(cvConfig.getDashboardPath())

                .riskProfile(
                    RiskProfile.builder()
                        .category(cvConfig.getMetricPack().getCategory())
                        .metricType(metricInfo.getMetricType())
                        .thresholdTypes(cvConfig.getThresholdTypeOfMetric(metricInfo.getMetricName(), cvConfig))
                        .build())
                .build();

        metricDefinitions.add(metricDefinition);
      });
    });
    return StackdriverMetricHealthSourceSpec.builder()
        .metricDefinitions(metricDefinitions)
        .connectorRef(cvConfigs.get(0).getConnectorIdentifier())
        .build();
  }
}
