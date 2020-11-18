package io.harness.ngtriggers.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.annotations.VisibleForTesting;

import io.harness.expression.JsonFunctor;
import io.harness.ngtriggers.beans.scm.WebhookBaseAttributes;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookPayloadCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.conditionchecker.ConditionEvaluator;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@UtilityClass
@Slf4j
public class WebhookTriggerFilterUtil {
  public boolean evaluateFilterConditions(
      WebhookPayloadData webhookPayloadData, WebhookTriggerSpec webhookTriggerConfigSpec) {
    return checkIfEventTypeMatches(webhookPayloadData.getWebhookEvent().getType(), webhookTriggerConfigSpec.getEvent())
        && checkIfActionMatches(webhookPayloadData, webhookTriggerConfigSpec)
        && checkIfPayloadConditionsMatch(webhookPayloadData, webhookTriggerConfigSpec.getPayloadConditions());
  }

  public boolean checkIfEventTypeMatches(
      io.harness.ngtriggers.beans.scm.WebhookEvent.Type eventTypeFromPayload, WebhookEvent eventTypeFromTrigger) {
    if (eventTypeFromPayload.equals(io.harness.ngtriggers.beans.scm.WebhookEvent.Type.PR)) {
      return eventTypeFromTrigger.equals(WebhookEvent.PULL_REQUEST)
          || eventTypeFromTrigger.equals(WebhookEvent.MERGE_REQUEST);
    }

    if (eventTypeFromPayload.equals(io.harness.ngtriggers.beans.scm.WebhookEvent.Type.BRANCH)) {
      return eventTypeFromTrigger.equals(WebhookEvent.PUSH);
    }

    return false;
  }

  public boolean checkIfActionMatches(
      WebhookPayloadData webhookPayloadData, WebhookTriggerSpec webhookTriggerConfigSpec) {
    WebhookBaseAttributes baseAttributes = webhookPayloadData.getWebhookEvent().getBaseAttributes();

    List<WebhookAction> actions = webhookTriggerConfigSpec.getActions();

    // No filter means any actions is valid for trigger invocation
    if (isEmpty(actions)) {
      return true;
    }

    String eventActionReceived = webhookPayloadData.getWebhookEvent().getBaseAttributes().getAction();
    Optional<WebhookAction> optionalWebhookAction =
        webhookTriggerConfigSpec.getActions()
            .stream()
            .filter(action -> action.name().equalsIgnoreCase(eventActionReceived))
            .findAny();

    return optionalWebhookAction.isPresent();
  }

  public boolean checkIfPayloadConditionsMatch(
      WebhookPayloadData webhookPayloadData, List<WebhookPayloadCondition> payloadConditions) {
    if (isEmpty(payloadConditions)) {
      return true;
    }

    String input;
    String standard;
    String operator;
    boolean allConditionsMatched = true;
    for (WebhookPayloadCondition webhookPayloadCondition : payloadConditions) {
      standard = webhookPayloadCondition.getValue();
      operator = webhookPayloadCondition.getOperator();

      if (webhookPayloadCondition.getKey().equals("sourceBranch")) {
        input = webhookPayloadData.getWebhookEvent().getBaseAttributes().getSource();
      } else if (webhookPayloadCondition.getKey().equals("targetBranch")) {
        input = webhookPayloadData.getWebhookEvent().getBaseAttributes().getTarget();
      } else {
        input = readFromPayload(webhookPayloadCondition.getKey(), webhookPayloadData.getOriginalEvent().getPayload());
      }

      allConditionsMatched = allConditionsMatched && ConditionEvaluator.evaluate(input, standard, operator);
      if (!allConditionsMatched) {
        break;
      }
    }

    return allConditionsMatched;
  }

  @VisibleForTesting
  String readFromPayload(String key, String payload) {
    JsonFunctor jsonFunctor = new JsonFunctor();
    Object value = jsonFunctor.select(key, payload);
    if (value instanceof String) {
      return (String) value;
    }

    return null;
  }
}
