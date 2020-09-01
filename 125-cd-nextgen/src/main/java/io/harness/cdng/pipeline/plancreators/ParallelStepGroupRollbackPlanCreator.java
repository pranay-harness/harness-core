package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.PARALLEL_STEP_GROUP_ROLLBACK_PLAN_CREATOR;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.cdng.executionplan.CDPlanCreatorType;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildrenParameters.RollbackOptionalChildrenParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildrenStep;
import io.harness.data.structure.UUIDGenerator;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.core.impl.CreateExecutionPlanResponseImpl.CreateExecutionPlanResponseImplBuilder;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

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
  protected Map<String, List<CreateExecutionPlanResponse>> createPlanForChildren(
      ParallelStepElement parallelStepElement, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> stepGroupsRollbackPlanList = new ArrayList<>();
    for (ExecutionWrapper section : parallelStepElement.getSections()) {
      if (section instanceof StepGroupElement && isNotEmpty(((StepGroupElement) section).getRollbackSteps())) {
        final ExecutionPlanCreator<StepGroupElement> executionRollbackPlanCreator =
            executionPlanCreatorHelper.getExecutionPlanCreator(
                CDPlanCreatorType.STEP_GROUP_ROLLBACK_PLAN_CREATOR.getName(), (StepGroupElement) section, context,
                "No execution plan creator found for Step Group Rollback Plan Creator");
        CreateExecutionPlanResponse stepGroupRollbackPlan =
            executionRollbackPlanCreator.createPlan((StepGroupElement) section, context);
        stepGroupsRollbackPlanList.add(stepGroupRollbackPlan);
      }
    }

    Map<String, List<CreateExecutionPlanResponse>> childrenPlanMap = new HashMap<>();
    childrenPlanMap.put("STEP_GROUPS_ROLLBACK", stepGroupsRollbackPlanList);
    return childrenPlanMap;
  }

  @Override
  protected CreateExecutionPlanResponse createPlanForSelf(ParallelStepElement parallelStepElement,
      Map<String, List<CreateExecutionPlanResponse>> planForChildrenMap, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> stepGroupsRollbackPlanList = planForChildrenMap.get("STEP_GROUPS_ROLLBACK");
    Iterator<CreateExecutionPlanResponse> iterator = stepGroupsRollbackPlanList.iterator();
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
            .facilitatorObtainment(FacilitatorObtainment.builder()
                                       .type(FacilitatorType.builder().type(FacilitatorType.CHILDREN).build())
                                       .build())
            .build();

    CreateExecutionPlanResponseImplBuilder createExecutionPlanResponseImplBuilder =
        CreateExecutionPlanResponse.builder()
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
