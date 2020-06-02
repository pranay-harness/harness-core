package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.EXECUTION_PHASES_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.executionplan.CDPlanCreatorType;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.cdng.pipeline.steps.DeploymentStageStep;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
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
    final PlanNode deploymentStageNode = prepareDeploymentNode(deploymentStage, context, planForExecution);

    return CreateExecutionPlanResponse.builder()
        .planNode(deploymentStageNode)
        .planNodes(planForExecution.getPlanNodes())
        .startingNodeId(deploymentStageNode.getUuid())
        .build();
  }

  private PlanNode prepareDeploymentNode(DeploymentStage deploymentStage, CreateExecutionPlanContext context,
      CreateExecutionPlanResponse planForExecution) {
    final String deploymentStageUid = generateUuid();

    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name(deploymentStage.getIdentifier())
        .identifier(deploymentStage.getIdentifier())
        .stepType(DeploymentStageStep.STEP_TYPE)
        .stepParameters(
            DeploymentStageStepParameters.builder()
                .deploymentStage(deploymentStage)
                .fieldToExecutionNodeIdMap(ImmutableMap.of("execution", planForExecution.getStartingNodeId()))
                .build())

        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.CHILD).build()).build())
        .build();
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
    return Collections.singletonList(CDPlanCreatorType.STAGE_PLAN_CREATOR.getName());
  }
}
