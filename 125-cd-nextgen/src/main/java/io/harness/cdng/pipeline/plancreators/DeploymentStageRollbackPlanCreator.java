package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.EXECUTION_ROLLBACK_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.ROLLBACK_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.STEP_GROUPS_ROLLBACK_PLAN_CREATOR;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.singletonList;

import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.data.structure.UUIDGenerator;
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
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentStageRollbackPlanCreator extends AbstractPlanCreatorWithChildren<DeploymentStage>
    implements SupportDefinedExecutorPlanCreator<DeploymentStage> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  protected String getPlanNodeType(DeploymentStage input) {
    return PlanNodeType.STAGE.name();
  }

  @Override
  protected Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      DeploymentStage deploymentStage, ExecutionPlanCreationContext context) {
    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();

    // TODO VS: Add Infra Rollback Node conditionally based on dynamic infra or not

    if (containsStepGroupsWithRollbackSteps(deploymentStage.getExecution().getSteps())) {
      context.addAttribute("stageIdentifier", deploymentStage.getIdentifier());
      ExecutionPlanCreatorResponse stepGroupRollbackPlan =
          createPlanForStepGroupsRollback(deploymentStage.getExecution(), context);
      childrenPlanMap.put("STEP_GROUP_ROLLBACK", singletonList(stepGroupRollbackPlan));
      context.removeAttribute("stageIdentifier");
    }

    if (isNotEmpty(deploymentStage.getExecution().getRollbackSteps())) {
      ExecutionPlanCreatorResponse executionRollbackPlan =
          createPlanForExecutionRollback(deploymentStage.getExecution(), context);
      childrenPlanMap.put("EXECUTION_ROLLBACK", singletonList(executionRollbackPlan));
    }
    return childrenPlanMap;
  }

  private ExecutionPlanCreatorResponse createPlanForExecutionRollback(
      ExecutionElement executionElement, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<ExecutionElement> executionRollbackPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(EXECUTION_ROLLBACK_PLAN_CREATOR.getName(), executionElement,
            context, "No execution plan creator found for Execution Rollback Plan Creator");
    return executionRollbackPlanCreator.createPlan(executionElement, context);
  }

  private ExecutionPlanCreatorResponse createPlanForStepGroupsRollback(
      ExecutionElement executionElement, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<ExecutionElement> stepGroupRollbackPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(STEP_GROUPS_ROLLBACK_PLAN_CREATOR.getName(),
            executionElement, context, "No execution plan creator found for Step Group Rollback Plan Creator");
    return stepGroupRollbackPlanCreator.createPlan(executionElement, context);
  }

  private boolean containsStepGroupsWithRollbackSteps(List<ExecutionWrapper> steps) {
    for (ExecutionWrapper step : steps) {
      if (PlanCreatorHelper.isStepGroupWithRollbacks(step)) {
        return true;
      } else if (step instanceof ParallelStepElement) {
        List<ExecutionWrapper> sections = ((ParallelStepElement) step).getSections();
        for (ExecutionWrapper section : sections) {
          if (PlanCreatorHelper.isStepGroupWithRollbacks(section)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  protected ExecutionPlanCreatorResponse createPlanForSelf(DeploymentStage deploymentStage,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    List<ExecutionPlanCreatorResponse> infraRollbackPlan = planForChildrenMap.get("INFRA_ROLLBACK");
    List<ExecutionPlanCreatorResponse> stepGroupRollbackPlan = planForChildrenMap.get("STEP_GROUP_ROLLBACK");
    List<ExecutionPlanCreatorResponse> executionRollbackPlan = planForChildrenMap.get("EXECUTION_ROLLBACK");

    RollbackOptionalChildChainStepParametersBuilder stepParametersBuilder =
        RollbackOptionalChildChainStepParameters.builder();

    ExecutionPlanCreatorResponseImplBuilder createExecutionPlanResponseImplBuilder =
        ExecutionPlanCreatorResponse.builder();

    if (infraRollbackPlan != null) {
      stepParametersBuilder.childNode(
          RollbackNode.builder()
              .nodeId(infraRollbackPlan.get(0).getStartingNodeId())
              .dependentNodeIdentifier(PlanCreatorConstants.STAGES_NODE_IDENTIFIER + deploymentStage.getIdentifier()
                  + "." + PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
              .build());
      createExecutionPlanResponseImplBuilder.planNodes(infraRollbackPlan.get(0).getPlanNodes());
    }

    String executionNodeFullIdentifier = String.join(".", PlanCreatorConstants.STAGES_NODE_IDENTIFIER,
        deploymentStage.getIdentifier(), PlanCreatorConstants.EXECUTION_NODE_IDENTIFIER);
    if (stepGroupRollbackPlan != null) {
      stepParametersBuilder.childNode(RollbackNode.builder()
                                          .nodeId(stepGroupRollbackPlan.get(0).getStartingNodeId())
                                          .dependentNodeIdentifier(executionNodeFullIdentifier)
                                          .build());
      createExecutionPlanResponseImplBuilder.planNodes(stepGroupRollbackPlan.get(0).getPlanNodes());
    }

    if (executionRollbackPlan != null) {
      stepParametersBuilder.childNode(RollbackNode.builder()
                                          .nodeId(executionRollbackPlan.get(0).getStartingNodeId())
                                          .dependentNodeIdentifier(executionNodeFullIdentifier)
                                          .build());
      createExecutionPlanResponseImplBuilder.planNodes(executionRollbackPlan.get(0).getPlanNodes());
    }

    PlanNode deploymentStageRollbackNode =
        PlanNode.builder()
            .uuid(UUIDGenerator.generateUuid())
            .name(deploymentStage.getName() + ":Rollback")
            .identifier(deploymentStage.getIdentifier() + "Rollback")
            .stepType(RollbackOptionalChildChainStep.STEP_TYPE)
            .stepParameters(stepParametersBuilder.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                    .build())
            .skipExpressionChain(true)
            .build();

    return createExecutionPlanResponseImplBuilder.startingNodeId(deploymentStageRollbackNode.getUuid())
        .planNode(deploymentStageRollbackNode)
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof DeploymentStage;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(ROLLBACK_PLAN_CREATOR.getName());
  }
}
