package io.harness.event.handlers;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.EnumSet;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class AddExecutableResponseRequestProcessor implements SdkResponseProcessor {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    AddExecutableResponseRequest request = event.getAddExecutableResponseRequest();
    nodeExecutionService.updateStatusWithOps(event.getNodeExecutionId(), request.getStatus(),
        ops
        -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()),
        EnumSet.noneOf(Status.class));
  }
}
