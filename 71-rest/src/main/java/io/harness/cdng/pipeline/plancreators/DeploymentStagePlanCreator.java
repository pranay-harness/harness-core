package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.EXECUTION_PHASES_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.SERVICE_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGE_PLAN_CREATOR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.service.Service;
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
    final CreateExecutionPlanResponse planForExecution =
        createPlanForExecution(deploymentStage.getExecution(), context);
    final CreateExecutionPlanResponse planForService = createPlanForService(deploymentStage.getService(), context);
    final PlanNode deploymentStageNode =
        prepareDeploymentNode(deploymentStage, context, planForExecution, planForService);

    return CreateExecutionPlanResponse.builder()
        .planNode(deploymentStageNode)
        .planNodes(planForService.getPlanNodes())
        .planNodes(planForExecution.getPlanNodes())
        .startingNodeId(deploymentStageNode.getUuid())
        .build();
  }

  private PlanNode prepareDeploymentNode(DeploymentStage deploymentStage, CreateExecutionPlanContext context,
      CreateExecutionPlanResponse planForExecution, CreateExecutionPlanResponse planForService) {
    final String deploymentStageUid = generateUuid();

    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name(deploymentStage.getName())
        .identifier(deploymentStage.getIdentifier())
        .stepType(SectionChainStep.STEP_TYPE)
        .group(StepGroup.STAGE.name())
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeId(planForService.getStartingNodeId())
                            .childNodeId(planForExecution.getStartingNodeId())
                            .build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.CHILD_CHAIN).build())
                                   .build())
        .build();
  }

  private CreateExecutionPlanResponse createPlanForService(Service service, CreateExecutionPlanContext context) {
    final ExecutionPlanCreator<Service> executionPlanCreator = executionPlanCreatorHelper.getExecutionPlanCreator(
        SERVICE_PLAN_CREATOR.getName(), service, context, "No execution plan creator found for Service Execution.");
    return executionPlanCreator.createPlan(service, context);
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
