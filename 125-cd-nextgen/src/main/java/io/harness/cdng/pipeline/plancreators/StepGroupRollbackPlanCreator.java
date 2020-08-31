package io.harness.cdng.pipeline.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.cdng.executionplan.CDPlanCreatorType;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

public class StepGroupRollbackPlanCreator extends AbstractPlanCreatorWithChildren<StepGroupElement>
    implements SupportDefinedExecutorPlanCreator<StepGroupElement> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  protected Map<String, List<CreateExecutionPlanResponse>> createPlanForChildren(
      StepGroupElement stepGroupElement, CreateExecutionPlanContext context) {
    Map<String, List<CreateExecutionPlanResponse>> childrenPlanMap = new HashMap<>();
    final List<CreateExecutionPlanResponse> rollbackStepsPlan =
        getPlanForRollbackSteps(context, stepGroupElement.getRollbackSteps());
    childrenPlanMap.put("ROLLBACK_STEPS", rollbackStepsPlan);
    return childrenPlanMap;
  }

  @Override
  protected CreateExecutionPlanResponse createPlanForSelf(StepGroupElement stepGroupElement,
      Map<String, List<CreateExecutionPlanResponse>> planForChildrenMap, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> planForSteps = planForChildrenMap.get("ROLLBACK_STEPS");
    final PlanNode stepGroupNode = prepareStepGroupNode(stepGroupElement, planForSteps);
    return CreateExecutionPlanResponse.builder()
        .planNode(stepGroupNode)
        .planNodes(getPlanNodes(planForSteps))
        .startingNodeId(stepGroupNode.getUuid())
        .build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<CreateExecutionPlanResponse> planForSteps) {
    return planForSteps.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<CreateExecutionPlanResponse> getPlanForRollbackSteps(
      CreateExecutionPlanContext context, List<ExecutionWrapper> stepsSection) {
    return stepsSection.stream()
        .map(step -> getPlanCreatorForStep(context, step).createPlan(step, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<ExecutionWrapper> getPlanCreatorForStep(
      CreateExecutionPlanContext context, ExecutionWrapper step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("No execution plan creator found for step [%s]", step));
  }

  private PlanNode prepareStepGroupNode(
      StepGroupElement stepGroupElement, List<CreateExecutionPlanResponse> planForSteps) {
    final String nodeId = generateUuid();
    return PlanNode.builder()
        .uuid(nodeId)
        .name(stepGroupElement.getName() + "_rollback")
        .identifier(stepGroupElement.getIdentifier() + "_rollback")
        .stepType(SectionChainStep.STEP_TYPE)
        .group(StepOutcomeGroup.STEP.name())
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeIds(planForSteps.stream()
                                              .map(CreateExecutionPlanResponse::getStartingNodeId)
                                              .collect(Collectors.toList()))
                            .build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.CHILD_CHAIN).build())
                                   .build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof StepGroupElement;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(CDPlanCreatorType.STEP_GROUP_ROLLBACK_PLAN_CREATOR.getName());
  }

  @Override
  protected String getPlanNodeType(StepGroupElement input) {
    return PlanNodeType.STEP_GROUP_ROLLBACK.name();
  }
}
