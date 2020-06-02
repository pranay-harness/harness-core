package io.harness.cdng.pipeline.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.constants.PlanCreatorType.STEP_PLAN_CREATOR;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.executionplan.CDPlanCreatorType;
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
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class PhasePlanCreator implements SupportDefinedExecutorPlanCreator<CDPhase> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public CreateExecutionPlanResponse createPlan(CDPhase phase, CreateExecutionPlanContext context) {
    final ExecutionPlanCreator<ExecutionSection> planCreatorForStep =
        getPlanCreatorForStep(context, phase.getSteps().get(0));
    final CreateExecutionPlanResponse planForStep = planCreatorForStep.createPlan(phase.getSteps().get(0), context);
    final PlanNode executionPhasesNode = preparePhaseNode(phase, context, planForStep);
    return CreateExecutionPlanResponse.builder()
        .planNode(executionPhasesNode)
        .planNodes(planForStep.getPlanNodes())
        .startingNodeId(executionPhasesNode.getUuid())
        .build();
  }

  private ExecutionPlanCreator<ExecutionSection> getPlanCreatorForStep(
      CreateExecutionPlanContext context, ExecutionSection step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("no execution plan creator found for step [%s]", step));
  }

  private PlanNode preparePhaseNode(
      CDPhase phase, CreateExecutionPlanContext context, CreateExecutionPlanResponse planForStep) {
    final String nodeId = generateUuid();

    final String phaseIdentifier = phase.getIdentifier();
    return PlanNode.builder()
        .uuid(nodeId)
        .name(phaseIdentifier)
        .identifier(phaseIdentifier)
        .stepType(SectionStep.STEP_TYPE)
        .stepParameters(SectionStepParameters.builder().childNodeId(planForStep.getStartingNodeId()).build())
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.CHILD).build()).build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType()) && searchContext.getObjectToPlan() instanceof CDPhase;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(CDPlanCreatorType.PHASE_PLAN_CREATOR.getName());
  }
}
