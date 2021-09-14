/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
                                                                      .metricPack(cvConfig.getMetricPack().toDTO())
                                                                      .build();
    appDynamicsDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return appDynamicsDataCollectionInfo;
  }
}
