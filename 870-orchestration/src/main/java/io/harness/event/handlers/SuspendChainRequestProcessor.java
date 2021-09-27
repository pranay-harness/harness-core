package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SuspendChainRequest;
import io.harness.pms.execution.utils.SdkResponseEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class SuspendChainRequestProcessor implements SdkResponseProcessor {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    SuspendChainRequest request = event.getSuspendChainRequest();
    nodeExecutionService.update(SdkResponseEventUtils.getNodeExecutionId(event),
        ops -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()));
    engine.resume(SdkResponseEventUtils.getNodeExecutionId(event), request.getResponseMap(), request.getIsError());
  }
}
