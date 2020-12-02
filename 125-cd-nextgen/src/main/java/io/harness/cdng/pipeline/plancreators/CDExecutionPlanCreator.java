package io.harness.cdng.pipeline.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;

import static java.lang.String.format;

import io.harness.cdng.executionplan.CDPlanCreatorType;
import io.harness.cdng.pipeline.steps.NGSectionStep;
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
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
public class CDExecutionPlanCreator extends AbstractPlanCreatorWithChildren<ExecutionElement>
    implements SupportDefinedExecutorPlanCreator<ExecutionElement> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      ExecutionElement execution, ExecutionPlanCreationContext context) {
    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();
    final List<ExecutionPlanCreatorResponse> planForSteps = getPlanForSteps(context, execution.getSteps());
    childrenPlanMap.put("STEPS", planForSteps);
    return childrenPlanMap;
  }

  @Override
  public ExecutionPlanCreatorResponse createPlanForSelf(ExecutionElement execution,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> planForSteps = planForChildrenMap.get("STEPS");
    final PlanNode executionNode = prepareExecutionNode(planForSteps);

    ExecutionPlanCreatorResponseImplBuilder planResponseImplBuilder = ExecutionPlanCreatorResponse.builder()
                                                                          .planNode(executionNode)
                                                                          .planNodes(getPlanNodes(planForSteps))
                                                                          .startingNodeId(executionNode.getUuid());

    return planResponseImplBuilder.build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<ExecutionPlanCreatorResponse> planForSteps) {
    return planForSteps.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<ExecutionPlanCreatorResponse> getPlanForSteps(
      ExecutionPlanCreationContext context, List<ExecutionWrapper> executionSections) {
    return executionSections.stream()
        .map(step -> getPlanCreatorForStep(context, step).createPlan(step, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<ExecutionWrapper> getPlanCreatorForStep(
      ExecutionPlanCreationContext context, ExecutionWrapper step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("no execution plan creator found for step [%s]", step));
  }

  private PlanNode prepareExecutionNode(List<ExecutionPlanCreatorResponse> planForSteps) {
    final String nodeId = generateUuid();
    return PlanNode.builder()
        .uuid(nodeId)
        .name(PlanCreatorConstants.EXECUTION_NODE_NAME)
        .identifier(EXECUTION_NODE_IDENTIFIER)
        .stepType(NGSectionStep.STEP_TYPE)
        .group(StepOutcomeGroup.EXECUTION.name())
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeIds(planForSteps.stream()
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
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof ExecutionElement;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(CDPlanCreatorType.CD_EXECUTION_PLAN_CREATOR.getName());
  }

  @Override
  public String getPlanNodeType(ExecutionElement input) {
    return PlanNodeType.EXECUTION.name();
  }
}
