package io.harness.cdng.pipeline.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.PIPELINE_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGES_PLAN_CREATOR;
import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.executionplan.utils.PlanCreatorConfigUtils;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.steps.PipelineSetupStep;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class PipelinePlanCreator
    extends AbstractPlanCreatorWithChildren<CDPipeline> implements SupportDefinedExecutorPlanCreator<CDPipeline> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  public Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      CDPipeline cdPipeline, ExecutionPlanCreationContext context) {
    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();
    ExecutionPlanCreatorResponse planForStages = createPlanForStages(cdPipeline.getStages(), context);
    childrenPlanMap.put("STAGES", singletonList(planForStages));
    return childrenPlanMap;
  }

  @Override
  public ExecutionPlanCreatorResponse createPlanForSelf(CDPipeline cdPipeline,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    ExecutionPlanCreatorResponse planForStages = planForChildrenMap.get("STAGES").get(0);
    final PlanNode pipelineExecutionNode = preparePipelineNode(cdPipeline, planForStages);
    return ExecutionPlanCreatorResponse.builder()
        .planNode(pipelineExecutionNode)
        .planNodes(planForStages.getPlanNodes())
        .startingNodeId(pipelineExecutionNode.getUuid())
        .build();
  }

  private ExecutionPlanCreatorResponse createPlanForStages(
      List<? extends StageElementWrapper> stages, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<List<? extends StageElementWrapper>> stagesPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(
            STAGES_PLAN_CREATOR.getName(), stages, context, "no execution plan creator found for pipeline stages");

    return stagesPlanCreator.createPlan(stages, context);
  }

  private PlanNode preparePipelineNode(CDPipeline pipeline, ExecutionPlanCreatorResponse planForStages) {
    final String pipelineSetupNodeId = generateUuid();

    return PlanNode.builder()
        .uuid(pipelineSetupNodeId)
        .name(pipeline.getName())
        .identifier(pipeline.getIdentifier())
        .stepType(PipelineSetupStep.STEP_TYPE)
        .group(StepOutcomeGroup.PIPELINE.name())
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

  @Override
  public String getPlanNodeType(CDPipeline cdPipeline) {
    return PlanNodeType.PIPELINE.name();
  }

  @Override
  public void prePlanCreation(CDPipeline cdPipeline, ExecutionPlanCreationContext context) {
    super.prePlanCreation(cdPipeline, context);
    PlanCreatorConfigUtils.setPipelineConfig(cdPipeline, context);
  }
}
