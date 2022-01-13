/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.StackdriverDataCollectionInfo;
import io.harness.cvng.beans.stackdriver.StackDriverMetricDefinition;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.cvng.core.entities.StackdriverCVConfig.MetricInfo;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionSLIInfoMapper;
import io.harness.cvng.core.utils.dataCollection.MetricDataCollectionUtils;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class StackdriverDataCollectionInfoMapper
    implements DataCollectionInfoMapper<StackdriverDataCollectionInfo, StackdriverCVConfig>,
               DataCollectionSLIInfoMapper<StackdriverDataCollectionInfo, StackdriverCVConfig> {
  @Override
  public StackdriverDataCollectionInfo toDataCollectionInfo(StackdriverCVConfig cvConfig, TaskType taskType) {
    List<StackDriverMetricDefinition> metricDefinitions = new ArrayList<>();
    cvConfig.getMetricInfoList()
        .stream()
        .filter(metricInfo -> MetricDataCollectionUtils.isMetricApplicableForDataCollection(metricInfo, taskType))
        .forEach(metricInfo -> { metricDefinitions.add(getMetricCollectionInfo(metricInfo)); });
    return getDataCollectionInfo(metricDefinitions, cvConfig);
  }

  @Override
  public StackdriverDataCollectionInfo toDataCollectionInfo(
      List<StackdriverCVConfig> cvConfigList, ServiceLevelIndicator serviceLevelIndicator) {
    List<String> sliMetricIdentifiers = serviceLevelIndicator.getMetricNames();
    Preconditions.checkNotNull(cvConfigList);
    StackdriverCVConfig baseCvConfig = cvConfigList.get(0);
    List<StackDriverMetricDefinition> metricDefinitions = new ArrayList<>();
    cvConfigList.forEach(cvConfig -> cvConfig.getMetricInfoList().forEach(metricInfo -> {
      if (sliMetricIdentifiers.contains(metricInfo.getIdentifier())) {
        metricDefinitions.add(getMetricCollectionInfo(metricInfo));
      }
    }));
    return getDataCollectionInfo(metricDefinitions, baseCvConfig);
  }

  private StackDriverMetricDefinition getMetricCollectionInfo(MetricInfo metricInfo) {
    StackDriverMetricDefinition metricDefinition =
        StackDriverMetricDefinition.extractFromJson(metricInfo.getJsonMetricDefinition());
    metricDefinition.setServiceInstanceField(metricInfo.getServiceInstanceField());
    metricDefinition.setMetricIdentifier(metricInfo.getIdentifier());
    return metricDefinition;
  }

  private StackdriverDataCollectionInfo getDataCollectionInfo(
      List<StackDriverMetricDefinition> metricDefinitions, StackdriverCVConfig cvConfig) {
    StackdriverDataCollectionInfo dataCollectionInfo =
        StackdriverDataCollectionInfo.builder().metricDefinitions(metricDefinitions).build();
    dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }
}
