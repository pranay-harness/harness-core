package io.harness.plancreators;

import static io.harness.common.CICommonPodConstants.POD_NAME_PREFIX;
import static io.harness.common.CIExecutionConstants.CI_PIPELINE_CONFIG;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.CIPlanCreatorType.EXECUTION_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGE_PLAN_CREATOR;
import static io.harness.states.IntegrationStageStep.CHILD_PLAN_START_NODE;

import static java.lang.Character.toLowerCase;
import static org.apache.commons.lang3.CharUtils.isAsciiAlphanumeric;

import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.executionargs.ExecutionArgs;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.integrationstage.CILiteEngineIntegrationStageModifier;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.serializer.KryoSerializer;
import io.harness.states.IntegrationStageStep;
import io.harness.yaml.core.ExecutionElement;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntegrationStagePlanCreator implements SupportDefinedExecutorPlanCreator<IntegrationStage> {
  public static final String GROUP_NAME = "INTEGRATION_STAGE";
  public static final String FAILED_STATUS = "failure";
  public static final String SUCCESS_STATUS = "success";
  static final String SOURCE = "123456789bcdfghjklmnpqrstvwxyz";
  static final Integer RANDOM_LENGTH = 8;
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;
  @Inject private KryoSerializer kryoSerializer;
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

    log.info("Started plan creation for integration stage from execution source: {}", integrationStage.getIdentifier(),
        ciExecutionArgs.getExecutionSource().getType());
    BuildNumberDetails buildNumber = ciExecutionArgs.getBuildNumberDetails();

    ExecutionElement execution = integrationStage.getExecution();
    ExecutionElement modifiedExecutionPlan =
        ciLiteEngineIntegrationStageModifier.modifyExecutionPlan(execution, integrationStage, context, podName);

    integrationStage.setExecution(modifiedExecutionPlan);
    if (ciExecutionArgs.getExecutionSource() != null
        && ciExecutionArgs.getExecutionSource().getType() == ExecutionSource.Type.WEBHOOK) {
      String sha = retrieveLastCommitSha((WebhookExecutionSource) ciExecutionArgs.getExecutionSource());

      String connectorRef = retrieveConnectorRef(context);
      return createExecutionPlanForWebhookExecution(
          integrationStage, modifiedExecutionPlan, context, buildNumber, sha, connectorRef);
    } else {
      return createExecutionPlanForManualExecution(integrationStage, modifiedExecutionPlan, context, buildNumber);
    }
  }

  private String retrieveConnectorRef(ExecutionPlanCreationContext context) {
    NgPipeline ngPipeline =
        (NgPipeline) context.getAttribute(CI_PIPELINE_CONFIG)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 "Execution arguments are empty for pipeline execution " + context.getAccountId()));

    return ngPipeline.getCiCodebase().getConnectorRef();
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
      ExecutionElement modifiedExecutionPlan, ExecutionPlanCreationContext context,
      BuildNumberDetails buildNumberDetails) {
    final ExecutionPlanCreatorResponse planForExecution = createPlanForExecution(modifiedExecutionPlan, context);

    ArrayList<AdviserObtainment> adviserObtainments = new ArrayList<>();
    // No need to send status to git in case of manual execution
    final PlanNode integrationStageNode =
        prepareIntegrationStageNode(integrationStage, planForExecution, buildNumberDetails, adviserObtainments, null);

    return ExecutionPlanCreatorResponse.builder()
        .planNode(integrationStageNode)
        .planNodes(planForExecution.getPlanNodes())
        .startingNodeId(integrationStageNode.getUuid())
        .build();
  }

  private ExecutionPlanCreatorResponse createExecutionPlanForWebhookExecution(IntegrationStage integrationStage,
      ExecutionElement modifiedExecutionPlan, ExecutionPlanCreationContext context,
      BuildNumberDetails buildNumberDetails, String sha, String connectorRef) {
    BuildStatusUpdateParameter buildStatusUpdateParameter = BuildStatusUpdateParameter.builder()
                                                                .sha(sha)
                                                                .connectorIdentifier(connectorRef)
                                                                .identifier(integrationStage.getIdentifier())
                                                                .build();
    final ExecutionPlanCreatorResponse planForExecution = createPlanForExecution(modifiedExecutionPlan, context);

    ArrayList<AdviserObtainment> adviserObtainments = new ArrayList<>();

    final PlanNode integrationStageNode = prepareIntegrationStageNode(
        integrationStage, planForExecution, buildNumberDetails, adviserObtainments, buildStatusUpdateParameter);

    // TODO Harsh Check whether we also have to add pending status
    return ExecutionPlanCreatorResponse.builder()
        .planNode(integrationStageNode)
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
      ExecutionPlanCreatorResponse planForExecution, BuildNumberDetails buildNumberDetails,
      ArrayList<AdviserObtainment> adviserObtainments, BuildStatusUpdateParameter buildStatusUpdateParameter) {
    final String deploymentStageUid = generateUuid();

    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name(integrationStage.getIdentifier())
        .identifier(integrationStage.getIdentifier())
        .stepType(IntegrationStageStep.STEP_TYPE)
        .group(GROUP_NAME)
        .stepParameters(
            IntegrationStageStepParameters.builder()
                .buildNumberDetails(buildNumberDetails)
                .integrationStageIdentifier(integrationStage.getIdentifier())
                .buildStatusUpdateParameter(buildStatusUpdateParameter)
                .integrationStage(integrationStage)
                .fieldToExecutionNodeIdMap(ImmutableMap.of(CHILD_PLAN_START_NODE, planForExecution.getStartingNodeId()))
                .build())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
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
    return POD_NAME_PREFIX + "-" + getK8PodIdentifier(integrationStage.getIdentifier()) + "-"
        + generateRandomAlphaNumbericString(RANDOM_LENGTH);
  }

  public String getK8PodIdentifier(String identifier) {
    StringBuilder sb = new StringBuilder(15);
    for (char c : identifier.toCharArray()) {
      if (isAsciiAlphanumeric(c)) {
        sb.append(toLowerCase(c));
      }
      if (sb.length() == 15) {
        return sb.toString();
      }
    }
    return sb.toString();
  }

  public static String generateRandomAlphaNumbericString(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(SOURCE.charAt(random.nextInt(SOURCE.length())));
    }
    return sb.toString();
  }
}
