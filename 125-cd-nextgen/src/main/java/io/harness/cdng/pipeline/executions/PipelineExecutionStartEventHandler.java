package io.harness.cdng.pipeline.executions;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Deprecated
@ToBeDeleted
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
    PlanNodeProto planNode = planExecution.getPlan().fetchStartingNode();
    if (!Objects.equals(planNode.getGroup(), StepOutcomeGroup.PIPELINE.name())) {
      return;
    }
    CDPipelineSetupParameters cdPipelineSetupParameters =
        RecastOrchestrationUtils.fromDocumentJson(planNode.getStepParameters(), CDPipelineSetupParameters.class);

    ngPipelineExecutionService.createPipelineExecutionSummary(
        accountId, orgId, projectId, planExecution, cdPipelineSetupParameters);
  }
}
