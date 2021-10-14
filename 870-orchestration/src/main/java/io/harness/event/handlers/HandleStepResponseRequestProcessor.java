package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.execution.utils.AmbianceUtils;

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
      nodeExecutionService.update(AmbianceUtils.obtainCurrentRuntimeId(event.getAmbiance()),
          ops -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()));
    }
    engine.processStepResponse(event.getAmbiance(), request.getStepResponse());
  }
}
