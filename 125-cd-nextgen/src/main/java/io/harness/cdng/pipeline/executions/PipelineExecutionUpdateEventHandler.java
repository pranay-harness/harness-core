package io.harness.cdng.pipeline.executions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.steps.ServiceSpecStep;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.execution.NodeExecution;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;

import com.google.inject.Inject;
import java.util.Objects;
import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
@ToBeDeleted
public class PipelineExecutionUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject private NgPipelineExecutionService ngPipelineExecutionService;
  @Inject private NodeExecutionServiceImpl nodeExecutionService;
  @Inject private OutcomeService outcomeService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String orgId = AmbianceHelper.getOrgIdentifier(ambiance);
    String projectId = AmbianceHelper.getProjectIdentifier(ambiance);
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    String planExecutionId = ambiance.getPlanExecutionId();
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (isServiceNodeAndCompleted(nodeExecution.getNode(), nodeExecution.getStatus())) {
      Optional<ServiceOutcome> serviceOutcome = getServiceOutcome(nodeExecutionId, planExecutionId);
      serviceOutcome.ifPresent(outcome
          -> ngPipelineExecutionService.addServiceInformationToPipelineExecutionNode(
              accountId, orgId, projectId, planExecutionId, nodeExecutionId, outcome));
      return;
    }
    if (isInfrastructureNodeAndCompleted(nodeExecution.getNode(), nodeExecution.getStatus())) {
      Optional<EnvironmentOutcome> environmentOutcome = getEnvironmentOutcome(nodeExecutionId, planExecutionId);
      environmentOutcome.ifPresent(outcome
          -> ngPipelineExecutionService.addEnvironmentInformationToPipelineExecutionNode(
              accountId, orgId, projectId, planExecutionId, nodeExecutionId, outcome));
      return;
    }
    if (!shouldHandle(nodeExecution.getNode().getGroup())) {
      return;
    }
    ngPipelineExecutionService.updateStatusForGivenNode(
        accountId, orgId, projectId, ambiance.getPlanExecutionId(), nodeExecution);
  }

  private Optional<ServiceOutcome> getServiceOutcome(String planNodeId, String planExecutionId) {
    return outcomeService.findAllByRuntimeId(planExecutionId, planNodeId)
        .stream()
        .filter(outcome -> outcome instanceof ServiceOutcome)
        .map(outcome -> (ServiceOutcome) outcome)
        .findFirst();
  }

  private Optional<EnvironmentOutcome> getEnvironmentOutcome(String planNodeId, String planExecutionId) {
    return outcomeService.findAllByRuntimeId(planExecutionId, planNodeId)
        .stream()
        .filter(outcome -> outcome instanceof EnvironmentOutcome)
        .map(outcome -> (EnvironmentOutcome) outcome)
        .findFirst();
  }

  private boolean isServiceNodeAndCompleted(PlanNodeProto node, Status status) {
    return Objects.equals(node.getStepType(), ServiceSpecStep.STEP_TYPE) && status == Status.SUCCEEDED;
  }

  private boolean isInfrastructureNodeAndCompleted(PlanNodeProto node, Status status) {
    return Objects.equals(node.getStepType(), InfrastructureStep.STEP_TYPE) && status == Status.SUCCEEDED;
  }

  private boolean shouldHandle(String stepOutcomeGroup) {
    return Objects.equals(stepOutcomeGroup, StepOutcomeGroup.STAGE.name())
        || Objects.equals(stepOutcomeGroup, StepOutcomeGroup.PIPELINE.name());
  }
}
