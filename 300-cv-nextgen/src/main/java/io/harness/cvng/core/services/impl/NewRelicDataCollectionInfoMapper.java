package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.NewRelicDataCollectionInfo;
import io.harness.cvng.core.entities.NewRelicCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.List;

public class NewRelicDataCollectionInfoMapper
    implements DataCollectionInfoMapper<NewRelicDataCollectionInfo, NewRelicCVConfig> {
  @Override
  public NewRelicDataCollectionInfo toDataCollectionInfo(NewRelicCVConfig cvConfig) {
    NewRelicDataCollectionInfo newRelicDataCollectionInfo = NewRelicDataCollectionInfo.builder()
                                                                .applicationId(cvConfig.getApplicationId())
                                                                .applicationName(cvConfig.getApplicationName())
                                                                .metricPack(cvConfig.getMetricPack().toDTO())
                                                                .build();
    newRelicDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return newRelicDataCollectionInfo;
  }

  @Override
  public NewRelicDataCollectionInfo toDataCollectionInfoForSLI(
      List<NewRelicCVConfig> cvConfig, ServiceLevelIndicator serviceLevelIndicator) {
    throw new IllegalStateException("SLI is not configured for Newrelic Metric");
  }
}
