package io.harness.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.CIPlanCreatorType.EXECUTION_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;

import static java.lang.String.format;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.node.BasicStepToExecutionNodeConverter;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.steps.section.chain.SectionChainStep;
import io.harness.steps.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import graph.StepInfoGraphConverter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
public class ExecutionPlanCreator implements SupportDefinedExecutorPlanCreator<ExecutionElement> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;
  @Inject private StepInfoGraphConverter graphConverter;
  @Inject private BasicStepToExecutionNodeConverter basicStepToExecutionNodeConverter;

  @Override
  public ExecutionPlanCreatorResponse createPlan(ExecutionElement execution, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> planForExecutionSections =
        getPlanForExecutionSections(context, execution.getSteps());

    final PlanNode executionPlanNode = prepareExecutionNode(planForExecutionSections);

    return ExecutionPlanCreatorResponse.builder()
        .planNode(executionPlanNode)
        .planNodes(getPlanNodes(planForExecutionSections))
        .startingNodeId(executionPlanNode.getUuid())
        .build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<ExecutionPlanCreatorResponse> planForExecutionSections) {
    return planForExecutionSections.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<ExecutionPlanCreatorResponse> getPlanForExecutionSections(
      ExecutionPlanCreationContext context, List<ExecutionWrapper> executionWrappers) {
    return executionWrappers.stream()
        .map(executionWrapper -> getPlanCreatorForStep(context, executionWrapper).createPlan(executionWrapper, context))
        .collect(Collectors.toList());
  }

  private io.harness.executionplan.core.ExecutionPlanCreator<ExecutionWrapper> getPlanCreatorForStep(
      ExecutionPlanCreationContext context, ExecutionWrapper step) {
    return planCreatorHelper.getExecutionPlanCreator(
        STEP_PLAN_CREATOR.getName(), step, context, format("no execution plan creator found for step [%s]", step));
  }

  private PlanNode prepareExecutionNode(List<ExecutionPlanCreatorResponse> planForExecutionSections) {
    final String nodeId = generateUuid();

    final String EXECUTION = "EXECUTION";
    return PlanNode.builder()
        .uuid(nodeId)
        .name("Execution")
        .identifier(EXECUTION)
        .stepType(SectionChainStep.STEP_TYPE)
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeIds(planForExecutionSections.stream()
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
    return Collections.singletonList(EXECUTION_PLAN_CREATOR.getName());
  }
}
