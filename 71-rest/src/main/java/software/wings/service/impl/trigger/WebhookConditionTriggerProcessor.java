package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.expression.ExpressionEvaluator.getName;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.trigger.Condition.Type.WEBHOOK;

import com.google.gson.Gson;
import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.logging.ExceptionLogger;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.Variable;
import software.wings.beans.WebHookToken;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.DeploymentTrigger;
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

@OwnedBy(CDC)
@Slf4j
public class WebhookConditionTriggerProcessor implements TriggerProcessor {
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;
  @Inject private transient TriggerArtifactVariableHandler triggerArtifactVariableHandler;
  @Inject private transient TriggerDeploymentExecution triggerDeploymentExecution;
  @Inject private ManagerExpressionEvaluator expressionEvaluator;

  private static String placeholder = "_placeholder";
  @Override
  public void validateTriggerConditionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
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
  public void transformTriggerActionRead(DeploymentTrigger deploymentTrigger, boolean readPrimaryVariablesValueNames) {
    triggerServiceHelper.reBuildTriggerActionWithNames(deploymentTrigger, readPrimaryVariablesValueNames);
  }

  @Override
  public WorkflowExecution executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams) {
    WebhookTriggerExecutionParams webhookTriggerExecutionParams =
        (WebhookTriggerExecutionParams) triggerExecutionParams;

    // Workflow variables Id's are not resolved so deferring artifact variable fetch
    try {
      return triggerDeploymentExecution.triggerDeployment(
          null, webhookTriggerExecutionParams.parameters, webhookTriggerExecutionParams.trigger, null);
    } catch (WingsException exception) {
      exception.addContext(Application.class, appId);
      exception.addContext(DeploymentTrigger.class, webhookTriggerExecutionParams.trigger.getUuid());
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      throw exception;
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

  private WebHookToken generateWebhookToken(WebHookToken existingToken, DeploymentTrigger deploymentTrigger) {
    WebHookToken webHookToken;
    if (existingToken == null || existingToken.getWebHookToken() == null) {
      webHookToken =
          WebHookToken.builder().httpMethod("POST").webHookToken(CryptoUtils.secureRandAlphaNumString(40)).build();
    } else {
      webHookToken = existingToken;
    }

    Map<String, Object> payload = new HashMap<>();
    addParametersToPayload(deploymentTrigger, payload);
    webHookToken.setPayload(new Gson().toJson(payload));
    return webHookToken;
  }

  private void addParametersToPayload(DeploymentTrigger deploymentTrigger, Map<String, Object> payload) {
    if (deploymentTrigger.getAction() != null) {
      switch (deploymentTrigger.getAction().getActionType()) {
        case PIPELINE:
          PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
          if (pipelineAction.getTriggerArgs() != null) {
            if (pipelineAction.getTriggerArgs().getVariables() != null) {
              updateWFVariables(pipelineAction.getTriggerArgs().getVariables(), payload);
            }

            if (pipelineAction.getTriggerArgs().getTriggerArtifactVariables() != null) {
              pipelineAction.getTriggerArgs().getTriggerArtifactVariables().forEach(artifactVariable -> {
                addArtifactVariablesexpression(artifactVariable.getVariableValue(), payload);
              });
            }
          }
          break;
        case WORKFLOW:
          WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
          if (workflowAction.getTriggerArgs() != null) {
            if (workflowAction.getTriggerArgs().getVariables() != null) {
              updateWFVariables(workflowAction.getTriggerArgs().getVariables(), payload);
            }

            if (workflowAction.getTriggerArgs().getTriggerArtifactVariables() != null) {
              workflowAction.getTriggerArgs().getTriggerArtifactVariables().forEach(artifactVariable -> {
                addArtifactVariablesexpression(artifactVariable.getVariableValue(), payload);
              });
            }
          }

          break;
        default:
          unhandled(deploymentTrigger.getAction().getActionType());
      }
    }
  }

  private void updateWFVariables(List<Variable> variables, Map<String, Object> parameters) {
    if (isNotEmpty(variables)) {
      variables.stream().filter(v -> isNotEmpty(v.getValue())).forEach(variable -> {
        String wfVariableValue = variable.getValue();
        if (matchesVariablePattern(wfVariableValue)) {
          String name = getNameFromExpression(variable.getValue());
          parameters.put(name, name + placeholder);
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
          String name = getNameFromExpression(lastCollected.getArtifactStreamId());
          parameters.put(name, name + placeholder);
        }

        if (matchesVariablePattern(lastCollected.getArtifactFilter())) {
          String name = getNameFromExpression(lastCollected.getArtifactFilter());
          parameters.put(name, name + placeholder);
        }

        if (matchesVariablePattern(lastCollected.getArtifactServerId())) {
          String name = getNameFromExpression(lastCollected.getArtifactServerId());
          parameters.put(name, name + placeholder);
        }
        break;
      case LAST_DEPLOYED:
        TriggerArtifactSelectionLastDeployed lastDeployed =
            (TriggerArtifactSelectionLastDeployed) triggerArtifactVariableValue;

        if (ExpressionEvaluator.matchesVariablePattern(lastDeployed.getId())) {
          String name = getNameFromExpression(lastDeployed.getId());
          parameters.put(name, name + placeholder);
        }
        break;
      case WEBHOOK_VARIABLE:
        TriggerArtifactSelectionWebhook webhook = (TriggerArtifactSelectionWebhook) triggerArtifactVariableValue;

        if (matchesVariablePattern(webhook.getArtifactStreamId())) {
          String name = getNameFromExpression(webhook.getArtifactStreamId());
          parameters.put(name, name + placeholder);
        }

        if (matchesVariablePattern(webhook.getArtifactFilter())) {
          String name = getNameFromExpression(webhook.getArtifactFilter());
          parameters.put(name, name + placeholder);
        }

        if (matchesVariablePattern(webhook.getArtifactServerId())) {
          String name = getNameFromExpression(webhook.getArtifactServerId());
          parameters.put(name, name + placeholder);
        }
        break;
      default:
        unhandled(triggerArtifactVariableValue.getArtifactSelectionType());
    }
  }

  private WebHookToken getExistingWebhookToken(DeploymentTrigger existingTrigger) {
    WebHookToken existingWebhookToken = null;
    if (existingTrigger != null && existingTrigger.getType() == WEBHOOK) {
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
