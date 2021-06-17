package io.harness.event.handlers;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.EnumSet;
import java.util.List;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class AddExecutableResponseRequestProcessor implements SdkResponseProcessor {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    AddExecutableResponseRequest request = event.getSdkResponseEventRequest().getAddExecutableResponseRequest();
    List<String> callbackIds = request.getCallbackIdsList();
    if (EmptyPredicate.isNotEmpty(callbackIds)) {
      OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(request.getNodeExecutionId()).build();
      waitNotifyEngine.waitForAllOn(publisherName, callback, callbackIds.toArray(new String[0]));
    }

    if (request.getStatus() == Status.NO_OP) {
      nodeExecutionService.update(request.getNodeExecutionId(),
          ops -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()));
    } else {
      nodeExecutionService.updateStatusWithOps(request.getNodeExecutionId(), request.getStatus(),
          ops
          -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()),
          EnumSet.noneOf(Status.class));
    }
  }
}
