package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.NewRelicDataCollectionInfo;
import io.harness.cvng.core.entities.NewRelicCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

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
}
