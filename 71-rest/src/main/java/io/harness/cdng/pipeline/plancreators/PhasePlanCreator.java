package io.harness.cdng.pipeline.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;
import static java.lang.String.format;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.executionplan.CDPlanCreatorType;
import io.harness.cdng.executionplan.utils.PlanCreatorConfigUtils;
import io.harness.cdng.pipeline.CDPhase;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.plancreator.beans.StepGroup;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class PhasePlanCreator
    extends AbstractPlanCreatorWithChildren<CDPhase> implements SupportDefinedExecutorPlanCreator<CDPhase> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public Map<String, List<CreateExecutionPlanResponse>> createPlanForChildren(
      CDPhase phase, CreateExecutionPlanContext context) {
    Map<String, List<CreateExecutionPlanResponse>> childrenPlanMap = new HashMap<>();
    final List<CreateExecutionPlanResponse> planForSteps = getPlanForSteps(context, phase.getSteps());
    final List<CreateExecutionPlanResponse> planForRollbackSteps =
        getPlanForSteps(context, emptyIfNull(phase.getRollbackSteps()));
    childrenPlanMap.put("STEPS", planForSteps);
    childrenPlanMap.put("ROLLBACK", planForRollbackSteps);
    return childrenPlanMap;
  }

  @Override
  public CreateExecutionPlanResponse createPlanForSelf(CDPhase phase,
      Map<String, List<CreateExecutionPlanResponse>> planForChildrenMap, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> planForSteps = planForChildrenMap.get("STEPS");
    List<CreateExecutionPlanResponse> planForRollbackSteps = planForChildrenMap.get("ROLLBACK");
    final PlanNode phasePlanNode = preparePhaseNode(phase, context, planForSteps, planForRollbackSteps);
    return CreateExecutionPlanResponse.builder()
        .planNode(phasePlanNode)
        .planNodes(getPlanNodes(planForSteps))
        .planNodes(getPlanNodes(planForRollbackSteps))
        .startingNodeId(phasePlanNode.getUuid())
        .build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<CreateExecutionPlanResponse> planForSteps) {
    return planForSteps.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<CreateExecutionPlanResponse> getPlanForSteps(
      CreateExecutionPlanContext context, List<ExecutionSection> executionSections) {
    return executionSections.stream()
        .map(step -> getPlanCreatorForStep(context, step).createPlan(step, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<ExecutionSection> getPlanCreatorForStep(
      CreateExecutionPlanContext context, ExecutionSection step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("no execution plan creator found for step [%s]", step));
  }

  private PlanNode preparePhaseNode(CDPhase phase, CreateExecutionPlanContext context,
      List<CreateExecutionPlanResponse> planForSteps, List<CreateExecutionPlanResponse> planForRollbackSteps) {
    final String nodeId = generateUuid();
    // TODO @rk: 02/06/20 : rollback steps wont be running as of now, they should run only if any of steps failed
    final String phaseIdentifier = phase.getIdentifier();
    return PlanNode.builder()
        .uuid(nodeId)
        .name(phase.getDisplayName())
        .identifier(phaseIdentifier)
        .stepType(SectionChainStep.STEP_TYPE)
        .group(StepGroup.PHASE.name())
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeIds(planForSteps.stream()
                                              .map(CreateExecutionPlanResponse::getStartingNodeId)
                                              .collect(Collectors.toList()))
                            .build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.CHILD_CHAIN).build())
                                   .build())
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

  @Override
  public void prePlanCreation(CDPhase phase, CreateExecutionPlanContext context) {
    super.prePlanCreation(phase, context);
    PlanCreatorConfigUtils.setCurrentPhaseConfig(phase, context);
  }

  @Override
  public void postPlanCreation(CDPhase phase, CreateExecutionPlanContext context) {
    super.postPlanCreation(phase, context);
    PlanCreatorConfigUtils.setCurrentPhaseConfig(null, context);
  }

  @Override
  public String getPlanNodeType(CDPhase input) {
    return PlanNodeType.PHASE.name();
  }
}
