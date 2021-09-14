/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.PrometheusDSConfig;
import io.harness.cvng.core.beans.PrometheusMetricDefinition;
import io.harness.cvng.core.beans.RiskProfile;
import io.harness.cvng.core.entities.PrometheusCVConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;

import java.util.ArrayList;
import java.util.List;

public class PrometheusCVConfigTransformer implements CVConfigTransformer<PrometheusCVConfig, PrometheusDSConfig> {
  @Override
  public PrometheusDSConfig transformToDSConfig(List<PrometheusCVConfig> cvConfigGroup) {
    PrometheusDSConfig dsConfig = new PrometheusDSConfig();
    dsConfig.populateCommonFields(cvConfigGroup.get(0));
    List<PrometheusMetricDefinition> metricDefinitions = new ArrayList<>();
    if (cvConfigGroup != null) {
      cvConfigGroup.forEach(prometheusCVConfig -> {
        prometheusCVConfig.getMetricInfoList().forEach(metricInfo -> {
          PrometheusMetricDefinition metricDefinition =
              PrometheusMetricDefinition.builder()
                  .serviceIdentifier(prometheusCVConfig.getServiceIdentifier())
                  .envIdentifier(prometheusCVConfig.getEnvIdentifier())
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
      dsConfig.setMetricDefinitions(metricDefinitions);
      return dsConfig;
    }
    return null;
  }
}
