package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.EXECUTION_PHASES_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.PHASE_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.executionplan.CDPlanNodeType;
import io.harness.cdng.pipeline.CDPhase;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.StepGroup;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ExecutionPhasesPlanCreator
    extends AbstractPlanCreatorWithChildren<List<CDPhase>> implements SupportDefinedExecutorPlanCreator<List<CDPhase>> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public Map<String, List<CreateExecutionPlanResponse>> createPlanForChildren(
      List<CDPhase> phases, CreateExecutionPlanContext context) {
    Map<String, List<CreateExecutionPlanResponse>> childrenPlanMap = new HashMap<>();
    final List<CreateExecutionPlanResponse> planForPhases = getPlanForPhases(context, phases);
    childrenPlanMap.put("PHASES", planForPhases);
    return childrenPlanMap;
  }

  @Override
  public CreateExecutionPlanResponse createPlanForSelf(List<CDPhase> input,
      Map<String, List<CreateExecutionPlanResponse>> planForChildrenMap, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> planForPhases = planForChildrenMap.get("PHASES");
    final PlanNode executionPlanNode = prepareExecutionPhasesNode(context, planForPhases);
    return CreateExecutionPlanResponse.builder()
        .planNode(executionPlanNode)
        .planNodes(getPlanNodes(planForPhases))
        .startingNodeId(executionPlanNode.getUuid())
        .build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<CreateExecutionPlanResponse> planForSteps) {
    return planForSteps.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<CreateExecutionPlanResponse> getPlanForPhases(CreateExecutionPlanContext context, List<CDPhase> phases) {
    return phases.stream()
        .map(phase -> getPlanCreatorForPhase(context, phase).createPlan(phase, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<CDPhase> getPlanCreatorForPhase(CreateExecutionPlanContext context, CDPhase phase) {
    return planCreatorHelper.getExecutionPlanCreator(
        PHASE_PLAN_CREATOR.getName(), phase, context, format("no execution plan creator found for phase [%s]", phase));
  }

  private PlanNode prepareExecutionPhasesNode(
      CreateExecutionPlanContext context, List<CreateExecutionPlanResponse> planForPhases) {
    final String nodeId = generateUuid();

    final String EXECUTION = "EXECUTION";
    return PlanNode.builder()
        .uuid(nodeId)
        .name(EXECUTION)
        .identifier(EXECUTION)
        .stepType(SectionChainStep.STEP_TYPE)
        .group(StepGroup.PHASES.name())
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeIds(planForPhases.stream()
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
    final Object objectToPlan = searchContext.getObjectToPlan();
    return getSupportedTypes().contains(searchContext.getType())
        && objectToPlan instanceof List<?> && ((List<?>) objectToPlan).stream().allMatch(o -> o instanceof CDPhase);
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(EXECUTION_PHASES_PLAN_CREATOR.getName());
  }

  @Override
  public String getPlanNodeType(List<CDPhase> input) {
    return CDPlanNodeType.EXECUTION.name();
  }
}
