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
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class HandleStepResponseRequestProcessor implements SdkResponseProcessor {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    HandleStepResponseRequest request = event.getHandleStepResponseRequest();
    if (request.hasExecutableResponse()) {
      nodeExecutionService.update(event.getNodeExecutionId(),
          ops -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()));
    }
    engine.handleStepResponse(event.getNodeExecutionId(), request.getStepResponse());
  }
}
