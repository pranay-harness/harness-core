package io.harness.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.CIPlanCreatorType.EXECUTION_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGE_PLAN_CREATOR;
import static io.harness.states.IntegrationStageStep.CHILD_PLAN_START_NODE;
import static software.wings.common.CICommonPodConstants.POD_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.executionargs.ExecutionArgs;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.integrationstage.CILiteEngineIntegrationStageModifier;
import io.harness.plan.PlanNode;
import io.harness.states.IntegrationStageStep;
import io.harness.yaml.core.ExecutionElement;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

public class IntegrationStagePlanCreator implements SupportDefinedExecutorPlanCreator<IntegrationStage> {
  public static final String GROUP_NAME = "INTEGRATION_STAGE";

  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;
  @Inject private CILiteEngineIntegrationStageModifier ciLiteEngineIntegrationStageModifier;
  private static final SecureRandom random = new SecureRandom();
  @Override
  public ExecutionPlanCreatorResponse createPlan(
      IntegrationStage integrationStage, ExecutionPlanCreationContext context) {
    final String podName = generatePodName(integrationStage);

    CIExecutionArgs ciExecutionArgs =
        (CIExecutionArgs) context.getAttribute(ExecutionArgs.EXEC_ARGS)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 "Execution arguments are empty for pipeline execution " + context.getAccountId()));

    String stageID = integrationStage.getIdentifier();
    BuildNumber buildNumber = ciExecutionArgs.getBuildNumber();

    ExecutionElement execution = integrationStage.getExecution();
    ExecutionElement modifiedExecutionPlan =
        ciLiteEngineIntegrationStageModifier.modifyExecutionPlan(execution, integrationStage, context);

    integrationStage.setExecution(modifiedExecutionPlan);
    final ExecutionPlanCreatorResponse planForExecution = createPlanForExecution(modifiedExecutionPlan, context);
    final PlanNode deploymentStageNode =
        prepareDeploymentNode(integrationStage, context, planForExecution, podName, buildNumber, stageID);

    return ExecutionPlanCreatorResponse.builder()
        .planNode(deploymentStageNode)
        .planNodes(planForExecution.getPlanNodes())
        .startingNodeId(deploymentStageNode.getUuid())
        .build();
  }

  private ExecutionPlanCreatorResponse createPlanForExecution(
      ExecutionElement execution, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<ExecutionElement> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(
            EXECUTION_PLAN_CREATOR.getName(), execution, context, "no execution plan creator found for execution");

    return executionPlanCreator.createPlan(execution, context);
  }

  private PlanNode prepareDeploymentNode(IntegrationStage integrationStage, ExecutionPlanCreationContext context,
      ExecutionPlanCreatorResponse planForExecution, String podName, BuildNumber buildNumber, String stageID) {
    final String deploymentStageUid = generateUuid();

    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name(integrationStage.getIdentifier())
        .identifier(integrationStage.getIdentifier())
        .stepType(IntegrationStageStep.STEP_TYPE)
        .group(GROUP_NAME)
        .stepParameters(
            IntegrationStageStepParameters.builder()
                .buildNumber(buildNumber)
                .podName(podName)
                .integrationStage(integrationStage)
                .fieldToExecutionNodeIdMap(ImmutableMap.of(CHILD_PLAN_START_NODE, planForExecution.getStartingNodeId()))
                .build())
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.CHILD).build()).build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof IntegrationStage;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STAGE_PLAN_CREATOR.getName());
  }

  private String generatePodName(IntegrationStage integrationStage) {
    // TODO Use better pod naming strategy after discussion with PM, attach build number in future
    return POD_NAME + "-" + integrationStage.getIdentifier() + random.nextInt(100000000);
  }
}
