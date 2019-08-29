package software.wings.service.impl.trigger;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.expression.ExpressionEvaluator.getName;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.trigger.Condition.Type.WEBHOOK;

import com.google.gson.Gson;
import com.google.inject.Inject;

import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.logging.ExceptionLogger;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.Variable;
import software.wings.beans.WebHookToken;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PayloadSource.Type;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.TriggerArtifactSelectionLastCollected;
import software.wings.beans.trigger.TriggerArtifactSelectionLastDeployed;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.trigger.TriggerArtifactSelectionWebhook;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.utils.CryptoUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class WebhookConditionTriggerProcessor implements TriggerProcessor {
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;
  @Inject private transient TriggerArtifactVariableHandler triggerArtifactVariableHandler;
  @Inject private transient TriggerDeploymentExecution triggerDeploymentExecution;
  @Inject private ManagerExpressionEvaluator expressionEvaluator;

  @Override
  public void validateTriggerConditionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    WebhookCondition webhookCondition = (WebhookCondition) deploymentTrigger.getCondition();
    validatePayloadSource(webhookCondition);
    updateWebhookToken(deploymentTrigger, existingTrigger);
  }

  @Override
  public void validateTriggerActionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    triggerServiceHelper.validateTriggerAction(deploymentTrigger);
  }

  @Override
  public void transformTriggerConditionRead(DeploymentTrigger deploymentTrigger) {
    // No need to update anything for webhook trigger
  }

  @Override
  public void transformTriggerActionRead(DeploymentTrigger deploymentTrigger) {
    triggerServiceHelper.reBuildTriggerActionWithNames(deploymentTrigger);
  }

  @Override
  public WorkflowExecution executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams) {
    WebhookTriggerExecutionParams webhookTriggerExecutionParams =
        (WebhookTriggerExecutionParams) triggerExecutionParams;

    List<ArtifactVariable> artifactVariables = triggerArtifactVariableHandler.fetchArtifactVariablesForExecution(
        webhookTriggerExecutionParams.trigger.getAppId(), webhookTriggerExecutionParams.trigger, null);

    try {
      return triggerDeploymentExecution.triggerDeployment(
          artifactVariables, webhookTriggerExecutionParams.parameters, webhookTriggerExecutionParams.trigger, null);
    } catch (WingsException exception) {
      exception.addContext(Application.class, appId);
      exception.addContext(DeploymentTrigger.class, webhookTriggerExecutionParams.trigger.getUuid());
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      return null;
    }
  }

  private void updateWebhookToken(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    WebhookCondition webhookCondition = (WebhookCondition) deploymentTrigger.getCondition();
    WebHookToken webHookToken = generateWebhookToken(getExistingWebhookToken(existingTrigger), deploymentTrigger);
    deploymentTrigger.setCondition(WebhookCondition.builder()
                                       .webHookToken(webHookToken)
                                       .payloadSource(webhookCondition.getPayloadSource())
                                       .build());
    deploymentTrigger.setWebHookToken(webHookToken.getWebHookToken());
  }

  private void validatePayloadSource(WebhookCondition webhookCondition) {
    Type type = webhookCondition.getPayloadSource().getType();
    if (!((type.equals(Type.BITBUCKET) || type.equals(Type.CUSTOM) || type.equals(Type.GITHUB)
            || type.equals(Type.GITLAB)))) {
      throw new TriggerException("Invalid Payload type", null);
    }
  }

  private WebHookToken generateWebhookToken(WebHookToken existingToken, DeploymentTrigger deploymentTrigger) {
    WebHookToken webHookToken;
    if (existingToken == null || existingToken.getWebHookToken() == null) {
      webHookToken =
          WebHookToken.builder().httpMethod("POST").webHookToken(CryptoUtils.secureRandAlphaNumString(40)).build();

      addExpressionsAsParameters(deploymentTrigger, webHookToken);
    } else {
      webHookToken = existingToken;
      addExpressionsAsParameters(deploymentTrigger, webHookToken);
    }

    return webHookToken;
  }

  private void addExpressionsAsParameters(DeploymentTrigger deploymentTrigger, WebHookToken webHookToken) {
    Map<String, Object> parameters = new HashMap<>();

    if (deploymentTrigger.getAction() != null) {
      switch (deploymentTrigger.getAction().getActionType()) {
        case PIPELINE:
          PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
          if (pipelineAction.getTriggerArgs().getVariables() != null) {
            updateWFVariables(pipelineAction.getTriggerArgs().getVariables(), parameters);
          }

          if (pipelineAction.getTriggerArgs() != null
              && pipelineAction.getTriggerArgs().getTriggerArtifactVariables() != null) {
            pipelineAction.getTriggerArgs().getTriggerArtifactVariables().forEach(artifactVariable -> {
              addArtifactVariablesexpression(artifactVariable.getVariableValue(), parameters);
            });
          }

          break;
        case ORCHESTRATION:
          WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
          if (workflowAction.getTriggerArgs().getVariables() != null) {
            updateWFVariables(workflowAction.getTriggerArgs().getVariables(), parameters);
          }

          if (workflowAction.getTriggerArgs() != null
              && workflowAction.getTriggerArgs().getTriggerArtifactVariables() != null) {
            workflowAction.getTriggerArgs().getTriggerArtifactVariables().forEach(artifactVariable -> {
              addArtifactVariablesexpression(artifactVariable.getVariableValue(), parameters);
            });
          }
          break;
        default:
          unhandled(deploymentTrigger.getAction().getActionType());
      }
    }

    webHookToken.setPayload(new Gson().toJson(parameters));
  }

  private void updateWFVariables(List<Variable> variables, Map<String, Object> parameters) {
    if (isNotEmpty(variables)) {
      variables.stream().filter(variableEntry -> isNotEmpty(variableEntry.getValue())).forEach(variable -> {
        String wfVariableValue = variable.getValue();
        if (matchesVariablePattern(wfVariableValue)) {
          parameters.put(getNameFromExpression(variable.getValue()), variable.getValue());
        }
      });
    }
  }

  private String getNameFromExpression(String expression) {
    return getName(expression);
  }

  private void addArtifactVariablesexpression(
      TriggerArtifactSelectionValue triggerArtifactVariableValue, Map<String, Object> parameters) {
    switch (triggerArtifactVariableValue.getArtifactSelectionType()) {
      case LAST_COLLECTED:
        TriggerArtifactSelectionLastCollected lastCollected =
            (TriggerArtifactSelectionLastCollected) triggerArtifactVariableValue;

        if (matchesVariablePattern(lastCollected.getArtifactStreamId())) {
          parameters.put(
              getNameFromExpression(lastCollected.getArtifactStreamId()), lastCollected.getArtifactStreamId());
        }

        if (matchesVariablePattern(lastCollected.getArtifactFilter())) {
          parameters.put(getNameFromExpression(lastCollected.getArtifactStreamId()), lastCollected.getArtifactFilter());
        }

        if (matchesVariablePattern(lastCollected.getArtifactServerId())) {
          parameters.put(
              getNameFromExpression(lastCollected.getArtifactStreamId()), lastCollected.getArtifactServerId());
        }
        break;
      case LAST_DEPLOYED:
        TriggerArtifactSelectionLastDeployed lastDeployed =
            (TriggerArtifactSelectionLastDeployed) triggerArtifactVariableValue;

        if (ExpressionEvaluator.matchesVariablePattern(lastDeployed.getWorkflowId())) {
          parameters.put(getNameFromExpression(lastDeployed.getWorkflowId()), lastDeployed.getWorkflowId());
        }
        break;
      case WEBHOOK_VARIABLE:
        TriggerArtifactSelectionWebhook webhook = (TriggerArtifactSelectionWebhook) triggerArtifactVariableValue;

        if (matchesVariablePattern(webhook.getArtifactStreamId())) {
          parameters.put(getNameFromExpression(webhook.getArtifactStreamId()), webhook.getArtifactStreamId());
        }

        if (matchesVariablePattern(webhook.getBuildNumber())) {
          parameters.put(getNameFromExpression(webhook.getBuildNumber()), webhook.getBuildNumber());
        }

        if (matchesVariablePattern(webhook.getArtifactServerId())) {
          parameters.put(getNameFromExpression(webhook.getArtifactServerId()), webhook.getArtifactServerId());
        }
        break;
      default:
        unhandled(triggerArtifactVariableValue.getArtifactSelectionType());
    }
  }

  private WebHookToken getExistingWebhookToken(DeploymentTrigger existingTrigger) {
    WebHookToken existingWebhookToken = null;
    if (existingTrigger != null && existingTrigger.getType().equals(WEBHOOK)) {
      WebhookCondition existingTriggerCondition = (WebhookCondition) existingTrigger.getCondition();
      existingWebhookToken = existingTriggerCondition.getWebHookToken();
    }
    return existingWebhookToken;
  }

  @Value
  @Builder
  public static class WebhookTriggerExecutionParams implements TriggerExecutionParams {
    DeploymentTrigger trigger;
    Map<String, String> parameters;
  }
}
