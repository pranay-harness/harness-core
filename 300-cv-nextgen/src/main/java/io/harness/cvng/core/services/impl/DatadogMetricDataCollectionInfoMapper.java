package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DatadogMetricsDataCollectionInfo;
import io.harness.cvng.core.entities.DatadogMetricCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class DatadogMetricDataCollectionInfoMapper
    implements DataCollectionInfoMapper<DatadogMetricsDataCollectionInfo, DatadogMetricCVConfig> {
  @Override
  public DatadogMetricsDataCollectionInfo toDataCollectionInfo(DatadogMetricCVConfig cvConfig) {
    List<DatadogMetricsDataCollectionInfo.MetricCollectionInfo> metricDefinitions = new ArrayList<>();
    cvConfig.getMetricInfoList().forEach(metricInfo
        -> metricDefinitions.add(DatadogMetricsDataCollectionInfo.MetricCollectionInfo.builder()
                                     .metricName(metricInfo.getMetricName())
                                     .metric(metricInfo.getMetric())
                                     .query(metricInfo.getQuery())
                                     .groupingQuery(metricInfo.getGroupingQuery())
                                     .serviceInstanceIdentifierTag(metricInfo.getServiceInstanceIdentifierTag())
                                     .build()));
    DatadogMetricsDataCollectionInfo dataCollectionInfo = DatadogMetricsDataCollectionInfo.builder()
                                                              .metricDefinitions(metricDefinitions)
                                                              .groupName(cvConfig.getDashboardName())
                                                              .build();
    dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }

  @Override
  public DatadogMetricsDataCollectionInfo toDataCollectionInfoForSLI(
      List<DatadogMetricCVConfig> cvConfigList, ServiceLevelIndicator serviceLevelIndicator) {
    List<String> sliMetricNames = serviceLevelIndicator.getMetricNames();
    Preconditions.checkNotNull(cvConfigList);
    DatadogMetricCVConfig baseCvConfig = cvConfigList.get(0);
    List<DatadogMetricsDataCollectionInfo.MetricCollectionInfo> metricDefinitions = new ArrayList<>();
    cvConfigList.forEach(cvConfig -> cvConfig.getMetricInfoList().forEach(metricInfo -> {
      if (sliMetricNames.contains(metricInfo.getMetricName())) {
        metricDefinitions.add(DatadogMetricsDataCollectionInfo.MetricCollectionInfo.builder()
                                  .metricName(metricInfo.getMetricName())
                                  .metric(metricInfo.getMetric())
                                  .query(metricInfo.getQuery())
                                  .groupingQuery(metricInfo.getGroupingQuery())
                                  .serviceInstanceIdentifierTag(metricInfo.getServiceInstanceIdentifierTag())
                                  .build());
      }
    }));
    DatadogMetricsDataCollectionInfo dataCollectionInfo = DatadogMetricsDataCollectionInfo.builder()
                                                              .metricDefinitions(metricDefinitions)
                                                              .groupName(baseCvConfig.getDashboardName())
                                                              .build();
    dataCollectionInfo.setDataCollectionDsl(baseCvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }
}
