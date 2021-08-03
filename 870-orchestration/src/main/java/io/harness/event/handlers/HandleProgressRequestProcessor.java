package io.harness.event.handlers;

import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.events.HandleProgressRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.data.progressdata.PmsProgressData;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class HandleProgressRequestProcessor implements SdkResponseProcessor {
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    HandleProgressRequest progressRequest = event.getProgressRequest();
    PmsProgressData progressDoc = PmsProgressData.parse(progressRequest.getProgressJson());
    nodeExecutionService.update(
        event.getNodeExecutionId(), ops -> setUnset(ops, NodeExecutionKeys.progressData, progressDoc));
  }
}
