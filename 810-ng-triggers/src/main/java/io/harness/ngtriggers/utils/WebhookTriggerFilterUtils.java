package io.harness.ngtriggers.utils;

import static io.harness.beans.WebhookEvent.Type.BRANCH;
import static io.harness.constants.Constants.BITBUCKET_CLOUD_HEADER_KEY;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.BT_PULL_REQUEST_CREATED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.BT_PULL_REQUEST_UPDATED;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HeaderConfig;
import io.harness.exception.TriggerException;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.conditionchecker.ConditionEvaluator;
import io.harness.ngtriggers.expressions.TriggerExpressionEvaluator;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
@Slf4j
public class WebhookTriggerFilterUtils {
  public boolean evaluateEventAndActionFilters(
      WebhookPayloadData webhookPayloadData, WebhookTriggerSpec webhookTriggerConfigSpec) {
    return checkIfEventTypeMatches(webhookPayloadData.getWebhookEvent().getType(), webhookTriggerConfigSpec.getEvent())
        && checkIfActionMatches(webhookPayloadData, webhookTriggerConfigSpec);
  }

  public boolean checkIfEventTypeMatches(
      io.harness.beans.WebhookEvent.Type eventTypeFromPayload, WebhookEvent eventTypeFromTrigger) {
    if (eventTypeFromPayload.equals(io.harness.beans.WebhookEvent.Type.PR)) {
      return eventTypeFromTrigger.equals(WebhookEvent.PULL_REQUEST)
          || eventTypeFromTrigger.equals(WebhookEvent.MERGE_REQUEST);
    }

    if (eventTypeFromPayload.equals(BRANCH)) {
      return eventTypeFromTrigger.equals(WebhookEvent.PUSH);
    }

    if (eventTypeFromPayload.equals(io.harness.beans.WebhookEvent.Type.ISSUE_COMMENT)) {
      return eventTypeFromTrigger.equals(WebhookEvent.ISSUE_COMMENT);
    }
    return false;
  }

  public boolean checkIfActionMatches(
      WebhookPayloadData webhookPayloadData, WebhookTriggerSpec webhookTriggerConfigSpec) {
    List<WebhookAction> actions = webhookTriggerConfigSpec.getActions();
    // No filter means any actions is valid for trigger invocation
    if (isEmpty(actions)) {
      return true;
    }

    Set<String> parsedActionValueSet = actions.stream().map(action -> action.getParsedValue()).collect(toSet());
    if (actions.contains(BT_PULL_REQUEST_UPDATED)) {
      specialHandlingForBBSPullReqUpdate(webhookPayloadData, actions, parsedActionValueSet);
    }

    String eventActionReceived = webhookPayloadData.getWebhookEvent().getBaseAttributes().getAction();
    return parsedActionValueSet.contains(eventActionReceived);
  }

  // SCM returns "sync" for pr:open for BitbucketCloud and "open" for BitbucketServer.
  // So, For BT_PULL_REQUEST_UPDATED, we have associated "sync" as parsedValue,
  // So, here are adding "open" in case, it was bitbucker server payload
  private static void specialHandlingForBBSPullReqUpdate(
      WebhookPayloadData webhookPayloadData, List<WebhookAction> actions, Set<String> parsedActionValueSet) {
    Set<String> headerKeys = webhookPayloadData.getOriginalEvent()
                                 .getHeaders()
                                 .stream()
                                 .map(headerConfig -> headerConfig.getKey())
                                 .collect(toSet());

    if (!headerKeys.contains(BITBUCKET_CLOUD_HEADER_KEY)
        && !headerKeys.contains(BITBUCKET_CLOUD_HEADER_KEY.toLowerCase())
        && !headerKeys.stream().anyMatch(BITBUCKET_CLOUD_HEADER_KEY::equalsIgnoreCase)) {
      parsedActionValueSet.add(BT_PULL_REQUEST_CREATED.getParsedValue());
    }
  }

