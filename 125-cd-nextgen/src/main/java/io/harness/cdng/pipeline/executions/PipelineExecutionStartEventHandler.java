package io.harness.cdng.pipeline.executions;

import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.AsyncOrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plan.PlanNode;
import io.harness.pms.ambiance.Ambiance;
import io.harness.serializer.json.JsonOrchestrationUtils;

import com.google.inject.Inject;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineExecutionStartEventHandler implements AsyncOrchestrationEventHandler {
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
    CDPipelineSetupParameters cdPipelineSetupParameters =
        JsonOrchestrationUtils.asObject(planNode.getStepParameters().toJson(), CDPipelineSetupParameters.class);

    ngPipelineExecutionService.createPipelineExecutionSummary(
        accountId, orgId, projectId, planExecution, cdPipelineSetupParameters);
  }
}
