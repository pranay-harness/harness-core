package io.harness.cdng.pipeline.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;

import static java.lang.String.format;

import io.harness.cdng.executionplan.CDPlanCreatorType;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.section.chain.SectionChainStep;
import io.harness.steps.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
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
  protected Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      StepGroupElement stepGroupElement, ExecutionPlanCreationContext context) {
    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();
    final List<ExecutionPlanCreatorResponse> rollbackStepsPlan =
        getPlanForRollbackSteps(context, stepGroupElement.getRollbackSteps());
    childrenPlanMap.put("ROLLBACK_STEPS", rollbackStepsPlan);
    return childrenPlanMap;
  }

  @Override
  protected ExecutionPlanCreatorResponse createPlanForSelf(StepGroupElement stepGroupElement,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> planForSteps = planForChildrenMap.get("ROLLBACK_STEPS");
    final PlanNode stepGroupNode = prepareStepGroupNode(stepGroupElement, planForSteps);
    return ExecutionPlanCreatorResponse.builder()
        .planNode(stepGroupNode)
        .planNodes(getPlanNodes(planForSteps))
        .startingNodeId(stepGroupNode.getUuid())
        .build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<ExecutionPlanCreatorResponse> planForSteps) {
    return planForSteps.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<ExecutionPlanCreatorResponse> getPlanForRollbackSteps(
      ExecutionPlanCreationContext context, List<ExecutionWrapper> stepsSection) {
    return stepsSection.stream()
        .map(step -> getPlanCreatorForStep(context, step).createPlan(step, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<ExecutionWrapper> getPlanCreatorForStep(
      ExecutionPlanCreationContext context, ExecutionWrapper step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("No execution plan creator found for step [%s]", step));
  }

  private PlanNode prepareStepGroupNode(
      StepGroupElement stepGroupElement, List<ExecutionPlanCreatorResponse> planForSteps) {
    final String nodeId = generateUuid();
    return PlanNode.builder()
        .uuid(nodeId)
        .name(stepGroupElement.getName() + "_rollback")
        .identifier(stepGroupElement.getIdentifier() + "_rollback")
        .stepType(SectionChainStep.STEP_TYPE)
        .group(StepOutcomeGroup.STEP.name())
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeIds(planForSteps.stream()
                                              .map(ExecutionPlanCreatorResponse::getStartingNodeId)
                                              .collect(Collectors.toList()))
                            .build())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
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
