package io.harness.plancreators;

import static io.harness.common.CIExecutionConstants.CI_PIPELINE_CONFIG;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.PIPELINE_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGES_PLAN_CREATOR;

import static java.util.Collections.singletonList;

import io.harness.beans.CIPipelineSetupParameters;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.executionargs.ExecutionArgs;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.integrationstage.IntegrationPipelineExecutionModifier;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.states.CIPipelineSetupStep;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * CI pipeline plan creator
 * CI Pipeline does not require separate step, it is creating plan for stages directly and returning it
 */
@Singleton
@Slf4j
public class CIPipelinePlanCreator implements SupportDefinedExecutorPlanCreator<NgPipeline> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;
  @Inject private IntegrationPipelineExecutionModifier integrationPipelineExecutionModifier;
  @Override
  public ExecutionPlanCreatorResponse createPlan(NgPipeline ngPipeline, ExecutionPlanCreationContext context) {
    addArgumentsToContext(ngPipeline, context);

    CIExecutionArgs ciExecutionArgs =
        (CIExecutionArgs) context.getAttribute(ExecutionArgs.EXEC_ARGS)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 "Execution arguments are empty for pipeline execution " + context.getAccountId()));
    NgPipeline modifiedNgPipeline = integrationPipelineExecutionModifier.modifyExecutionPlan(ngPipeline, context);

    final ExecutionPlanCreatorResponse planForStages = createPlanForStages(modifiedNgPipeline.getStages(), context);

    final PlanNode pipelineExecutionNode = preparePipelineNode(modifiedNgPipeline, planForStages, ciExecutionArgs);

    return ExecutionPlanCreatorResponse.builder()
        .planNode(pipelineExecutionNode)
        .planNodes(planForStages.getPlanNodes())
        .startingNodeId(pipelineExecutionNode.getUuid())
        .build();
  }

  private void addArgumentsToContext(NgPipeline pipeline, ExecutionPlanCreationContext context) {
    context.addAttribute(CI_PIPELINE_CONFIG, pipeline);
  }

  private PlanNode preparePipelineNode(
      NgPipeline pipeline, ExecutionPlanCreatorResponse planForStages, CIExecutionArgs ciExecutionArgs) {
    final String pipelineSetupNodeId = generateUuid();

    return PlanNode.builder()
        .uuid(pipelineSetupNodeId)
        .name(pipeline.getName())
        .identifier(pipeline.getIdentifier())
        .stepType(CIPipelineSetupStep.STEP_TYPE)
        .stepParameters(CIPipelineSetupParameters.builder()
                            .ngPipeline(pipeline)
                            .ciExecutionArgs(ciExecutionArgs)
                            .fieldToExecutionNodeIdMap(ImmutableMap.of("stages", planForStages.getStartingNodeId()))
                            .build())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
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
        && searchContext.getObjectToPlan() instanceof NgPipeline;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(PIPELINE_PLAN_CREATOR.getName());
  }
}
