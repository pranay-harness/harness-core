package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.EXECUTION_PHASES_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.INFRA_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.SERVICE_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGE_PLAN_CREATOR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.executionplan.utils.PlanCreatorConfigUtils;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.ServiceConfig;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.StepGroup;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.auxiliary.intfc.PhaseWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class DeploymentStagePlanCreator implements SupportDefinedExecutorPlanCreator<DeploymentStage> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  public CreateExecutionPlanResponse createPlan(DeploymentStage deploymentStage, CreateExecutionPlanContext context) {
    try {
      prePlanning(deploymentStage, context);
      final CreateExecutionPlanResponse planForExecution =
          createPlanForExecution(deploymentStage.getDeployment().getExecution(), context);
      final CreateExecutionPlanResponse planForService =
          createPlanForService(deploymentStage.getDeployment().getService(), context);
      final CreateExecutionPlanResponse planForInfrastructure =
          createPlanForInfrastructure(deploymentStage.getDeployment().getInfrastructure(), context);
      final PlanNode deploymentStageNode =
          prepareDeploymentNode(deploymentStage, context, planForExecution, planForService, planForInfrastructure);

      return CreateExecutionPlanResponse.builder()
          .planNode(deploymentStageNode)
          .planNodes(planForService.getPlanNodes())
          .planNodes(planForInfrastructure.getPlanNodes())
          .planNodes(planForExecution.getPlanNodes())
          .startingNodeId(deploymentStageNode.getUuid())
          .build();
    } finally {
      postPlanning(deploymentStage, context);
    }
  }

  private void postPlanning(DeploymentStage deploymentStage, CreateExecutionPlanContext context) {
    PlanCreatorConfigUtils.setCurrentStageConfig(null, context);
  }

  private void prePlanning(DeploymentStage deploymentStage, CreateExecutionPlanContext context) {
    PlanCreatorConfigUtils.setCurrentStageConfig(deploymentStage, context);
  }

  private CreateExecutionPlanResponse createPlanForInfrastructure(
      PipelineInfrastructure pipelineInfrastructure, CreateExecutionPlanContext context) {
    final ExecutionPlanCreator<PipelineInfrastructure> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(INFRA_PLAN_CREATOR.getName(), pipelineInfrastructure,
            context, "No execution plan creator found for Infra Execution.");
    return executionPlanCreator.createPlan(pipelineInfrastructure, context);
  }

  private PlanNode prepareDeploymentNode(DeploymentStage deploymentStage, CreateExecutionPlanContext context,
      CreateExecutionPlanResponse planForExecution, CreateExecutionPlanResponse planForService,
      CreateExecutionPlanResponse planForInfrastructure) {
    final String deploymentStageUid = generateUuid();

    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name(deploymentStage.getDisplayName())
        .identifier(deploymentStage.getIdentifier())
        .stepType(SectionChainStep.STEP_TYPE)
        .group(StepGroup.STAGE.name())
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeId(planForService.getStartingNodeId())
                            .childNodeId(planForInfrastructure.getStartingNodeId())
                            .childNodeId(planForExecution.getStartingNodeId())
                            .build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.CHILD_CHAIN).build())
                                   .build())
        .build();
  }

  private CreateExecutionPlanResponse createPlanForService(
      ServiceConfig serviceConfig, CreateExecutionPlanContext context) {
    final ExecutionPlanCreator<ServiceConfig> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(SERVICE_PLAN_CREATOR.getName(), serviceConfig, context,
            "No execution plan creator found for Service Execution.");
    return executionPlanCreator.createPlan(serviceConfig, context);
  }

  private CreateExecutionPlanResponse createPlanForExecution(
      List<PhaseWrapper> execution, CreateExecutionPlanContext context) {
    final ExecutionPlanCreator<List<PhaseWrapper>> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(EXECUTION_PHASES_PLAN_CREATOR.getName(), execution, context,
            "no execution plan creator found for stage execution");

    return executionPlanCreator.createPlan(execution, context);
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof DeploymentStage;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STAGE_PLAN_CREATOR.getName());
  }
}
