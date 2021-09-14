/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.FacilitatorResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class FacilitateResponseRequestProcessor implements SdkResponseProcessor {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private OrchestrationEngine orchestrationEngine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    log.info("Starting to process facilitation response");
    FacilitatorResponseRequest request = event.getFacilitatorResponseRequest();
    FacilitatorResponseProto facilitatorResponseProto = request.getFacilitatorResponse();
    if (facilitatorResponseProto.getIsSuccessful()) {
      orchestrationEngine.facilitateExecution(event.getNodeExecutionId(), facilitatorResponseProto);
    } else {
      StepResponseProto stepResponseProto = StepResponseProto.newBuilder().setStatus(Status.FAILED).build();
      orchestrationEngine.handleStepResponse(event.getNodeExecutionId(), stepResponseProto);
    }
  }
}
