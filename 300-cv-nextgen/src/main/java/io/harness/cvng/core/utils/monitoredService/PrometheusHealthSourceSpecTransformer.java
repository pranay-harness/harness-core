/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.utils.monitoredService;

import io.harness.cvng.core.beans.PrometheusMetricDefinition;
import io.harness.cvng.core.beans.RiskProfile;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.PrometheusHealthSourceSpec;
import io.harness.cvng.core.entities.PrometheusCVConfig;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class PrometheusHealthSourceSpecTransformer
    implements CVConfigToHealthSourceTransformer<PrometheusCVConfig, PrometheusHealthSourceSpec> {
  @Override
  public PrometheusHealthSourceSpec transformToHealthSourceConfig(List<PrometheusCVConfig> cvConfigs) {
    Preconditions.checkNotNull(cvConfigs);
    Preconditions.checkArgument(
        cvConfigs.stream().map(PrometheusCVConfig::getConnectorIdentifier).distinct().count() == 1,
        "ConnectorRef should be same for all the configs in the list.");
    List<PrometheusMetricDefinition> metricDefinitions = new ArrayList<>();

    cvConfigs.forEach(prometheusCVConfig -> {
      prometheusCVConfig.getMetricInfoList().forEach(metricInfo -> {
        PrometheusMetricDefinition metricDefinition =
            PrometheusMetricDefinition.builder()
                .groupName(prometheusCVConfig.getGroupName())
                .serviceFilter(metricInfo.getServiceFilter())
                .envFilter(metricInfo.getEnvFilter())
                .additionalFilters(metricInfo.getAdditionalFilters())
                .isManualQuery(metricInfo.isManualQuery())
                .query(metricInfo.getQuery())
                .metricName(metricInfo.getMetricName())
                .prometheusMetric(metricInfo.getPrometheusMetricName())
                .aggregation(metricInfo.getAggregation())
                .serviceInstanceFieldName(metricInfo.getServiceInstanceFieldName())
                .riskProfile(RiskProfile.builder()
                                 .category(prometheusCVConfig.getMetricPack().getCategory())
                                 .metricType(metricInfo.getMetricType())
                                 .thresholdTypes(prometheusCVConfig.getThresholdTypeOfMetric(
                                     metricInfo.getMetricName(), prometheusCVConfig))
                                 .build())
                .build();
        metricDefinitions.add(metricDefinition);
      });
    });

    return PrometheusHealthSourceSpec.builder()
        .connectorRef(cvConfigs.get(0).getConnectorIdentifier())
        .metricDefinitions(metricDefinitions)
        .build();
  }
}
