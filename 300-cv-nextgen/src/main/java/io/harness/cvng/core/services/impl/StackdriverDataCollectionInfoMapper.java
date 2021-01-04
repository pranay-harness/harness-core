package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.StackdriverDataCollectionInfo;
import io.harness.cvng.beans.stackdriver.StackDriverMetricDefinition;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

import java.util.ArrayList;
import java.util.List;

public class StackdriverDataCollectionInfoMapper
    implements DataCollectionInfoMapper<StackdriverDataCollectionInfo, StackdriverCVConfig> {
  @Override
  public StackdriverDataCollectionInfo toDataCollectionInfo(StackdriverCVConfig cvConfig) {
    List<StackDriverMetricDefinition> metricDefinitions = new ArrayList<>();
    cvConfig.getMetricInfoList().forEach(metricInfo
        -> metricDefinitions.add(StackDriverMetricDefinition.extractFromJson(metricInfo.getJsonMetricDefinition())));
    StackdriverDataCollectionInfo dataCollectionInfo =
        StackdriverDataCollectionInfo.builder().metricDefinitions(metricDefinitions).build();
    dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }
}
