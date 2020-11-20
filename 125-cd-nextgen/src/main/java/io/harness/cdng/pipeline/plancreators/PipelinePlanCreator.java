package io.harness.cdng.pipeline.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.PIPELINE_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGES_PLAN_CREATOR;
import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.executionplan.utils.PlanCreatorConfigUtils;
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
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.plan.PlanNode;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
@Slf4j
public class PipelinePlanCreator
    extends AbstractPlanCreatorWithChildren<NgPipeline> implements SupportDefinedExecutorPlanCreator<NgPipeline> {
  public static String INPUT_SET_YAML_KEY = "InputSetYaml";
  public static String EVENT_PAYLOAD_KEY = "eventPayload";

  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  public Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      NgPipeline ngPipeline, ExecutionPlanCreationContext context) {
    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();
    ExecutionPlanCreatorResponse planForStages = createPlanForStages(ngPipeline.getStages(), context);
    childrenPlanMap.put("STAGES", singletonList(planForStages));
    return childrenPlanMap;
  }

  @Override
  public ExecutionPlanCreatorResponse createPlanForSelf(NgPipeline ngPipeline,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    ExecutionPlanCreatorResponse planForStages = planForChildrenMap.get("STAGES").get(0);
    Optional<String> inputSetPipelineYaml = context.getAttribute(INPUT_SET_YAML_KEY);
    final PlanNode pipelineExecutionNode =
        preparePipelineNode(ngPipeline, planForStages, inputSetPipelineYaml.orElse(null));
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

  private PlanNode preparePipelineNode(
      NgPipeline pipeline, ExecutionPlanCreatorResponse planForStages, String inputSetPipelineYaml) {
    final String pipelineSetupNodeId = generateUuid();

    return PlanNode.builder()
        .uuid(pipelineSetupNodeId)
        .name(pipeline.getName())
        .identifier(pipeline.getIdentifier())
        .stepType(PipelineSetupStep.STEP_TYPE)
        .group(StepOutcomeGroup.PIPELINE.name())
        .skipExpressionChain(true)
        .stepParameters(CDPipelineSetupParameters.builder()
                            .ngPipeline(pipeline)
                            .inputSetPipelineYaml(inputSetPipelineYaml)
                            .fieldToExecutionNodeIdMap(ImmutableMap.of("stages", planForStages.getStartingNodeId()))
                            .build())

        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof NgPipeline;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(PIPELINE_PLAN_CREATOR.getName());
  }

  @Override
  public String getPlanNodeType(NgPipeline ngPipeline) {
    return PlanNodeType.PIPELINE.name();
  }

  @Override
  public void prePlanCreation(NgPipeline ngPipeline, ExecutionPlanCreationContext context) {
    super.prePlanCreation(ngPipeline, context);
    PlanCreatorConfigUtils.setPipelineConfig(ngPipeline, context);
  }
}
