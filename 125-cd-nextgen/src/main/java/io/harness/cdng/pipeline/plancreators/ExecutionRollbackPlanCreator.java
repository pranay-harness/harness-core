package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.EXECUTION_ROLLBACK_PLAN_CREATOR;

import static java.util.Collections.singletonList;

import io.harness.data.structure.UUIDGenerator;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.core.impl.ExecutionPlanCreatorResponseImpl.ExecutionPlanCreatorResponseImplBuilder;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.executionplan.plancreator.beans.PlanCreatorType;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.steps.section.chain.SectionChainStep;
import io.harness.steps.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutionRollbackPlanCreator extends AbstractPlanCreatorWithChildren<ExecutionElement>
    implements SupportDefinedExecutorPlanCreator<ExecutionElement> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  protected String getPlanNodeType(ExecutionElement input) {
    return PlanNodeType.EXECUTION_ROLLBACK.name();
  }

  @Override
  protected Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      ExecutionElement executionElement, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> stepsPlanList = new ArrayList<>();
    for (ExecutionWrapper rollbackStep : executionElement.getRollbackSteps()) {
      final ExecutionPlanCreator<ExecutionWrapper> executionRollbackPlanCreator =
          executionPlanCreatorHelper.getExecutionPlanCreator(PlanCreatorType.STEP_PLAN_CREATOR.getName(), rollbackStep,
              context, "No execution plan creator found for Step Plan Creator");
      ExecutionPlanCreatorResponse rollbackStepPlan = executionRollbackPlanCreator.createPlan(rollbackStep, context);
      stepsPlanList.add(rollbackStepPlan);
    }

    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();
    childrenPlanMap.put("ROLLBACK_STEPS", stepsPlanList);
    return childrenPlanMap;
  }

  @Override
  protected ExecutionPlanCreatorResponse createPlanForSelf(ExecutionElement input,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> rollbackStepsPlan = planForChildrenMap.get("ROLLBACK_STEPS");

    PlanNode executionRollbackNode =
        PlanNode.builder()
            .uuid(UUIDGenerator.generateUuid())
            .name("Execution Rollback")
            .identifier(PlanCreatorConstants.EXECUTION_ROLLBACK_NODE_IDENTIFIER)
            .stepType(SectionChainStep.STEP_TYPE)
            .stepParameters(SectionChainStepParameters.builder()
                                .childNodeIds(rollbackStepsPlan.stream()
                                                  .map(ExecutionPlanCreatorResponse::getStartingNodeId)
                                                  .collect(Collectors.toList()))
                                .build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                    .build())
            .build();

    ExecutionPlanCreatorResponseImplBuilder executionPlanCreatorResponseImplBuilder =
        ExecutionPlanCreatorResponse.builder()
            .planNode(executionRollbackNode)
            .startingNodeId(executionRollbackNode.getUuid());

    rollbackStepsPlan.forEach(
        rollbackStepPlan -> executionPlanCreatorResponseImplBuilder.planNodes(rollbackStepPlan.getPlanNodes()));
    return executionPlanCreatorResponseImplBuilder.build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof ExecutionElement;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(EXECUTION_ROLLBACK_PLAN_CREATOR.getName());
  }
}
