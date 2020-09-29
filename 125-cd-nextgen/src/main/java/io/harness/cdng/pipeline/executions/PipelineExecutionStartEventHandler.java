package io.harness.cdng.pipeline.executions;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.common.AmbianceHelper;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.SyncOrchestrationEventHandler;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class PipelineExecutionStartEventHandler implements SyncOrchestrationEventHandler {
  @Inject private NgPipelineExecutionService ngPipelineExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String orgId = AmbianceHelper.getOrgIdentifier(ambiance);
    String projectId = AmbianceHelper.getProjectIdentifier(ambiance);
    PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
    PlanNode planNode = planExecution.getPlan().fetchStartingNode();
    if (!Objects.equals(planNode.getGroup(), StepOutcomeGroup.PIPELINE.name())) {
      return;
    }
    CDPipeline cdPipeline = ((CDPipelineSetupParameters) planNode.getStepParameters()).getCdPipeline();
    ngPipelineExecutionService.createPipelineExecutionSummary(accountId, orgId, projectId, planExecution, cdPipeline);
  }
}
