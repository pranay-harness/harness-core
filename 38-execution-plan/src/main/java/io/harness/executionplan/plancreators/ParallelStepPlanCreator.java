package io.harness.executionplan.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.constants.PlanCreatorType.STEP_PLAN_CREATOR;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.yaml.core.Parallel;
import io.harness.yaml.core.auxiliary.intfc.StepWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ParallelStepPlanCreator implements SupportDefinedExecutorPlanCreator<Parallel> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public CreateExecutionPlanResponse createPlan(Parallel parallelStep, CreateExecutionPlanContext context) {
    final List<CreateExecutionPlanResponse> planForSteps = getPlanCreatorForSteps(context, parallelStep);
    final PlanNode parallelExecutionNode = prepareParallelExecutionNode(context,
        planForSteps.stream().map(CreateExecutionPlanResponse::getStartingNodeId).collect(Collectors.toList()));
    return CreateExecutionPlanResponse.builder()
        .planNode(parallelExecutionNode)
        .planNodes(planForSteps.stream()
                       .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
                       .collect(Collectors.toList()))
        .startingNodeId(parallelExecutionNode.getUuid())
        .build();
  }

  private List<CreateExecutionPlanResponse> getPlanCreatorForSteps(
      CreateExecutionPlanContext context, Parallel parallelStep) {
    return parallelStep.getSections()
        .stream()
        .map(step -> getPlanCreatorForStep(context, step).createPlan(step, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<StepWrapper> getPlanCreatorForStep(
      CreateExecutionPlanContext context, StepWrapper step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("no execution plan creator found for step [%s]", step));
  }

  private PlanNode prepareParallelExecutionNode(CreateExecutionPlanContext context, List<String> childNodeIds) {
    final String deploymentStageUid = generateUuid();

    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name("parallel-step")
        .identifier("parallel-step-" + deploymentStageUid)
        .stepType(ForkStep.STEP_TYPE)
        .stepParameters(ForkStepParameters.builder().parallelNodeIds(childNodeIds).build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.CHILDREN).build())
                                   .build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType()) && searchContext.getObjectToPlan() instanceof Parallel;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STEP_PLAN_CREATOR.getName());
  }
}
