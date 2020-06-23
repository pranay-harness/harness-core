package io.harness.cvng.core.services.impl;

import com.google.inject.Singleton;

import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.entities.AppDynamicsCVConfig;
@Singleton
public class AppDynamicsDataCollectionInfoMapper
    implements DataCollectionInfoMapper<AppDynamicsDataCollectionInfo, AppDynamicsCVConfig> {
  @Override
  public AppDynamicsDataCollectionInfo toDataCollectionInfo(AppDynamicsCVConfig cvConfig) {
    return AppDynamicsDataCollectionInfo.builder()
        .applicationId(cvConfig.getApplicationId())
        .metricPack(cvConfig.getMetricPack())
        .tierId(cvConfig.getTierId())
        .build();
  }
}
