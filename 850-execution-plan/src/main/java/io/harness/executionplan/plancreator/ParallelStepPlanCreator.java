package io.harness.executionplan.plancreator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;

import static java.lang.String.format;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.steps.StepType;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.fork.ForkStep;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParallelStepPlanCreator implements SupportDefinedExecutorPlanCreator<ParallelStepElement> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public ExecutionPlanCreatorResponse createPlan(
      ParallelStepElement parallelStepElement, ExecutionPlanCreationContext context) {
    final List<ExecutionPlanCreatorResponse> planForSteps = getPlanForSteps(context, parallelStepElement);
    final PlanNode parallelExecutionNode = prepareParallelExecutionNode(
        planForSteps.stream().map(ExecutionPlanCreatorResponse::getStartingNodeId).collect(Collectors.toList()));
    return ExecutionPlanCreatorResponse.builder()
        .planNode(parallelExecutionNode)
        .planNodes(planForSteps.stream()
                       .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
                       .collect(Collectors.toList()))
        .startingNodeId(parallelExecutionNode.getUuid())
        .build();
  }

  private List<ExecutionPlanCreatorResponse> getPlanForSteps(
      ExecutionPlanCreationContext context, ParallelStepElement parallelStepElement) {
    return parallelStepElement.getSections()
        .stream()
        .map(step -> getPlanCreatorForStep(context, step).createPlan(step, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<ExecutionWrapper> getPlanCreatorForStep(
      ExecutionPlanCreationContext context, ExecutionWrapper step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("no execution plan creator found for step [%s]", step));
  }

  private PlanNode prepareParallelExecutionNode(List<String> childNodeIds) {
    final String parallelStepUuid = generateUuid();

    return PlanNode.builder()
        .uuid(parallelStepUuid)
        .name("parallel-step")
        .identifier("parallel-step-" + parallelStepUuid)
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
        && searchContext.getObjectToPlan() instanceof ParallelStepElement;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STEP_PLAN_CREATOR.getName());
  }
}
