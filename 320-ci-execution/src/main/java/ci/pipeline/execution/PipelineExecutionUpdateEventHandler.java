package ci.pipeline.execution;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineExecutionUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GitBuildStatusUtility gitBuildStatusUtility;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    NodeExecutionProto nodeExecutionProto = event.getNodeExecutionProto();
    NodeExecution nodeExecution = NodeExecutionMapper.fromNodeExecutionProto(nodeExecutionProto);
    Ambiance ambiance = nodeExecution.getAmbiance();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    try {
      if (gitBuildStatusUtility.shouldSendStatus(nodeExecution)) {
        log.info("Received event with status {} to update git status for stage {}", nodeExecution.getStatus(),
            nodeExecution.getNode().getGroup());
        gitBuildStatusUtility.sendStatusToGit(nodeExecution, ambiance, accountId);
      }
    } catch (Exception ex) {
      log.error("Failed to send git status update task for node {}", nodeExecution.getUuid(), ex);
    }
  }
}
