package io.harness.cdng.pipeline.executions;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.common.AmbianceHelper;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.execution.NodeExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.SyncOrchestrationEventHandler;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;

import java.util.Objects;

public class PipelineExecutionUpdateEventHandler implements SyncOrchestrationEventHandler {
  @Inject private NgPipelineExecutionService ngPipelineExecutionService;
  @Inject private NodeExecutionServiceImpl nodeExecutionService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String orgId = AmbianceHelper.getOrgIdentifier(ambiance);
    String projectId = AmbianceHelper.getProjectIdentifier(ambiance);
    String nodeExecutionId = ambiance.obtainCurrentRuntimeId();
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (!shouldHandle(nodeExecution.getNode().getGroup())) {
      return;
    }
    ngPipelineExecutionService.updateStatusForGivenNode(
        accountId, orgId, projectId, ambiance.getPlanExecutionId(), nodeExecution);
  }

  private boolean shouldHandle(String stepOutcomeGroup) {
    return Objects.equals(stepOutcomeGroup, StepOutcomeGroup.STAGE.name())
        || Objects.equals(stepOutcomeGroup, StepOutcomeGroup.PIPELINE.name());
  }
}
