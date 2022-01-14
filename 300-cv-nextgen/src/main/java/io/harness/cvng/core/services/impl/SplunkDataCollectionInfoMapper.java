package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.SplunkDataCollectionInfo;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

public class SplunkDataCollectionInfoMapper
    implements DataCollectionInfoMapper<SplunkDataCollectionInfo, SplunkCVConfig> {
  @Override
  public SplunkDataCollectionInfo toDataCollectionInfo(SplunkCVConfig cvConfig, TaskType taskType) {
    SplunkDataCollectionInfo splunkDataCollectionInfo =
        SplunkDataCollectionInfo.builder()
            .query(cvConfig.getQuery())
            .serviceInstanceIdentifier(cvConfig.getServiceInstanceIdentifier())
            .build();
    splunkDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    splunkDataCollectionInfo.setHostCollectionDSL(cvConfig.getHostCollectionDSL());
    return splunkDataCollectionInfo;
  }
}
