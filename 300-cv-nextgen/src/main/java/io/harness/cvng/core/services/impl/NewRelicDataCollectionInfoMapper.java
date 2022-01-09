/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.NewRelicDataCollectionInfo;
import io.harness.cvng.beans.NewRelicDataCollectionInfo.NewRelicMetricInfoDTO;
import io.harness.cvng.core.entities.NewRelicCVConfig;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.utils.dataCollection.MetricDataCollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class NewRelicDataCollectionInfoMapper
    implements DataCollectionInfoMapper<NewRelicDataCollectionInfo, NewRelicCVConfig> {
  @Override
  public NewRelicDataCollectionInfo toDataCollectionInfo(NewRelicCVConfig cvConfig, TaskType taskType) {
    NewRelicDataCollectionInfo newRelicDataCollectionInfo = NewRelicDataCollectionInfo.builder()
                                                                .applicationId(cvConfig.getApplicationId())
                                                                .applicationName(cvConfig.getApplicationName())
                                                                .metricPack(cvConfig.getMetricPack().toDTO())
                                                                .build();
    newRelicDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());

    if (cvConfig.isCustomQuery()) {
      newRelicDataCollectionInfo.setGroupName(cvConfig.getGroupName());
      newRelicDataCollectionInfo.setCustomQuery(true);
      List<NewRelicMetricInfoDTO> metricInfoDTOS = new ArrayList<>();
      cvConfig.getMetricInfos()
          .stream()
          .filter(metricInfo -> MetricDataCollectionUtils.isMetricApplicableForDataCollection(metricInfo, taskType))
          .forEach(newRelicMetricInfo -> {
            NewRelicMetricInfoDTO dto = NewRelicMetricInfoDTO.builder()
                                            .metricName(newRelicMetricInfo.getMetricName())
                                            .metricIdentifier(newRelicMetricInfo.getIdentifier())
                                            .nrql(newRelicMetricInfo.getNrql())
                                            .responseMapping(newRelicMetricInfo.getResponseMapping().toDto())
                                            .build();
            metricInfoDTOS.add(dto);
          });
      newRelicDataCollectionInfo.setMetricInfoList(metricInfoDTOS);
    }
    return newRelicDataCollectionInfo;
  }
}
