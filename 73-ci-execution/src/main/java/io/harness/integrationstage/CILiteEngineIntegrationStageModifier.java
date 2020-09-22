package io.harness.integrationstage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.executionargs.ExecutionArgs;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.intfc.StageType;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Modifies saved integration stage execution plan by appending pre and post execution steps for setting up pod and
 * adding cleanup
 */

@Slf4j
@Singleton
public class CILiteEngineIntegrationStageModifier implements StageExecutionModifier {
  @Inject private CILiteEngineStepGroupUtils ciLiteEngineStepGroupUtils;
  private static final String EMPTY_BRANCH = "";

  @Override
  public ExecutionElement modifyExecutionPlan(
      ExecutionElement execution, StageType stageType, ExecutionPlanCreationContext context) {
    IntegrationStage integrationStage = (IntegrationStage) stageType;
    CIExecutionArgs ciExecutionArgs =
        (CIExecutionArgs) context.getAttribute(ExecutionArgs.EXEC_ARGS)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 "Execution arguments are empty for pipeline execution " + context.getAccountId()));

    String branch = getBranchNameFromExecutionSource(ciExecutionArgs);
    return getCILiteEngineTaskExecution(integrationStage, branch, context.getAccountId());
  }

  private ExecutionElement getCILiteEngineTaskExecution(
      IntegrationStage integrationStage, String inputBranch, String accountId) {
    // TODO Only git is supported currently
    String branchToBeCloned = getBranchNameFromPipeline(integrationStage);

    if (!inputBranch.equals(EMPTY_BRANCH)) {
      branchToBeCloned = inputBranch;
    }

    if (integrationStage.getGitConnector().getType().equals("git")) {
      GitConnectorYaml gitConnectorYaml = (GitConnectorYaml) integrationStage.getGitConnector();
      return ExecutionElement.builder()
          .steps(ciLiteEngineStepGroupUtils.createExecutionWrapperWithLiteEngineSteps(
              integrationStage, branchToBeCloned, gitConnectorYaml.getIdentifier(), accountId))
          .build();
    } else {
      throw new IllegalArgumentException("Input connector type is not of type git");
    }
  }

  private String getBranchNameFromExecutionSource(CIExecutionArgs ciExecutionArgs) {
    if (ciExecutionArgs != null && (ciExecutionArgs.getExecutionSource() != null)) {
      if (ciExecutionArgs.getExecutionSource().getType() == ExecutionSource.Type.Webhook) {
        WebhookExecutionSource webhookExecutionSource = (WebhookExecutionSource) ciExecutionArgs.getExecutionSource();
        if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.BRANCH) {
          BranchWebhookEvent branchWebhookEvent = (BranchWebhookEvent) webhookExecutionSource.getWebhookEvent();
          return branchWebhookEvent.getBranchName();
        }
        if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.PR) {
          PRWebhookEvent prWebhookEvent = (PRWebhookEvent) webhookExecutionSource.getWebhookEvent();
          return prWebhookEvent.getSourceBranch();
        }
      }
    }
    return EMPTY_BRANCH;
  }

  private String getBranchNameFromPipeline(IntegrationStage integrationStage) {
    Optional<CIStepInfo> stepInfo =
        integrationStage.getExecution()
            .getSteps()
            .stream()
            .filter(executionWrapper -> executionWrapper instanceof StepElement)
            .filter(executionWrapper -> ((StepElement) executionWrapper).getStepSpecType() instanceof GitCloneStepInfo)
            .findFirst()
            .map(executionWrapper -> (GitCloneStepInfo) ((StepElement) executionWrapper).getStepSpecType());
    if (stepInfo.isPresent()) {
      GitCloneStepInfo gitCloneStepInfo = (GitCloneStepInfo) stepInfo.get();
      return gitCloneStepInfo.getBranch();
    } else {
      throw new InvalidRequestException("Failed to execute pipeline, Git clone section is missing");
    }
  }
}