  public boolean checkIfPayloadConditionsMatch(
      WebhookPayloadData webhookPayloadData, List<WebhookCondition> payloadConditions) {
    if (isEmpty(payloadConditions)) {
      return true;
    }

    String input;
    String standard;
    String operator;
    TriggerExpressionEvaluator triggerExpressionEvaluator = null;
    boolean allConditionsMatched = true;
    for (WebhookCondition webhookCondition : payloadConditions) {
      standard = webhookCondition.getValue();
      operator = webhookCondition.getOperator();

      if (webhookCondition.getKey().equals("sourceBranch")) {
        input = webhookPayloadData.getWebhookEvent().getBaseAttributes().getSource();
        if (isBlank(input)) {
          // Skipping for push event type, because it doesn't have a source branch
          continue;
        }
      } else if (webhookCondition.getKey().equals("targetBranch")) {
        input = webhookPayloadData.getWebhookEvent().getBaseAttributes().getTarget();
      } else {
        if (triggerExpressionEvaluator == null) {
          triggerExpressionEvaluator = generatorPMSExpressionEvaluator(webhookPayloadData);
        }
        input = readFromPayload(webhookCondition.getKey(), triggerExpressionEvaluator);
      }

      allConditionsMatched = allConditionsMatched && ConditionEvaluator.evaluate(input, standard, operator);
      if (!allConditionsMatched) {
        break;
      }
    }

    return allConditionsMatched;
  }

  public boolean checkIfJexlConditionsMatch(
      ParseWebhookResponse parseWebhookResponse, List<HeaderConfig> headers, String payload, String jexlExpression) {
    if (isBlank(jexlExpression)) {
      return true;
    }

    TriggerExpressionEvaluator triggerExpressionEvaluator =
        generatorPMSExpressionEvaluator(parseWebhookResponse, headers, payload);
    Object result = triggerExpressionEvaluator.evaluateExpression(jexlExpression);
    if (result != null && Boolean.class.isAssignableFrom(result.getClass())) {
      return (Boolean) result;
    }

    StringBuilder errorMsg = new StringBuilder(128);
    if (result == null) {
      errorMsg.append("Expression ")
          .append(jexlExpression)
          .append(" was evaluated to null. Expected type is Boolean")
          .toString();
    } else {
      errorMsg.append("Expression ")
          .append(jexlExpression)
          .append(":  was evaluated to type: ")
          .append(result.getClass())
          .append(". Expected type is Boolean")
          .toString();
    }

    throw new TriggerException(errorMsg.toString(), USER);
  }

  public boolean checkIfCustomPayloadConditionsMatch(String payload, WebhookTriggerSpec triggerSpec) {
    if (triggerSpec == null || isEmpty(triggerSpec.getPayloadConditions())) {
      return true;
    }

    String input;
    String standard;
    String operator;
    boolean allConditionsMatched = true;
    TriggerExpressionEvaluator triggerExpressionEvaluator = generatorPMSExpressionEvaluator(null, emptyList(), payload);

    for (WebhookCondition webhookCondition : triggerSpec.getPayloadConditions()) {
      standard = webhookCondition.getValue();
      operator = webhookCondition.getOperator();
      input = readFromPayload(webhookCondition.getKey(), triggerExpressionEvaluator);
      allConditionsMatched = allConditionsMatched && ConditionEvaluator.evaluate(input, standard, operator);
      if (!allConditionsMatched) {
        break;
      }
    }

    return allConditionsMatched;
  }

  public boolean checkIfCustomHeaderConditionsMatch(List<HeaderConfig> headers, WebhookTriggerSpec triggerSpec) {
    if (triggerSpec == null || isEmpty(triggerSpec.getHeaderConditions())) {
      return true;
    }

    for (WebhookCondition webhookHeaderCondition : triggerSpec.getHeaderConditions()) {
      HeaderConfig header = headers.stream()
                                .filter(headerConfig -> headerConfig.getKey().equals(webhookHeaderCondition.getKey()))
                                .findAny()
                                .orElse(null);

      if (header != null) {
        for (String value : header.getValues()) {
          if (!ConditionEvaluator.evaluate(
                  value, webhookHeaderCondition.getValue(), webhookHeaderCondition.getOperator())) {
            return false;
          }
        }
      } else {
        if (!webhookHeaderCondition.getOperator().contains("not")) {
          return false;
        }
      }
    }
    return true;
  }

  @VisibleForTesting
  String readFromPayload(String key, TriggerExpressionEvaluator triggerExpressionEvaluator) {
    return triggerExpressionEvaluator.renderExpression(key);
  }

  TriggerExpressionEvaluator generatorPMSExpressionEvaluator(WebhookPayloadData webhookPayloadData) {
    return generatorPMSExpressionEvaluator(webhookPayloadData.getParseWebhookResponse(),
        webhookPayloadData.getOriginalEvent().getHeaders(), webhookPayloadData.getOriginalEvent().getPayload());
  }

  TriggerExpressionEvaluator generatorPMSExpressionEvaluator(
      ParseWebhookResponse parseWebhookResponse, List<HeaderConfig> headerConfigs, String payload) {
    return new TriggerExpressionEvaluator(parseWebhookResponse, headerConfigs, payload);
  }
}
