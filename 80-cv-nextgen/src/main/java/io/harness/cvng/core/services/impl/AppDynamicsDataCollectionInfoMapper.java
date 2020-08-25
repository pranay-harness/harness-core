package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

public class AppDynamicsDataCollectionInfoMapper
    implements DataCollectionInfoMapper<AppDynamicsDataCollectionInfo, AppDynamicsCVConfig> {
  @Override
  public AppDynamicsDataCollectionInfo toDataCollectionInfo(AppDynamicsCVConfig cvConfig) {
    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo = AppDynamicsDataCollectionInfo.builder()
                                                                      .applicationName(cvConfig.getApplicationName())
                                                                      .tierName(cvConfig.getTierName())
                                                                      .metricPack(cvConfig.getMetricPack().getDTO())
                                                                      .build();
    appDynamicsDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return appDynamicsDataCollectionInfo;
  }
}
