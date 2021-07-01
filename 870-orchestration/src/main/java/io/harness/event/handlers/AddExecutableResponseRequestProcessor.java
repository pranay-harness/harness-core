package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class AddExecutableResponseRequestProcessor implements SdkResponseProcessor {
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    AddExecutableResponseRequest request = event.getAddExecutableResponseRequest();
    nodeExecutionService.update(event.getNodeExecutionId(),
        ops -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()));
  }
}
