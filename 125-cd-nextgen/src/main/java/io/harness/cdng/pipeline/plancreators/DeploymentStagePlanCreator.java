package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.CD_EXECUTION_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.INFRA_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.ROLLBACK_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.SERVICE_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGE_PLAN_CREATOR;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.advisers.fail.OnFailAdviserParameters;
import io.harness.cdng.executionplan.utils.PlanCreatorConfigUtils;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceConfig;
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
import io.harness.steps.section.chain.SectionChainStep;
import io.harness.steps.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.ExecutionElement;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class DeploymentStagePlanCreator extends AbstractPlanCreatorWithChildren<DeploymentStage>
    implements SupportDefinedExecutorPlanCreator<DeploymentStage> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  public Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      DeploymentStage deploymentStage, ExecutionPlanCreationContext context) {
    Map<String, List<ExecutionPlanCreatorResponse>> childrenPlanMap = new HashMap<>();
    final ExecutionPlanCreatorResponse planForService = createPlanForService(deploymentStage.getService(), context);
    final ExecutionPlanCreatorResponse planForInfrastructure =
        createPlanForInfrastructure(deploymentStage.getInfrastructure(), context);
    final ExecutionPlanCreatorResponse planForExecution =
        createPlanForExecution(deploymentStage.getExecution(), context);
    childrenPlanMap.put("SERVICE", singletonList(planForService));
    childrenPlanMap.put("INFRA", singletonList(planForInfrastructure));
    childrenPlanMap.put("EXECUTION", singletonList(planForExecution));
    return childrenPlanMap;
  }

  @Override
  public ExecutionPlanCreatorResponse createPlanForSelf(DeploymentStage deploymentStage,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    ExecutionPlanCreatorResponse planForService = planForChildrenMap.get("SERVICE").get(0);
    ExecutionPlanCreatorResponse planForInfrastructure = planForChildrenMap.get("INFRA").get(0);
    ExecutionPlanCreatorResponse planForExecution = planForChildrenMap.get("EXECUTION").get(0);

    ExecutionPlanCreatorResponse rollbackExecutionPlan = createPlanForRollbackNode(deploymentStage, context);

    final PlanNode deploymentStageNode = prepareDeploymentNode(
        deploymentStage, planForExecution, planForService, planForInfrastructure, rollbackExecutionPlan);

    return ExecutionPlanCreatorResponse.builder()
        .planNode(deploymentStageNode)
        .planNodes(planForService.getPlanNodes())
        .planNodes(planForInfrastructure.getPlanNodes())
        .planNodes(planForExecution.getPlanNodes())
        .planNodes(rollbackExecutionPlan.getPlanNodes())
        .startingNodeId(deploymentStageNode.getUuid())
        .build();
  }

  private ExecutionPlanCreatorResponse createPlanForRollbackNode(
      DeploymentStage deploymentStage, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<DeploymentStage> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(
            ROLLBACK_PLAN_CREATOR.getName(), deploymentStage, context, "No execution plan creator found for Rollback");
    return executionPlanCreator.createPlan(deploymentStage, context);
  }

  private ExecutionPlanCreatorResponse createPlanForInfrastructure(
      PipelineInfrastructure pipelineInfrastructure, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<PipelineInfrastructure> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(INFRA_PLAN_CREATOR.getName(), pipelineInfrastructure,
            context, "No execution plan creator found for Infra Execution.");
    return executionPlanCreator.createPlan(pipelineInfrastructure, context);
  }

  private PlanNode prepareDeploymentNode(DeploymentStage deploymentStage, ExecutionPlanCreatorResponse planForExecution,
      ExecutionPlanCreatorResponse planForService, ExecutionPlanCreatorResponse planForInfrastructure,
      ExecutionPlanCreatorResponse rollbackExecutionPlan) {
    final String deploymentStageUid = generateUuid();

    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name(deploymentStage.getName())
        .identifier(deploymentStage.getIdentifier())
        .stepType(SectionChainStep.STEP_TYPE)
        .group(StepOutcomeGroup.STAGE.name())
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeId(planForService.getStartingNodeId())
                            .childNodeId(planForInfrastructure.getStartingNodeId())
                            .childNodeId(planForExecution.getStartingNodeId())
                            .build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.CHILD_CHAIN).build())
                                   .build())
        .adviserObtainment(
            AdviserObtainment.builder()
                .type(AdviserType.builder().type(AdviserType.ON_FAIL).build())
                .parameters(
                    OnFailAdviserParameters.builder().nextNodeId(rollbackExecutionPlan.getStartingNodeId()).build())
                .build())
        .build();
  }

  private ExecutionPlanCreatorResponse createPlanForService(
      ServiceConfig serviceConfig, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<ServiceConfig> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(SERVICE_PLAN_CREATOR.getName(), serviceConfig, context,
            "No execution plan creator found for Service Execution.");
    return executionPlanCreator.createPlan(serviceConfig, context);
  }

  private ExecutionPlanCreatorResponse createPlanForExecution(
      ExecutionElement execution, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<ExecutionElement> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(CD_EXECUTION_PLAN_CREATOR.getName(), execution, context,
            "no execution plan creator found for stage execution");

    return executionPlanCreator.createPlan(execution, context);
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof DeploymentStage;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STAGE_PLAN_CREATOR.getName());
  }

  @Override
  public void prePlanCreation(DeploymentStage deploymentStage, ExecutionPlanCreationContext context) {
    super.prePlanCreation(deploymentStage, context);
    PlanCreatorConfigUtils.setCurrentStageConfig(deploymentStage, context);
  }

  @Override
  public void postPlanCreation(DeploymentStage deploymentStage, ExecutionPlanCreationContext context) {
    super.postPlanCreation(deploymentStage, context);
    PlanCreatorConfigUtils.setCurrentStageConfig(null, context);
  }

  @Override
  public String getPlanNodeType(DeploymentStage input) {
    return PlanNodeType.STAGE.name();
  }
}
