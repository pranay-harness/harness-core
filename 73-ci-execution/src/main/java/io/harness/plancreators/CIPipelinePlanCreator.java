package io.harness.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.PIPELINE_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGES_PLAN_CREATOR;
import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.CIPipeline;
import io.harness.beans.CIPipelineSetupParameters;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.states.CIPipelineSetupStep;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * CI pipeline plan creator
 * CI Pipeline does not require separate step, it is creating plan for stages directly and returning it
 */
@Singleton
@Slf4j
public class CIPipelinePlanCreator implements SupportDefinedExecutorPlanCreator<CIPipeline> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;
  @Override
  public ExecutionPlanCreatorResponse createPlan(CIPipeline ciPipeline, ExecutionPlanCreationContext context) {
    addArgumentsToContext(ciPipeline, context);
    final ExecutionPlanCreatorResponse planForStages = createPlanForStages(ciPipeline.getStages(), context);

    final PlanNode pipelineExecutionNode = preparePipelineNode(ciPipeline, planForStages);

    return ExecutionPlanCreatorResponse.builder()
        .planNode(pipelineExecutionNode)
        .planNodes(planForStages.getPlanNodes())
        .startingNodeId(pipelineExecutionNode.getUuid())
        .build();
  }

  private void addArgumentsToContext(CIPipeline pipeline, ExecutionPlanCreationContext context) {
    context.addAttribute("CI_PIPELINE_CONFIG", pipeline);
  }

  private PlanNode preparePipelineNode(CIPipeline pipeline, ExecutionPlanCreatorResponse planForStages) {
    final String pipelineSetupNodeId = generateUuid();

    return PlanNode.builder()
        .uuid(pipelineSetupNodeId)
        .name(pipeline.getName())
        .identifier(pipeline.getIdentifier())
        .stepType(CIPipelineSetupStep.STEP_TYPE)
        .stepParameters(CIPipelineSetupParameters.builder()
                            .ciPipeline(pipeline)
                            .fieldToExecutionNodeIdMap(ImmutableMap.of("stages", planForStages.getStartingNodeId()))
                            .build())
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.CHILD).build()).build())
        .build();
  }

  private ExecutionPlanCreatorResponse createPlanForStages(
      List<? extends StageElementWrapper> stages, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<List<? extends StageElementWrapper>> stagesPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(
            STAGES_PLAN_CREATOR.getName(), stages, context, "no execution plan creator found for ci pipeline stages");

    return stagesPlanCreator.createPlan(stages, context);
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof CIPipeline;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(PIPELINE_PLAN_CREATOR.getName());
  }
}
