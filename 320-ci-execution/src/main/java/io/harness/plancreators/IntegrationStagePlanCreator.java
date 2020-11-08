package io.harness.plancreators;

import static io.harness.common.CIExecutionConstants.CI_PIPELINE_CONFIG;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.CIPlanCreatorType.EXECUTION_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGE_PLAN_CREATOR;
import static io.harness.states.IntegrationStageStep.CHILD_PLAN_START_NODE;
import static software.wings.common.CICommonPodConstants.POD_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.advisers.fail.OnFailAdviserParameters;
import io.harness.advisers.success.OnSuccessAdviserParameters;
import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
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
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.plan.PlanNode;
import io.harness.states.BuildStatusStepNodeCreator;
import io.harness.states.IntegrationStageStep;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.extended.ci.codebase.impl.GitHubCodeBase;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class IntegrationStagePlanCreator implements SupportDefinedExecutorPlanCreator<IntegrationStage> {
  public static final String GROUP_NAME = "INTEGRATION_STAGE";
  public static final String FAILED_STATUS = "failure";
  public static final String SUCCESS_STATUS = "success";

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

    BuildNumber buildNumber = ciExecutionArgs.getBuildNumber();

    ExecutionElement execution = integrationStage.getExecution();
    ExecutionElement modifiedExecutionPlan =
        ciLiteEngineIntegrationStageModifier.modifyExecutionPlan(execution, integrationStage, context);

    integrationStage.setExecution(modifiedExecutionPlan);
    if (ciExecutionArgs.getExecutionSource() != null
        && ciExecutionArgs.getExecutionSource().getType() == ExecutionSource.Type.WEBHOOK) {
      String sha = retrieveLastCommitSha((WebhookExecutionSource) ciExecutionArgs.getExecutionSource());

      String connectorRef = retrieveConnectorRef(context);
      return createExecutionPlanForWebhookExecution(
          integrationStage, modifiedExecutionPlan, context, buildNumber, podName, sha, connectorRef);
    } else {
      return createExecutionPlanForManualExecution(
          integrationStage, modifiedExecutionPlan, context, buildNumber, podName);
    }
  }

  private String retrieveConnectorRef(ExecutionPlanCreationContext context) {
    NgPipeline ngPipeline =
        (NgPipeline) context.getAttribute(CI_PIPELINE_CONFIG)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 "Execution arguments are empty for pipeline execution " + context.getAccountId()));

    return ((GitHubCodeBase) ngPipeline.getCiCodebase().getCodeBaseSpec()).getConnectorRef();
  }

  private String retrieveLastCommitSha(WebhookExecutionSource webhookExecutionSource) {
    if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.PR) {
      PRWebhookEvent prWebhookEvent = (PRWebhookEvent) webhookExecutionSource.getWebhookEvent();
      return prWebhookEvent.getBaseAttributes().getAfter();
    } else if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.BRANCH) {
      BranchWebhookEvent branchWebhookEvent = (BranchWebhookEvent) webhookExecutionSource.getWebhookEvent();
      return branchWebhookEvent.getBaseAttributes().getAfter();
    }

    log.error("Non supported event type, status will be empty");
    return "";
  }

  private ExecutionPlanCreatorResponse createExecutionPlanForManualExecution(IntegrationStage integrationStage,
      ExecutionElement modifiedExecutionPlan, ExecutionPlanCreationContext context, BuildNumber buildNumber,
      String podName) {
    final ExecutionPlanCreatorResponse planForExecution = createPlanForExecution(modifiedExecutionPlan, context);

    ArrayList<AdviserObtainment> adviserObtainments = new ArrayList<>();
    final PlanNode integrationStageNode =
        prepareIntegrationStageNode(integrationStage, planForExecution, podName, buildNumber, adviserObtainments);

    return ExecutionPlanCreatorResponse.builder()
        .planNode(integrationStageNode)
        .planNodes(planForExecution.getPlanNodes())
        .startingNodeId(integrationStageNode.getUuid())
        .build();
  }

  private ExecutionPlanCreatorResponse createExecutionPlanForWebhookExecution(IntegrationStage integrationStage,
      ExecutionElement modifiedExecutionPlan, ExecutionPlanCreationContext context, BuildNumber buildNumber,
      String podName, String sha, String connectorRef) {
    final ExecutionPlanCreatorResponse planForExecution = createPlanForExecution(modifiedExecutionPlan, context);
    PlanNode buildStatusFailedStepNode = BuildStatusStepNodeCreator.prepareBuildStatusStepNode(
        FAILED_STATUS, "Integration stage failed", sha, integrationStage.getIdentifier(), connectorRef);

    PlanNode buildStatusSucceededStepNode = BuildStatusStepNodeCreator.prepareBuildStatusStepNode(
        SUCCESS_STATUS, "Integration stage succeeded", sha, integrationStage.getIdentifier(), connectorRef);

    ArrayList<AdviserObtainment> adviserObtainments = new ArrayList<>();

    adviserObtainments.add(
        AdviserObtainment.builder()
            .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
            .parameters(OnSuccessAdviserParameters.builder().nextNodeId(buildStatusSucceededStepNode.getUuid()).build())
            .build());

    adviserObtainments.add(
        AdviserObtainment.builder()
            .type(AdviserType.builder().type(AdviserType.ON_FAIL).build())
            .parameters(OnFailAdviserParameters.builder().nextNodeId(buildStatusFailedStepNode.getUuid()).build())
            .build());

    final PlanNode integrationStageNode =
        prepareIntegrationStageNode(integrationStage, planForExecution, podName, buildNumber, adviserObtainments);

    // TODO Harsh Check whether we also have to add pending status
    return ExecutionPlanCreatorResponse.builder()
        .planNode(integrationStageNode)
        .planNode(buildStatusFailedStepNode)
        .planNode(buildStatusSucceededStepNode)
        .planNodes(planForExecution.getPlanNodes())
        .startingNodeId(integrationStageNode.getUuid())
        .build();
  }

  private ExecutionPlanCreatorResponse createPlanForExecution(
      ExecutionElement execution, ExecutionPlanCreationContext context) {
    final ExecutionPlanCreator<ExecutionElement> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(
            EXECUTION_PLAN_CREATOR.getName(), execution, context, "no execution plan creator found for execution");

    return executionPlanCreator.createPlan(execution, context);
  }

  private PlanNode prepareIntegrationStageNode(IntegrationStage integrationStage,
      ExecutionPlanCreatorResponse planForExecution, String podName, BuildNumber buildNumber,
      ArrayList<AdviserObtainment> adviserObtainments) {
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
        .adviserObtainments(adviserObtainments)
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
