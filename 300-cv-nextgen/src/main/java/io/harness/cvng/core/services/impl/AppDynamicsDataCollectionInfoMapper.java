package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.List;

public class AppDynamicsDataCollectionInfoMapper
    implements DataCollectionInfoMapper<AppDynamicsDataCollectionInfo, AppDynamicsCVConfig> {
  @Override
  public AppDynamicsDataCollectionInfo toDataCollectionInfo(AppDynamicsCVConfig cvConfig) {
    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo = AppDynamicsDataCollectionInfo.builder()
                                                                      .applicationName(cvConfig.getApplicationName())
                                                                      .tierName(cvConfig.getTierName())
                                                                      .metricPack(cvConfig.getMetricPack().toDTO())
                                                                      .build();
    appDynamicsDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return appDynamicsDataCollectionInfo;
  }

  @Override
  public AppDynamicsDataCollectionInfo toDataCollectionInfoForSLI(
      List<AppDynamicsCVConfig> cvConfig, ServiceLevelIndicator serviceLevelIndicator) {
    return null;
  }
}
