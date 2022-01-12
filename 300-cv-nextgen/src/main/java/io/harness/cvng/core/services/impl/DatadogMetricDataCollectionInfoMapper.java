/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DatadogMetricsDataCollectionInfo;
import io.harness.cvng.beans.DatadogMetricsDataCollectionInfo.MetricCollectionInfo;
import io.harness.cvng.core.entities.DatadogMetricCVConfig;
import io.harness.cvng.core.entities.DatadogMetricCVConfig.MetricInfo;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionSLIInfoMapper;
import io.harness.cvng.core.utils.dataCollection.MetricDataCollectionUtils;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class DatadogMetricDataCollectionInfoMapper
    implements DataCollectionInfoMapper<DatadogMetricsDataCollectionInfo, DatadogMetricCVConfig>,
               DataCollectionSLIInfoMapper<DatadogMetricsDataCollectionInfo, DatadogMetricCVConfig> {
  @Override
  public DatadogMetricsDataCollectionInfo toDataCollectionInfo(DatadogMetricCVConfig cvConfig, TaskType taskType) {
    List<MetricCollectionInfo> metricDefinitions = new ArrayList<>();
    cvConfig.getMetricInfoList()
        .stream()
        .filter(metricInfo -> MetricDataCollectionUtils.isMetricApplicableForDataCollection(metricInfo, taskType))
        .forEach(metricInfo -> metricDefinitions.add(getMetricCollectionInfo(metricInfo)));
    return getDataCollectionInfo(metricDefinitions, cvConfig);
  }

  @Override
  public DatadogMetricsDataCollectionInfo toDataCollectionInfo(
      List<DatadogMetricCVConfig> cvConfigList, ServiceLevelIndicator serviceLevelIndicator) {
    List<String> sliMetricNames = serviceLevelIndicator.getMetricNames();
    Preconditions.checkNotNull(cvConfigList);
    DatadogMetricCVConfig baseCvConfig = cvConfigList.get(0);
    List<MetricCollectionInfo> metricDefinitions = new ArrayList<>();
    cvConfigList.forEach(cvConfig -> cvConfig.getMetricInfoList().forEach(metricInfo -> {
      if (sliMetricNames.contains(metricInfo.getMetricName())) {
        metricDefinitions.add(getMetricCollectionInfo(metricInfo));
      }
    }));

    return getDataCollectionInfo(metricDefinitions, baseCvConfig);
  }

  private MetricCollectionInfo getMetricCollectionInfo(MetricInfo metricInfo) {
    return DatadogMetricsDataCollectionInfo.MetricCollectionInfo.builder()
        .metricName(metricInfo.getMetricName())
        .metricIdentifier(metricInfo.getIdentifier())
        .metric(metricInfo.getMetric())
        .query(metricInfo.getQuery())
        .groupingQuery(metricInfo.getGroupingQuery())
        .serviceInstanceIdentifierTag(metricInfo.getServiceInstanceIdentifierTag())
        .build();
  }

  private DatadogMetricsDataCollectionInfo getDataCollectionInfo(
      List<MetricCollectionInfo> metricDefinitions, DatadogMetricCVConfig cvConfig) {
    DatadogMetricsDataCollectionInfo dataCollectionInfo = DatadogMetricsDataCollectionInfo.builder()
                                                              .metricDefinitions(metricDefinitions)
                                                              .groupName(cvConfig.getDashboardName())
                                                              .build();
    dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }
}
