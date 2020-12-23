package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.core.beans.StackdriverDSConfig;
import io.harness.cvng.core.beans.StackdriverDSConfig.StackdriverConfiguration;
import io.harness.cvng.core.beans.StackdriverDefinition;
import io.harness.cvng.core.entities.MetricPack.MetricDefinition;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.serializer.JsonUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                          .riskProfile(
                              StackdriverDefinition.RiskProfile.builder()
                                  .category(cvConfig.getMetricPack().getCategory())
                                  .metricType(metricInfo.getMetricType())
                                  .thresholdTypes(getThresholdTypeOfMetric(metricInfo.getMetricName(), cvConfig))
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
