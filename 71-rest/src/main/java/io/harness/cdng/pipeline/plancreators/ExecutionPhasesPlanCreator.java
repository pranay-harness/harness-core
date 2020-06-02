package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.EXECUTION_PHASES_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.PHASE_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.pipeline.CDPhase;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.SectionStep;
import io.harness.state.core.section.SectionStepParameters;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Singleton
@Slf4j
public class ExecutionPhasesPlanCreator implements SupportDefinedExecutorPlanCreator<List<CDPhase>> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public CreateExecutionPlanResponse createPlan(List<CDPhase> stagesList, CreateExecutionPlanContext context) {
    // TODO @rk: 27/05/20 : support multiple stages
    final CDPhase phase = stagesList.get(0);
    final ExecutionPlanCreator<CDPhase> planCreatorForPhase = getPlanCreatorForPhase(context, phase);
    final CreateExecutionPlanResponse planForPhase = planCreatorForPhase.createPlan(phase, context);
    final PlanNode executionPhasesNode = prepareExecutionPhasesNode(context, planForPhase);
    return CreateExecutionPlanResponse.builder()
        .planNode(executionPhasesNode)
        .planNodes(planForPhase.getPlanNodes())
        .startingNodeId(executionPhasesNode.getUuid())
        .build();
  }

  private ExecutionPlanCreator<CDPhase> getPlanCreatorForPhase(CreateExecutionPlanContext context, CDPhase phase) {
    return planCreatorHelper.getExecutionPlanCreator(
        PHASE_PLAN_CREATOR.getName(), phase, context, format("no execution plan creator found for phase [%s]", phase));
  }

  private PlanNode prepareExecutionPhasesNode(
      CreateExecutionPlanContext context, CreateExecutionPlanResponse planForphase) {
    final String nodeId = generateUuid();

    final String EXECUTION = "EXECUTION";
    return PlanNode.builder()
        .uuid(nodeId)
        .name(EXECUTION)
        .identifier(EXECUTION)
        .stepType(SectionStep.STEP_TYPE)
        .stepParameters(SectionStepParameters.builder().childNodeId(planForphase.getStartingNodeId()).build())
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.CHILD).build()).build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    final Object objectToPlan = searchContext.getObjectToPlan();
    return getSupportedTypes().contains(searchContext.getType())
        && objectToPlan instanceof List<?> && ((List<?>) objectToPlan).stream().allMatch(o -> o instanceof CDPhase);
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(EXECUTION_PHASES_PLAN_CREATOR.getName());
  }
}
