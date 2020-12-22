package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.core.beans.StackdriverDSConfig;
import io.harness.cvng.core.beans.StackdriverDefinition;
import io.harness.cvng.core.entities.MetricPack.MetricDefinition;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StackdriverCVConfigTransformer implements CVConfigTransformer<StackdriverCVConfig, StackdriverDSConfig> {
  @Override
  public StackdriverDSConfig transformToDSConfig(List<StackdriverCVConfig> cvConfigGroup) {
    StackdriverCVConfig cvConfig = cvConfigGroup.get(0);
    StackdriverDSConfig dsConfig = new StackdriverDSConfig();
    dsConfig.setServiceIdentifier(cvConfig.getServiceIdentifier());

    dsConfig.populateCommonFields(cvConfig);
    dsConfig.setMetricPacks(
        new HashSet<>(cvConfigGroup.stream().map(cv -> cv.getMetricPack()).collect(Collectors.toList())));

    List<StackdriverDefinition> metricDefinitions = new ArrayList<>();
    cvConfigGroup.forEach(cvConfig1 -> cvConfig.getMetricInfoList().forEach(metricInfo -> {
      metricDefinitions.add(
          StackdriverDefinition.builder()
              .metricName(metricInfo.getMetricName())
              .metricTags(metricInfo.getTags())
              .dashboardName(cvConfig.getDashboardName())
              .isManualQuery(metricInfo.isManualQuery())
              .jsonMetricDefinition(metricInfo.getJsonMetricDefinition())
              .riskProfile(StackdriverDefinition.RiskProfile.builder()
                               .category(cvConfig.getMetricPack().getCategory())
                               .metricType(metricInfo.getMetricType())
                               .thresholdTypes(getThresholdTypeOfMetric(metricInfo.getMetricName(), cvConfig1))
                               .build())
              .build());
    }));
    dsConfig.setMetricDefinitions(Sets.newHashSet(metricDefinitions));

    return dsConfig;
  }

  private List<TimeSeriesThresholdType> getThresholdTypeOfMetric(String metricName, StackdriverCVConfig cvConfig) {
    Set<TimeSeriesThresholdType> thresholdTypes = new HashSet<>();
    for (MetricDefinition metricDefinition : cvConfig.getMetricPack().getMetrics()) {
      if (metricDefinition.getName().equals(metricName)) {
        metricDefinition.getThresholds().forEach(
            threshold -> thresholdTypes.add(threshold.getCriteria().getThresholdType()));
      }
    }
    return new ArrayList<>(thresholdTypes);
  }
}
