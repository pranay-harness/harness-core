package io.harness.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.CIPlanCreatorType.EXECUTION_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import graph.StepInfoGraphConverter;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.node.BasicStepToExecutionNodeConverter;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ExecutionPlanCreator implements SupportDefinedExecutorPlanCreator<Execution> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;
  @Inject private StepInfoGraphConverter graphConverter;
  @Inject private BasicStepToExecutionNodeConverter basicStepToExecutionNodeConverter;

  @Override
  public CreateExecutionPlanResponse createPlan(Execution execution, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> planForExecutionSections =
        getPlanForExecutionSections(context, execution.getSteps());

    final PlanNode executionPlanNode = prepareExecutionNode(planForExecutionSections);

    return CreateExecutionPlanResponse.builder()
        .planNode(executionPlanNode)
        .planNodes(getPlanNodes(planForExecutionSections))
        .startingNodeId(executionPlanNode.getUuid())
        .build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<CreateExecutionPlanResponse> planForExecutionSections) {
    return planForExecutionSections.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<CreateExecutionPlanResponse> getPlanForExecutionSections(
      CreateExecutionPlanContext context, List<ExecutionSection> executionSections) {
    return executionSections.stream()
        .map(executionSection -> getPlanCreatorForStep(context, executionSection).createPlan(executionSection, context))
        .collect(Collectors.toList());
  }

  private io.harness.executionplan.core.ExecutionPlanCreator<ExecutionSection> getPlanCreatorForStep(
      CreateExecutionPlanContext context, ExecutionSection step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("no execution plan creator found for step [%s]", step));
  }

  private PlanNode prepareExecutionNode(List<CreateExecutionPlanResponse> planForExecutionSections) {
    final String nodeId = generateUuid();

    final String EXECUTION = "EXECUTION";
    return PlanNode.builder()
        .uuid(nodeId)
        .name(EXECUTION)
        .identifier(EXECUTION)
        .stepType(SectionChainStep.STEP_TYPE)
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeIds(planForExecutionSections.stream()
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
        && searchContext.getObjectToPlan() instanceof Execution;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(EXECUTION_PLAN_CREATOR.getName());
  }
}
