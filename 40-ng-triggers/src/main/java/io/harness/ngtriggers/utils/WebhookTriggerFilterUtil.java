package io.harness.ngtriggers.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.expression.ExpressionEvaluator;
import io.harness.ngtriggers.beans.scm.WebhookBaseAttributes;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookPayloadCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.conditionchecker.ConditionEvaluator;
import io.harness.ngtriggers.functor.EventPayloadFunctor;
import io.harness.pms.ambiance.Ambiance;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

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
    Map<String, Object> context = null;
    boolean allConditionsMatched = true;
    for (WebhookPayloadCondition webhookPayloadCondition : payloadConditions) {
      standard = webhookPayloadCondition.getValue();
      operator = webhookPayloadCondition.getOperator();

      if (webhookPayloadCondition.getKey().equals("sourceBranch")) {
        input = webhookPayloadData.getWebhookEvent().getBaseAttributes().getSource();
      } else if (webhookPayloadCondition.getKey().equals("targetBranch")) {
        input = webhookPayloadData.getWebhookEvent().getBaseAttributes().getTarget();
      } else {
        if (context == null) {
          context = generateContext(webhookPayloadData.getOriginalEvent().getPayload());
        }
        input = readFromPayload(webhookPayloadCondition.getKey(), context);
      }

      allConditionsMatched = allConditionsMatched && ConditionEvaluator.evaluate(input, standard, operator);
      if (!allConditionsMatched) {
        break;
      }
    }

    return allConditionsMatched;
  }

  @VisibleForTesting
  String readFromPayload(String key, Map<String, Object> context) {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
    return expressionEvaluator.substitute(key, context);
  }

  @VisibleForTesting
  Map<String, Object> generateContext(String payload) {
    EventPayloadFunctor eventPayloadFunctor = new EventPayloadFunctor(
        Ambiance.newBuilder().putAllSetupAbstractions(Collections.singletonMap("eventPayload", payload)).build());

    Map<String, Object> context = new HashMap<>();
    context.put("eventPayload", eventPayloadFunctor);
    return context;
  }
}
