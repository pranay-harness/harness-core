package io.harness.executionplan.plancreator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;

import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.executionplan.GenericStepInfo;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class GenericStepPlanCreator implements SupportDefinedExecutorPlanCreator<GenericStepInfo> {
  @Override
  public CreateExecutionPlanResponse createPlan(GenericStepInfo genericStepInfo, CreateExecutionPlanContext context) {
    final PlanNode genericStepPlanNode = prepareStepExecutionNode(genericStepInfo, context);

    return CreateExecutionPlanResponse.builder()
        .planNode(genericStepPlanNode)
        .startingNodeId(genericStepPlanNode.getUuid())
        .build();
  }

  private PlanNode prepareStepExecutionNode(GenericStepInfo genericStepInfo, CreateExecutionPlanContext context) {
    final String nodeId = generateUuid();
    String nodeName;

    if (EmptyPredicate.isEmpty(genericStepInfo.getName())) {
      nodeName = genericStepInfo.getIdentifier();
    } else {
      nodeName = genericStepInfo.getName();
    }
    return PlanNode.builder()
        .uuid(nodeId)
        .name(nodeName)
        .identifier(genericStepInfo.getIdentifier())
        .stepType(genericStepInfo.getStepType())
        .stepParameters(genericStepInfo)
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(genericStepInfo.getFacilitatorType()).build())
                                   .build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof GenericStepInfo;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STEP_PLAN_CREATOR.getName());
  }
}
