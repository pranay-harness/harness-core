package ci.pipeline.execution;

import io.harness.AmbianceUtils;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.execution.NodeExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.SyncOrchestrationEventHandler;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.ambiance.Ambiance;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineExecutionUpdateEventHandler implements SyncOrchestrationEventHandler {
  @Inject private NodeExecutionServiceImpl nodeExecutionService;
  @Inject private GitBuildStatusUtility gitBuildStatusUtility;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (gitBuildStatusUtility.shouldSendStatus(nodeExecution)) {
      gitBuildStatusUtility.sendStatusToGit(nodeExecution, ambiance, accountId);
    }
  }
}
