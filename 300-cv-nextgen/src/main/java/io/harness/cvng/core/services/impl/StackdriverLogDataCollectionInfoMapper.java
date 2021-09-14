/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.StackdriverLogDataCollectionInfo;
import io.harness.cvng.beans.stackdriver.StackdriverLogDefinition;
import io.harness.cvng.core.entities.StackdriverLogCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

public class StackdriverLogDataCollectionInfoMapper
    implements DataCollectionInfoMapper<StackdriverLogDataCollectionInfo, StackdriverLogCVConfig> {
  @Override
  public StackdriverLogDataCollectionInfo toDataCollectionInfo(StackdriverLogCVConfig cvConfig) {
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
