/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.utils.monitoredService;

import io.harness.cvng.core.beans.CustomHealthMetricDefinition;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.CustomHealthSourceSpec;
import io.harness.cvng.core.entities.CustomHealthCVConfig;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class CustomHealthSourceSpecTransformer
    implements CVConfigToHealthSourceTransformer<CustomHealthCVConfig, CustomHealthSourceSpec> {
  @Override
  public CustomHealthSourceSpec transformToHealthSourceConfig(List<CustomHealthCVConfig> cvConfigGroup) {
    Preconditions.checkNotNull(cvConfigGroup);
    CustomHealthSourceSpec customHealthSourceSpec = CustomHealthSourceSpec.builder()
                                                        .connectorRef(cvConfigGroup.get(0).getConnectorIdentifier())
                                                        .metricDefinitions(new ArrayList<>())
                                                        .build();

    cvConfigGroup.forEach(customHealthCVConfig -> {
      customHealthCVConfig.getMetricDefinitions().forEach(definition -> {
        CustomHealthMetricDefinition customHealthMetricDefinition =
            CustomHealthMetricDefinition.builder()
                .method(definition.getMethod())
                .urlPath(definition.getUrlPath())
                .queryType(definition.getQueryType())
                .groupName(customHealthCVConfig.getGroupName())
                .metricName(definition.getMetricName())
                .metricResponseMapping(definition.getMetricResponseMapping())
                .metricName(definition.getMetricName())
                .analysis(definition.getAnalysis())
                .endTime(definition.getEndTime())
                .startTime(definition.getStartTime())
                .identifier(definition.getIdentifier())
                .riskProfile(definition.getRiskProfile())
                .requestBody(definition.getRequestBody())
                .build();
        customHealthSourceSpec.getMetricDefinitions().add(customHealthMetricDefinition);
      });
    });
    return customHealthSourceSpec;
  }
}
