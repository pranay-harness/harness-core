/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo.AppMetricInfoDTO;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionSLIInfoMapper;
import io.harness.cvng.core.utils.dataCollection.MetricDataCollectionUtils;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class AppDynamicsDataCollectionInfoMapper
    implements DataCollectionInfoMapper<AppDynamicsDataCollectionInfo, AppDynamicsCVConfig>,
               DataCollectionSLIInfoMapper<AppDynamicsDataCollectionInfo, AppDynamicsCVConfig> {
  @Override
  public AppDynamicsDataCollectionInfo toDataCollectionInfo(AppDynamicsCVConfig cvConfig, TaskType taskType) {
    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo =
        AppDynamicsDataCollectionInfo.builder()
            .applicationName(cvConfig.getApplicationName())
            .tierName(cvConfig.getTierName())
            .metricPack(cvConfig.getMetricPack().toDTO())
            .groupName(cvConfig.getGroupName())
            .customMetrics(
                CollectionUtils.emptyIfNull(cvConfig.getMetricInfos())
                    .stream()
                    .filter(metricInfo
                        -> MetricDataCollectionUtils.isMetricApplicableForDataCollection(metricInfo, taskType))
                    .map(metricInfo
                        -> AppMetricInfoDTO.builder()
                               .baseFolder(metricInfo.getBaseFolder())
                               .metricName(metricInfo.getMetricName())
                               .metricIdentifier(metricInfo.getIdentifier())
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

  @Override
  public AppDynamicsDataCollectionInfo toDataCollectionInfo(
      List<AppDynamicsCVConfig> cvConfigs, ServiceLevelIndicator serviceLevelIndicator) {
    List<String> sliMetricIdentifiers = serviceLevelIndicator.getMetricNames();
    cvConfigs =
        CollectionUtils.emptyIfNull(cvConfigs)
            .stream()
            .filter(
                cvConfig -> cvConfig.getMetricPack().getIdentifier().equals(CVNextGenConstants.CUSTOM_PACK_IDENTIFIER))
            .collect(Collectors.toList());
    Preconditions.checkArgument(!cvConfigs.isEmpty(), "Metrics not found in the health-source");
    AppDynamicsCVConfig baseCvConfig = cvConfigs.get(0);

    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo =
        AppDynamicsDataCollectionInfo.builder()
            .applicationName(baseCvConfig.getApplicationName())
            .tierName(baseCvConfig.getTierName())
            .metricPack(baseCvConfig.getMetricPack().toDTO())
            .groupName(baseCvConfig.getGroupName())
            .customMetrics(
                cvConfigs.stream()
                    .flatMap(cvConfig -> CollectionUtils.emptyIfNull(cvConfig.getMetricInfos()).stream())
                    .filter(metricInfo -> sliMetricIdentifiers.contains(metricInfo.getIdentifier()))
                    .map(metricInfo
                        -> AppMetricInfoDTO.builder()
                               .baseFolder(metricInfo.getBaseFolder())
                               .metricName(metricInfo.getMetricName())
                               .metricIdentifier(metricInfo.getIdentifier())
                               .metricPath(metricInfo.getMetricPath())
                               .serviceInstanceMetricPath(metricInfo.getDeploymentVerification() == null
                                       ? null
                                       : metricInfo.getDeploymentVerification().getServiceInstanceMetricPath())
                               .build())
                    .collect(Collectors.toList()))
            .build();
    appDynamicsDataCollectionInfo.setDataCollectionDsl(baseCvConfig.getDataCollectionDsl());
    return appDynamicsDataCollectionInfo;
  }
}
