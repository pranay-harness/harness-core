package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DatadogMetricsDataCollectionInfo;
import io.harness.cvng.core.entities.DatadogMetricCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

import java.util.ArrayList;
import java.util.List;

public class DatadogMetricDataCollectionInfoMapper
        implements DataCollectionInfoMapper<DatadogMetricsDataCollectionInfo, DatadogMetricCVConfig> {
    @Override
    public DatadogMetricsDataCollectionInfo toDataCollectionInfo(DatadogMetricCVConfig cvConfig) {
        List<DatadogMetricsDataCollectionInfo.MetricCollectionInfo> metricDefinitions = new ArrayList<>();
        cvConfig.getMetricInfoList().forEach(metricInfo -> metricDefinitions.add(
                DatadogMetricsDataCollectionInfo.MetricCollectionInfo.builder()
                        .metricName(metricInfo.getMetricName())
                        .metric(metricInfo.getMetric())
                        .query(metricInfo.getQuery())
                        .groupingQuery(metricInfo.getGroupingQuery())
                        .serviceInstanceIdentifierTag(metricInfo.getServiceInstanceIdentifierTag())
                        .build()));
        DatadogMetricsDataCollectionInfo dataCollectionInfo =
                DatadogMetricsDataCollectionInfo.builder()
                        .metricDefinitions(metricDefinitions)
                        .groupName(cvConfig.getDashboardName())
                        .build();
        dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
        return dataCollectionInfo;
    }
}
