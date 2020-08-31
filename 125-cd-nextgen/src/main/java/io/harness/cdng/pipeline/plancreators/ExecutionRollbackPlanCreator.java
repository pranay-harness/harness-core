package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.EXECUTION_ROLLBACK_PLAN_CREATOR;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.data.structure.UUIDGenerator;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.core.impl.CreateExecutionPlanResponseImpl.CreateExecutionPlanResponseImplBuilder;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.executionplan.plancreator.beans.PlanCreatorType;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

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
  protected Map<String, List<CreateExecutionPlanResponse>> createPlanForChildren(
      ExecutionElement executionElement, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> stepsPlanList = new ArrayList<>();
    for (ExecutionWrapper rollbackStep : executionElement.getRollbackSteps()) {
      final ExecutionPlanCreator<ExecutionWrapper> executionRollbackPlanCreator =
          executionPlanCreatorHelper.getExecutionPlanCreator(PlanCreatorType.STEP_PLAN_CREATOR.getName(), rollbackStep,
              context, "No execution plan creator found for Step Plan Creator");
      CreateExecutionPlanResponse rollbackStepPlan = executionRollbackPlanCreator.createPlan(rollbackStep, context);
      stepsPlanList.add(rollbackStepPlan);
    }

    Map<String, List<CreateExecutionPlanResponse>> childrenPlanMap = new HashMap<>();
    childrenPlanMap.put("ROLLBACK_STEPS", stepsPlanList);
    return childrenPlanMap;
  }

  @Override
  protected CreateExecutionPlanResponse createPlanForSelf(ExecutionElement input,
      Map<String, List<CreateExecutionPlanResponse>> planForChildrenMap, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> rollbackStepsPlan = planForChildrenMap.get("ROLLBACK_STEPS");

    PlanNode executionRollbackNode =
        PlanNode.builder()
            .uuid(UUIDGenerator.generateUuid())
            .name("Execution Rollback")
            .identifier(PlanCreatorConstants.EXECUTION_ROLLBACK_NODE_IDENTIFIER)
            .stepType(SectionChainStep.STEP_TYPE)
            .stepParameters(SectionChainStepParameters.builder()
                                .childNodeIds(rollbackStepsPlan.stream()
                                                  .map(CreateExecutionPlanResponse::getStartingNodeId)
                                                  .collect(Collectors.toList()))
                                .build())
            .facilitatorObtainment(FacilitatorObtainment.builder()
                                       .type(FacilitatorType.builder().type(FacilitatorType.CHILD_CHAIN).build())
                                       .build())
            .build();

    CreateExecutionPlanResponseImplBuilder createExecutionPlanResponseImplBuilder =
        CreateExecutionPlanResponse.builder()
            .planNode(executionRollbackNode)
            .startingNodeId(executionRollbackNode.getUuid());

    rollbackStepsPlan.forEach(
        rollbackStepPlan -> createExecutionPlanResponseImplBuilder.planNodes(rollbackStepPlan.getPlanNodes()));
    return createExecutionPlanResponseImplBuilder.build();
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
