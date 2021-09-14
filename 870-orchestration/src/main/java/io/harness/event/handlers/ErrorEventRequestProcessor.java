/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.EventErrorRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.tasks.FailureResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ErrorEventRequestProcessor implements SdkResponseProcessor {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    EventErrorRequest request = event.getEventErrorRequest();
    waitNotifyEngine.doneWith(request.getEventNotifyId(),
        FailureResponseData.builder()
            .errorMessage(request.getFailureInfo().getErrorMessage())
            .failureTypes(
                EngineExceptionUtils.transformToWingsFailureTypes(request.getFailureInfo().getFailureTypesList()))
            .build());
  }
}
