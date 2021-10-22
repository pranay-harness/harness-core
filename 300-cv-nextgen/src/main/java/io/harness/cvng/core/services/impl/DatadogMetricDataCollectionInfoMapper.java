package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DatadogDataCollectionInfo;
import io.harness.cvng.core.entities.DatadogMetricCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

import java.util.ArrayList;
import java.util.List;

public class DatadogMetricDataCollectionInfoMapper
    implements DataCollectionInfoMapper<DatadogDataCollectionInfo, DatadogMetricCVConfig> {
  @Override
  public DatadogDataCollectionInfo toDataCollectionInfo(DatadogMetricCVConfig cvConfig) {
    List<DatadogDataCollectionInfo.MetricCollectionInfo> metricDefinitions = new ArrayList<>();
    cvConfig.getMetricInfoList().forEach(metricInfo -> metricDefinitions.add(DatadogDataCollectionInfo.MetricCollectionInfo.builder()
            .metricName(metricInfo.getMetricName())
            .query(metricInfo.getQuery())
            .build()));
    DatadogDataCollectionInfo dataCollectionInfo =
            DatadogDataCollectionInfo.builder().metricDefinitions(metricDefinitions).build();
    dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }
}
