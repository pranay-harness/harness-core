package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.PARALLEL_STEP_GROUP_ROLLBACK_PLAN_CREATOR;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.singletonList;

import io.harness.cdng.executionplan.CDPlanCreatorType;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters.RollbackOptionalChildrenParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildrenStep;
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
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ParallelStepGroupRollbackPlanCreator extends AbstractPlanCreatorWithChildren<ParallelStepElement>
    implements SupportDefinedExecutorPlanCreator<ParallelStepElement> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  protected String getPlanNodeType(ParallelStepElement input) {
    return PlanNodeType.PARALLEL_STEP_GROUP_ROLLBACK.name();
  }

  @Override
  protected Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      ParallelStepElement parallelStepElement, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> stepGroupsRollbackPlanList = new ArrayList<>();
    for (ExecutionWrapper section : parallelStepElement.getSections()) {
      if (section instanceof StepGroupElement && isNotEmpty(((StepGroupElement) section).getRollbackSteps())) {
        final ExecutionPlanCreator<StepGroupElement> executionRollbackPlanCreator =
            executionPlanCreatorHelper.getExecutionPlanCreator(
                CDPlanCreatorType.STEP_GROUP_ROLLBACK_PLAN_CREATOR.getName(), (StepGroupElement) section, context,
                "No execution plan creator found for Step Group Rollback Plan Creator");
        ExecutionPlanCreatorResponse stepGroupRollbackPlan =
            executionRollbackPlanCreator.createPlan((StepGroupElement) section, context);
        stepGroupsRollbackPlanList.add(stepGroupRollbackPlan);
      }
    }

    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();
    childrenPlanMap.put("STEP_GROUPS_ROLLBACK", stepGroupsRollbackPlanList);
    return childrenPlanMap;
  }

  @Override
  protected ExecutionPlanCreatorResponse createPlanForSelf(ParallelStepElement parallelStepElement,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> stepGroupsRollbackPlanList = planForChildrenMap.get("STEP_GROUPS_ROLLBACK");
    Iterator<ExecutionPlanCreatorResponse> iterator = stepGroupsRollbackPlanList.iterator();
    RollbackOptionalChildrenParametersBuilder rollbackOptionalChildrenParametersBuilder =
        RollbackOptionalChildrenParameters.builder();
    for (ExecutionWrapper section : parallelStepElement.getSections()) {
      if (section instanceof StepGroupElement && isNotEmpty(((StepGroupElement) section).getRollbackSteps())) {
        RollbackNode rollbackNode = RollbackNode.builder()
                                        .nodeId(iterator.next().getStartingNodeId())
                                        .dependentNodeIdentifier(PlanCreatorConstants.STAGES_NODE_IDENTIFIER + "."
                                            + context.getAttribute("stageIdentifier").get() + "."
                                            + PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER + "."
                                            + ((StepGroupElement) section).getIdentifier())
                                        .shouldAlwaysRun(true)
                                        .build();
        rollbackOptionalChildrenParametersBuilder.parallelNode(rollbackNode);
      }
    }

    PlanNode parallelStepGroupsRollbackNode =
        PlanNode.builder()
            .uuid(UUIDGenerator.generateUuid())
            .name("Parallel Step Groups Rollback")
            .identifier(PlanCreatorConstants.PARALLEL_STEP_GROUPS_ROLLBACK_NODE_IDENTIFIER)
            .stepType(RollbackOptionalChildrenStep.STEP_TYPE)
            .stepParameters(rollbackOptionalChildrenParametersBuilder.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                    .build())
            .build();

    ExecutionPlanCreatorResponseImplBuilder createExecutionPlanResponseImplBuilder =
        ExecutionPlanCreatorResponse.builder()
            .planNode(parallelStepGroupsRollbackNode)
            .startingNodeId(parallelStepGroupsRollbackNode.getUuid());

    stepGroupsRollbackPlanList.forEach(stepGroupRollbackPlan
        -> createExecutionPlanResponseImplBuilder.planNodes(stepGroupRollbackPlan.getPlanNodes()));
    return createExecutionPlanResponseImplBuilder.build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof ParallelStepElement;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(PARALLEL_STEP_GROUP_ROLLBACK_PLAN_CREATOR.getName());
  }
}
