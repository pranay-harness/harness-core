package io.harness.executionplan.plancreator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.util.Collections.singletonList;

import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.executionplan.plancreator.beans.PlanCreatorType;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.steps.StepType;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.section.chain.SectionChainStep;
import io.harness.steps.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.StageType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StagesPlanCreator extends AbstractPlanCreatorWithChildren<List<StageElementWrapper>>
    implements SupportDefinedExecutorPlanCreator<List<StageElementWrapper>> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      List<StageElementWrapper> stagesList, ExecutionPlanCreationContext context) {
    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();
    List<ExecutionPlanCreatorResponse> planForStages = getPlanForStages(context, stagesList);
    childrenPlanMap.put("STAGES", planForStages);
    return childrenPlanMap;
  }

  @Override
  public ExecutionPlanCreatorResponse createPlanForSelf(List<StageElementWrapper> input,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> planForStages = planForChildrenMap.get("STAGES");
    final PlanNode stagesPlanNode = prepareStagesNode(planForStages);
    return ExecutionPlanCreatorResponse.builder()
        .planNode(stagesPlanNode)
        .planNodes(getPlanNodes(planForStages))
        .startingNodeId(stagesPlanNode.getUuid())
        .build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<ExecutionPlanCreatorResponse> planForSteps) {
    return planForSteps.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<ExecutionPlanCreatorResponse> getPlanForStages(
      ExecutionPlanCreationContext context, List<StageElementWrapper> stages) {
    List<ExecutionPlanCreatorResponse> list = new ArrayList<>();
    for (StageElementWrapper stage : stages) {
      ExecutionPlanCreatorResponse plan;
      if (stage instanceof ParallelStageElement) {
        ParallelStageElement parallelStage = (ParallelStageElement) stage;
        plan = getPlanCreatorForParallelStage(context, parallelStage).createPlan(parallelStage, context);
      } else {
        StageElement stageElement = (StageElement) stage;
        plan = getPlanCreatorForStage(context, stageElement.getStageType())
                   .createPlan(stageElement.getStageType(), context);
      }
      list.add(plan);
    }
    return list;
  }

  private ExecutionPlanCreator<StageType> getPlanCreatorForStage(
      ExecutionPlanCreationContext context, StageType stage) {
    return planCreatorHelper.getExecutionPlanCreator(
        PlanCreatorType.STAGE_PLAN_CREATOR.getName(), stage, context, "no execution plan creator found for stage");
  }

  private ExecutionPlanCreator<ParallelStageElement> getPlanCreatorForParallelStage(
      ExecutionPlanCreationContext context, ParallelStageElement parallelStage) {
    return planCreatorHelper.getExecutionPlanCreator(PlanCreatorType.STAGE_PLAN_CREATOR.getName(), parallelStage,
        context, "no execution plan creator found for  parallelStage");
  }

  private PlanNode prepareStagesNode(List<ExecutionPlanCreatorResponse> planForStages) {
    final String nodeId = generateUuid();
    return PlanNode.builder()
        .uuid(nodeId)
        .name(PlanCreatorConstants.STAGES_NODE_IDENTIFIER)
        .identifier(PlanCreatorConstants.STAGES_NODE_IDENTIFIER)
        .stepType(StepType.newBuilder().setType(SectionChainStep.STEP_TYPE.getType()).build())
        .group(StepOutcomeGroup.STAGES.name())
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeIds(planForStages.stream()
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
    final Object objectToPlan = searchContext.getObjectToPlan();
    return getSupportedTypes().contains(searchContext.getType())
        && objectToPlan
        instanceof List<?> && ((List<?>) objectToPlan).stream().allMatch(o -> o instanceof StageElementWrapper);
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(PlanCreatorType.STAGES_PLAN_CREATOR.getName());
  }

  @Override
  public String getPlanNodeType(List<StageElementWrapper> input) {
    return PlanNodeType.STAGES.name();
  }
}
