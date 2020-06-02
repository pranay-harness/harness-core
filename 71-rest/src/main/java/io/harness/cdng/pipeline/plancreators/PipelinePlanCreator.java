package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.STAGES_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.constants.PlanCreatorType.PIPELINE_PLAN_CREATOR;
import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.steps.PipelineSetupStep;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.yaml.core.auxiliary.intfc.StageWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Singleton
@Slf4j
public class PipelinePlanCreator implements SupportDefinedExecutorPlanCreator<CDPipeline> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;
  @Override
  public CreateExecutionPlanResponse createPlan(CDPipeline cdPipeline, CreateExecutionPlanContext context) {
    addArgumentsToContext(cdPipeline, context);
    final CreateExecutionPlanResponse planForStages = createPlanForStages(cdPipeline.getStages(), context);
    final PlanNode pipelineExecutionNode = preparePipelineNode(cdPipeline, context, planForStages);

    return CreateExecutionPlanResponse.builder()
        .planNode(pipelineExecutionNode)
        .planNodes(planForStages.getPlanNodes())
        .startingNodeId(pipelineExecutionNode.getUuid())
        .build();
  }

  private void addArgumentsToContext(CDPipeline pipeline, CreateExecutionPlanContext context) {
    context.addAttribute("CD_PIPELINE_CONFIG", pipeline);
  }

  private CreateExecutionPlanResponse createPlanForStages(
      List<? extends StageWrapper> stages, CreateExecutionPlanContext context) {
    final ExecutionPlanCreator<List<? extends StageWrapper>> stagesPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(
            STAGES_PLAN_CREATOR.getName(), stages, context, "no execution plan creator found for pipeline stages");

    return stagesPlanCreator.createPlan(stages, context);
  }

  private PlanNode preparePipelineNode(
      CDPipeline pipeline, CreateExecutionPlanContext context, CreateExecutionPlanResponse planForStages) {
    final String PIPELINE_SETUP_STEP_NAME = "PIPELINE SETUP";

    final String pipelineSetupNodeId = generateUuid();

    return PlanNode.builder()
        .uuid(pipelineSetupNodeId)
        .name(PIPELINE_SETUP_STEP_NAME)
        .identifier(PIPELINE_SETUP_STEP_NAME)
        .stepType(PipelineSetupStep.STEP_TYPE)
        .skipExpressionChain(true)
        .stepParameters(CDPipelineSetupParameters.builder()
                            .cdPipeline(pipeline)
                            .fieldToExecutionNodeIdMap(ImmutableMap.of("stages", planForStages.getStartingNodeId()))
                            .build())

        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.CHILD).build()).build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof CDPipeline;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(PIPELINE_PLAN_CREATOR.getName());
  }
}
