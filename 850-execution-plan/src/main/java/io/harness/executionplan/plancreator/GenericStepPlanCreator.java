package io.harness.executionplan.plancreator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;

import io.harness.data.structure.EmptyPredicate;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.plan.PlanNode;
import io.harness.plan.PlanNode.PlanNodeBuilder;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.steps.StepType;
import io.harness.steps.StepOutcomeGroup;
import io.harness.yaml.core.StepElement;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericStepPlanCreator implements SupportDefinedExecutorPlanCreator<StepElement> {
  @Inject private StepDependencyService stepDependencyService;

  @Override
  public ExecutionPlanCreatorResponse createPlan(StepElement stepElement, ExecutionPlanCreationContext context) {
    final PlanNode genericStepPlanNode = prepareStepExecutionNode(stepElement, context);

    return ExecutionPlanCreatorResponse.builder()
        .planNode(genericStepPlanNode)
        .startingNodeId(genericStepPlanNode.getUuid())
        .build();
  }

  private PlanNode prepareStepExecutionNode(StepElement stepElement, ExecutionPlanCreationContext context) {
    final String nodeId = generateUuid();
    String nodeName;

    if (EmptyPredicate.isEmpty(stepElement.getName())) {
      nodeName = stepElement.getIdentifier();
    } else {
      nodeName = stepElement.getName();
    }
    PlanNodeBuilder planNodeBuilder = PlanNode.builder();

    // Add Step dependencies.
    GenericStepInfo genericStepInfo = (GenericStepInfo) stepElement.getStepSpecType();
    Map<String, StepDependencySpec> stepDependencyMap = genericStepInfo.getInputStepDependencyList(context);
    if (EmptyPredicate.isNotEmpty(stepDependencyMap)) {
      stepDependencyMap.forEach(
          (key, value) -> stepDependencyService.attachDependency(value, planNodeBuilder, context));
    }
    // Register step dependency instructors.
    genericStepInfo.registerStepDependencyInstructors(stepDependencyService, context, nodeId);

    planNodeBuilder.uuid(nodeId)
        .name(nodeName)
        .identifier(stepElement.getIdentifier())
        .stepType(StepType.newBuilder().setType(genericStepInfo.getStepType().getType()).build())
        .group(StepOutcomeGroup.STEP.name())
        .stepParameters(genericStepInfo.getStepParameters())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(genericStepInfo.getFacilitatorType()).build())
                .build());

    return planNodeBuilder.build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof StepElement;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STEP_PLAN_CREATOR.getName());
  }
}
