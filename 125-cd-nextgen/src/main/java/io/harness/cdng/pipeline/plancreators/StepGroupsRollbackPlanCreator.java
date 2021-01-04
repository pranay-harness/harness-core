package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.STEP_GROUPS_ROLLBACK_PLAN_CREATOR;

import static java.util.Collections.singletonList;

import io.harness.cdng.executionplan.CDPlanCreatorType;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.data.structure.UUIDGenerator;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.core.impl.ExecutionPlanCreatorResponseImpl.ExecutionPlanCreatorResponseImplBuilder;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StepGroupsRollbackPlanCreator extends AbstractPlanCreatorWithChildren<ExecutionElement>
    implements SupportDefinedExecutorPlanCreator<ExecutionElement> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  protected String getPlanNodeType(ExecutionElement input) {
    return PlanNodeType.STEP_GROUPS_ROLLBACK.name();
  }

  @Override
  protected Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      ExecutionElement executionElement, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> stepGroupsRollbackPlanList = new ArrayList<>();

    List<ExecutionWrapper> steps = executionElement.getSteps();
    for (int i = steps.size() - 1; i >= 0; i--) {
      ExecutionWrapper executionWrapper = steps.get(i);
      if (PlanCreatorHelper.isStepGroupWithRollbacks(executionWrapper)) {
        final ExecutionPlanCreator<StepGroupElement> executionRollbackPlanCreator =
            executionPlanCreatorHelper.getExecutionPlanCreator(
                CDPlanCreatorType.STEP_GROUP_ROLLBACK_PLAN_CREATOR.getName(), (StepGroupElement) executionWrapper,
                context, "No execution plan creator found for Step Group Rollback Plan Creator");
        ExecutionPlanCreatorResponse stepGroupRollbackPlan =
            executionRollbackPlanCreator.createPlan((StepGroupElement) executionWrapper, context);
        stepGroupsRollbackPlanList.add(stepGroupRollbackPlan);
      } else if (executionWrapper instanceof ParallelStepElement) {
        for (ExecutionWrapper section : ((ParallelStepElement) executionWrapper).getSections()) {
          if (PlanCreatorHelper.isStepGroupWithRollbacks(section)) {
            final ExecutionPlanCreator<ParallelStepElement> executionRollbackPlanCreator =
                executionPlanCreatorHelper.getExecutionPlanCreator(
                    CDPlanCreatorType.PARALLEL_STEP_GROUP_ROLLBACK_PLAN_CREATOR.getName(),
                    (ParallelStepElement) executionWrapper, context,
                    "No execution plan creator found for Parallel Step Group Rollback Plan Creator");
            ExecutionPlanCreatorResponse parallelStepGroupRollbackPlan =
                executionRollbackPlanCreator.createPlan((ParallelStepElement) executionWrapper, context);
            stepGroupsRollbackPlanList.add(parallelStepGroupRollbackPlan);
            break;
          }
        }
      }
    }

    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();
    childrenPlanMap.put("STEP_GROUPS_ROLLBACK", stepGroupsRollbackPlanList);
    return childrenPlanMap;
  }

  @Override
  protected ExecutionPlanCreatorResponse createPlanForSelf(ExecutionElement executionElement,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> stepGroupsRollbackPlanList = planForChildrenMap.get("STEP_GROUPS_ROLLBACK");

    RollbackOptionalChildChainStepParametersBuilder sectionOptionalChildChainStepParametersBuilder =
        RollbackOptionalChildChainStepParameters.builder();

    List<ExecutionWrapper> steps = executionElement.getSteps();
    Iterator<ExecutionPlanCreatorResponse> iterator = stepGroupsRollbackPlanList.iterator();
    for (int i = steps.size() - 1; i >= 0; i--) {
      ExecutionWrapper executionWrapper = steps.get(i);
      if (PlanCreatorHelper.isStepGroupWithRollbacks(executionWrapper)) {
        sectionOptionalChildChainStepParametersBuilder.childNode(
            RollbackNode.builder()
                .nodeId(iterator.next().getStartingNodeId())
                .dependentNodeIdentifier(PlanCreatorConstants.STAGES_NODE_IDENTIFIER + "."
                    + context.getAttribute("stageIdentifier").get() + "."
                    + PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER + "."
                    + ((StepGroupElement) executionWrapper).getIdentifier())
                .build());
      } else if (executionWrapper instanceof ParallelStepElement) {
        for (ExecutionWrapper section : ((ParallelStepElement) executionWrapper).getSections()) {
          if (PlanCreatorHelper.isStepGroupWithRollbacks(section)) {
            sectionOptionalChildChainStepParametersBuilder.childNode(
                RollbackNode.builder()
                    .nodeId(iterator.next().getStartingNodeId())
                    .dependentNodeIdentifier(String.join(".",
                        PlanCreatorConstants.STAGES_NODE_IDENTIFIER + "."
                            + context.getAttribute("stageIdentifier").get() + "."
                            + PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER + "."
                            + ((StepGroupElement) section).getIdentifier()))
                    .build());
            break;
          }
        }
      }
    }

    PlanNode stepGroupsRollbackNode =
        PlanNode.builder()
            .uuid(UUIDGenerator.generateUuid())
            .name("Step Groups Rollback")
            .identifier(PlanCreatorConstants.STEP_GROUPS_ROLLBACK_NODE_IDENTIFIER)
            .stepType(RollbackOptionalChildChainStep.STEP_TYPE)
            .stepParameters(sectionOptionalChildChainStepParametersBuilder.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                    .build())
            .build();

    ExecutionPlanCreatorResponseImplBuilder createExecutionPlanResponseImplBuilder =
        ExecutionPlanCreatorResponse.builder()
            .planNode(stepGroupsRollbackNode)
            .startingNodeId(stepGroupsRollbackNode.getUuid());

    stepGroupsRollbackPlanList.forEach(stepGroupRollbackPlan
        -> createExecutionPlanResponseImplBuilder.planNodes(stepGroupRollbackPlan.getPlanNodes()));
    return createExecutionPlanResponseImplBuilder.build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof ExecutionElement;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(STEP_GROUPS_ROLLBACK_PLAN_CREATOR.getName());
  }
}
