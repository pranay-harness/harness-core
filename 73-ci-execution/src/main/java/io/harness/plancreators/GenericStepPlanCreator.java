package io.harness.plancreators;

import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;

import com.google.inject.Singleton;

import io.harness.beans.steps.AbstractStepWithMetaInfo;
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
public class GenericStepPlanCreator implements SupportDefinedExecutorPlanCreator<AbstractStepWithMetaInfo> {
  @Override
  public CreateExecutionPlanResponse createPlan(
      AbstractStepWithMetaInfo abstractStepWithMetaInfo, CreateExecutionPlanContext context) {
    final PlanNode abstractStepMetaInfoNode = prepareStepExecutionNode(abstractStepWithMetaInfo, context);

    return CreateExecutionPlanResponse.builder()
        .planNode(abstractStepMetaInfoNode)
        .startingNodeId(abstractStepMetaInfoNode.getUuid())
        .build();
  }

  private PlanNode prepareStepExecutionNode(
      AbstractStepWithMetaInfo abstractStepWithMetaInfo, CreateExecutionPlanContext context) {
    return PlanNode.builder()
        .uuid(abstractStepWithMetaInfo.getStepMetadata().getUuid())
        .name(abstractStepWithMetaInfo.getStepMetadata().getUuid())
        .identifier(abstractStepWithMetaInfo.getIdentifier())
        .stepType(abstractStepWithMetaInfo.getNonYamlInfo().getStepType())
        .stepParameters(abstractStepWithMetaInfo)
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof AbstractStepWithMetaInfo;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STEP_PLAN_CREATOR.getName());
  }
}
