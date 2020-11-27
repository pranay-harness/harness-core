package io.harness.executionplan.plancreator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGE_PLAN_CREATOR;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanCreatorType;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.steps.StepType;
import io.harness.steps.fork.ForkStep;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.StageType;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ParallelStagePlanCreator implements SupportDefinedExecutorPlanCreator<ParallelStageElement> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public ExecutionPlanCreatorResponse createPlan(
      ParallelStageElement parallelStageElement, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> planForStages = getPlanForStages(context, parallelStageElement.getSections());
    PlanNode parallelExecutionNode = prepareParallelExecutionNode(
        planForStages.stream().map(ExecutionPlanCreatorResponse::getStartingNodeId).collect(Collectors.toList()));
    return ExecutionPlanCreatorResponse.builder()
        .planNode(parallelExecutionNode)
        .planNodes(planForStages.stream()
                       .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
                       .collect(Collectors.toList()))
        .startingNodeId(parallelExecutionNode.getUuid())
        .build();
  }

  private List<ExecutionPlanCreatorResponse> getPlanForStages(
      ExecutionPlanCreationContext context, List<StageElementWrapper> stages) {
    return stages.stream()
        .map(stageElementWrapper -> (StageElement) stageElementWrapper)
        .map(stageElement
            -> getPlanCreatorForStage(context, stageElement.getStageType())
                   .createPlan(stageElement.getStageType(), context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<StageType> getPlanCreatorForStage(
      ExecutionPlanCreationContext context, StageType stage) {
    return planCreatorHelper.getExecutionPlanCreator(
        PlanCreatorType.STAGE_PLAN_CREATOR.getName(), stage, context, "no execution plan creator found for stage");
  }

  private PlanNode prepareParallelExecutionNode(List<String> childNodeIds) {
    final String deploymentStageUid = generateUuid();

    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name("parallel-stage")
        .identifier("parallel-stage-" + deploymentStageUid)
        .stepType(StepType.newBuilder().setType(ForkStep.STEP_TYPE.getType()).build())
        .group(StepOutcomeGroup.STEP.name())
        .stepParameters(ForkStepParameters.builder().parallelNodeIds(childNodeIds).build())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
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
