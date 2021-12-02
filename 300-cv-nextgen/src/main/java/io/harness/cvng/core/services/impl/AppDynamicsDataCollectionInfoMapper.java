package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo.AppMetricInfoDTO;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class AppDynamicsDataCollectionInfoMapper
    implements DataCollectionInfoMapper<AppDynamicsDataCollectionInfo, AppDynamicsCVConfig> {
  @Override
  public AppDynamicsDataCollectionInfo toDataCollectionInfo(AppDynamicsCVConfig cvConfig) {
    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo =
        AppDynamicsDataCollectionInfo.builder()
            .applicationName(cvConfig.getApplicationName())
            .tierName(cvConfig.getTierName())
            .metricPack(cvConfig.getMetricPack().toDTO())
            .groupName(cvConfig.getGroupName())
            .customMetrics(
                CollectionUtils.emptyIfNull(cvConfig.getMetricInfos())
                    .stream()
                    .map(metricInfo
                        -> AppMetricInfoDTO.builder()
                               .baseFolder(metricInfo.getBaseFolder())
                               .metricName(metricInfo.getMetricName())
                               .metricPath(metricInfo.getMetricPath())
                               .serviceInstanceMetricPath(metricInfo.getDeploymentVerification() == null
                                       ? null
                                       : metricInfo.getDeploymentVerification().getServiceInstanceMetricPath())
                               .build())
                    .collect(Collectors.toList()))
            .build();
    appDynamicsDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());

    return appDynamicsDataCollectionInfo;
  }
}
