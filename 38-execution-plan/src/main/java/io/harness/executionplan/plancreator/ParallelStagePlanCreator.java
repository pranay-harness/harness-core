package io.harness.executionplan.plancreator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGE_PLAN_CREATOR;

import com.google.inject.Inject;

import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanCreatorType;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.StepType;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.StageType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ParallelStagePlanCreator implements SupportDefinedExecutorPlanCreator<ParallelStageElement> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public CreateExecutionPlanResponse createPlan(
      ParallelStageElement parallelStageElement, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> planForStages = getPlanForStages(context, parallelStageElement.getSections());
    PlanNode parallelExecutionNode = prepareParallelExecutionNode(
        planForStages.stream().map(CreateExecutionPlanResponse::getStartingNodeId).collect(Collectors.toList()));
    return CreateExecutionPlanResponse.builder()
        .planNode(parallelExecutionNode)
        .planNodes(planForStages.stream()
                       .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
                       .collect(Collectors.toList()))
        .startingNodeId(parallelExecutionNode.getUuid())
        .build();
  }

  private List<CreateExecutionPlanResponse> getPlanForStages(
      CreateExecutionPlanContext context, List<StageElementWrapper> stages) {
    return stages.stream()
        .map(stageElementWrapper -> (StageElement) stageElementWrapper)
        .map(stageElement
            -> getPlanCreatorForStage(context, stageElement.getStageType())
                   .createPlan(stageElement.getStageType(), context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<StageType> getPlanCreatorForStage(CreateExecutionPlanContext context, StageType stage) {
    return planCreatorHelper.getExecutionPlanCreator(
        PlanCreatorType.STAGE_PLAN_CREATOR.getName(), stage, context, "no execution plan creator found for stage");
  }

  private PlanNode prepareParallelExecutionNode(List<String> childNodeIds) {
    final String deploymentStageUid = generateUuid();

    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name("parallel-stage")
        .identifier("parallel-stage-" + deploymentStageUid)
        .stepType(StepType.builder().type(ForkStep.STEP_TYPE.getType()).build())
        .group(StepOutcomeGroup.STEP.name())
        .stepParameters(ForkStepParameters.builder().parallelNodeIds(childNodeIds).build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.CHILDREN).build())
                                   .build())
        .skipExpressionChain(true)
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof ParallelStageElement;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STAGE_PLAN_CREATOR.getName());
  }
}