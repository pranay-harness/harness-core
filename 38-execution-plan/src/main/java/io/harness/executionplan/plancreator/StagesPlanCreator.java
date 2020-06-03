package io.harness.executionplan.plancreator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanCreatorType;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.intfc.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class StagesPlanCreator implements SupportDefinedExecutorPlanCreator<List<Stage>> {
  @Inject private ExecutionPlanCreatorHelper planCreatorHelper;

  @Override
  public CreateExecutionPlanResponse createPlan(List<Stage> stagesList, CreateExecutionPlanContext context) {
    final List<CreateExecutionPlanResponse> planForStages = getPlanForStages(context, stagesList);
    final PlanNode stagesPlanNode = prepareStagesNode(context, planForStages);
    return CreateExecutionPlanResponse.builder()
        .planNode(stagesPlanNode)
        .planNodes(getPlanNodes(planForStages))
        .startingNodeId(stagesPlanNode.getUuid())
        .build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<CreateExecutionPlanResponse> planForSteps) {
    return planForSteps.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<CreateExecutionPlanResponse> getPlanForStages(CreateExecutionPlanContext context, List<Stage> stages) {
    return stages.stream()
        .map(stage -> getPlanCreatorForStage(context, stage).createPlan(stage, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<Stage> getPlanCreatorForStage(CreateExecutionPlanContext context, Stage stage) {
    return planCreatorHelper.getExecutionPlanCreator(
        PlanCreatorType.STAGE_PLAN_CREATOR.getName(), stage, context, "no execution plan creator found for  stage");
  }

  private PlanNode prepareStagesNode(
      CreateExecutionPlanContext context, List<CreateExecutionPlanResponse> planForStages) {
    final String nodeId = generateUuid();

    final String STAGES = "STAGES";

    return PlanNode.builder()
        .uuid(nodeId)
        .name(STAGES)
        .identifier(STAGES)
        .stepType(SectionChainStep.STEP_TYPE)
        .skipExpressionChain(true)
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeIds(planForStages.stream()
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
        && objectToPlan instanceof List<?> && ((List<?>) objectToPlan).stream().allMatch(o -> o instanceof Stage);
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(PlanCreatorType.STAGES_PLAN_CREATOR.getName());
  }
}
