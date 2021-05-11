package io.harness.event.handlers;

import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.events.HandleProgressRequest;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bson.Document;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class HandleProgressResponseEventHandler implements SdkResponseEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleEvent(SdkResponseEvent event) {
    HandleProgressRequest progressRequest = event.getSdkResponseEventRequest().getProgressRequest();
    Document progressDoc = RecastOrchestrationUtils.toDocumentFromJson(progressRequest.getProgressJson());
    nodeExecutionService.update(
        progressRequest.getNodeExecutionId(), ops -> setUnset(ops, NodeExecutionKeys.progressData, progressDoc));
  }
}
