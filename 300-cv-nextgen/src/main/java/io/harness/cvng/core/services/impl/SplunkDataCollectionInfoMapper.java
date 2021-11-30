package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.SplunkDataCollectionInfo;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.List;

public class SplunkDataCollectionInfoMapper
    implements DataCollectionInfoMapper<SplunkDataCollectionInfo, SplunkCVConfig> {
  @Override
  public SplunkDataCollectionInfo toDataCollectionInfo(SplunkCVConfig cvConfig) {
    SplunkDataCollectionInfo splunkDataCollectionInfo =
        SplunkDataCollectionInfo.builder()
            .query(cvConfig.getQuery())
            .serviceInstanceIdentifier(cvConfig.getServiceInstanceIdentifier())
            .build();
    splunkDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    splunkDataCollectionInfo.setHostCollectionDSL(cvConfig.getHostCollectionDSL());
    return splunkDataCollectionInfo;
  }

  @Override
  public SplunkDataCollectionInfo toDataCollectionInfoForSLI(
      List<SplunkCVConfig> cvConfig, ServiceLevelIndicator serviceLevelIndicator) {
    throw new IllegalStateException("SLI is not configured for  SplunkData");
  }
}
