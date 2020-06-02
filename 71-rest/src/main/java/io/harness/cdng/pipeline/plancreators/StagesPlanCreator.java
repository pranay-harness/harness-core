package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.STAGES_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.STAGE_PLAN_CREATOR;
import static io.harness.cdng.pipeline.steps.StagesStep.STEP_TYPE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.pipeline.CDStage;
import io.harness.cdng.pipeline.beans.StagesStepParameters;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class StagesPlanCreator implements SupportDefinedExecutorPlanCreator<List<CDStage>> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public CreateExecutionPlanResponse createPlan(List<CDStage> stagesList, CreateExecutionPlanContext context) {
    // TODO @rk: 27/05/20 : support multiple stages
    final CDStage stage = stagesList.get(0);
    final ExecutionPlanCreator<CDStage> planCreatorForStage = getPlanCreatorForStage(context, stage);
    final CreateExecutionPlanResponse planForStage = planCreatorForStage.createPlan(stage, context);
    final PlanNode stagesNode = prepareStagesNode(context, planForStage);
    return CreateExecutionPlanResponse.builder()
        .planNode(stagesNode)
        .planNodes(planForStage.getPlanNodes())
        .startingNodeId(stagesNode.getUuid())
        .build();
  }

  private ExecutionPlanCreator<CDStage> getPlanCreatorForStage(CreateExecutionPlanContext context, CDStage stage) {
    return planCreatorHelper.getExecutionPlanCreator(
        STAGE_PLAN_CREATOR.getName(), stage, context, "no execution plan creator found for  stage");
  }

  private PlanNode prepareStagesNode(CreateExecutionPlanContext context, CreateExecutionPlanResponse planForStage) {
    final String nodeId = generateUuid();

    final String STAGES = "STAGES";
    return PlanNode.builder()
        .uuid(nodeId)
        .name(STAGES)
        .identifier(STAGES)
        .stepType(STEP_TYPE)
        .skipExpressionChain(true)
        .stepParameters(StagesStepParameters.builder()
                            .stageNodeIds(Collections.singletonList(planForStage.getStartingNodeId()))
                            .build())
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.CHILD).build()).build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    final Object objectToPlan = searchContext.getObjectToPlan();
    return getSupportedTypes().contains(searchContext.getType())
        && objectToPlan instanceof List<?> && ((List<?>) objectToPlan).stream().allMatch(o -> o instanceof CDStage);
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(STAGES_PLAN_CREATOR.getName());
  }
}
