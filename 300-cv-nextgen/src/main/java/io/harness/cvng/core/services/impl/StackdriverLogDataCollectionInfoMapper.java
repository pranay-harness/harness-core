package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.StackdriverLogDataCollectionInfo;
import io.harness.cvng.beans.stackdriver.StackdriverLogDefinition;
import io.harness.cvng.core.entities.StackdriverLogCVConfig;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

public class StackdriverLogDataCollectionInfoMapper
    implements DataCollectionInfoMapper<StackdriverLogDataCollectionInfo, StackdriverLogCVConfig> {
  @Override
  public StackdriverLogDataCollectionInfo toDataCollectionInfo(StackdriverLogCVConfig cvConfig, TaskType taskType) {
    StackdriverLogDefinition definition = StackdriverLogDefinition.builder()
                                              .name(cvConfig.getQueryName())
                                              .query(cvConfig.getQuery())
                                              .messageIdentifier(cvConfig.getMessageIdentifier())
                                              .serviceInstanceIdentifier(cvConfig.getServiceInstanceIdentifier())
                                              .build();
    StackdriverLogDataCollectionInfo stackdriverLogDataCollectionInfo =
        StackdriverLogDataCollectionInfo.builder().logDefinition(definition).build();
    stackdriverLogDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return stackdriverLogDataCollectionInfo;
  }
}
