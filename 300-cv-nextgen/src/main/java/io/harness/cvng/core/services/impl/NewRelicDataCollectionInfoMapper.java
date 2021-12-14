package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.beans.NewRelicDataCollectionInfo;
import io.harness.cvng.beans.NewRelicDataCollectionInfo.NewRelicMetricInfoDTO;
import io.harness.cvng.core.entities.NewRelicCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

import java.util.ArrayList;
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
    if (isNotEmpty(cvConfig.getMetricInfos())) {
      newRelicDataCollectionInfo.setGroupName(cvConfig.getGroupName());
      List<NewRelicMetricInfoDTO> metricInfoDTOS = new ArrayList<>();
      cvConfig.getMetricInfos().forEach(newRelicMetricInfo -> {
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
