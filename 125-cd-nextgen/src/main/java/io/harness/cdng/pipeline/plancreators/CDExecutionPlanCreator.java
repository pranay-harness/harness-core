package io.harness.cdng.pipeline.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.executionplan.CDPlanCreatorType;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.core.impl.CreateExecutionPlanResponseImpl.CreateExecutionPlanResponseImplBuilder;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class CDExecutionPlanCreator extends AbstractPlanCreatorWithChildren<ExecutionElement>
    implements SupportDefinedExecutorPlanCreator<ExecutionElement> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public Map<String, List<CreateExecutionPlanResponse>> createPlanForChildren(
      ExecutionElement execution, CreateExecutionPlanContext context) {
    Map<String, List<CreateExecutionPlanResponse>> childrenPlanMap = new HashMap<>();
    final List<CreateExecutionPlanResponse> planForSteps = getPlanForSteps(context, execution.getSteps());
    childrenPlanMap.put("STEPS", planForSteps);
    return childrenPlanMap;
  }

  @Override
  public CreateExecutionPlanResponse createPlanForSelf(ExecutionElement execution,
      Map<String, List<CreateExecutionPlanResponse>> planForChildrenMap, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> planForSteps = planForChildrenMap.get("STEPS");
    final PlanNode executionNode = prepareExecutionNode(planForSteps);

    CreateExecutionPlanResponseImplBuilder planResponseImplBuilder = CreateExecutionPlanResponse.builder()
                                                                         .planNode(executionNode)
                                                                         .planNodes(getPlanNodes(planForSteps))
                                                                         .startingNodeId(executionNode.getUuid());

    return planResponseImplBuilder.build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<CreateExecutionPlanResponse> planForSteps) {
    return planForSteps.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<CreateExecutionPlanResponse> getPlanForSteps(
      CreateExecutionPlanContext context, List<ExecutionWrapper> executionSections) {
    return executionSections.stream()
        .map(step -> getPlanCreatorForStep(context, step).createPlan(step, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<ExecutionWrapper> getPlanCreatorForStep(
      CreateExecutionPlanContext context, ExecutionWrapper step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("no execution plan creator found for step [%s]", step));
  }

  private PlanNode prepareExecutionNode(List<CreateExecutionPlanResponse> planForSteps) {
    final String nodeId = generateUuid();
    return PlanNode.builder()
        .uuid(nodeId)
        .name(EXECUTION_NODE_IDENTIFIER)
        .identifier(EXECUTION_NODE_IDENTIFIER)
        .stepType(SectionChainStep.STEP_TYPE)
        .group(StepOutcomeGroup.EXECUTION.name())
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
