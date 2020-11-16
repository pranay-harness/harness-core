package io.harness.ng.ngtriggers.helpers;

import io.harness.data.structure.EmptyPredicate;
import io.harness.ngtriggers.beans.scm.WebhookBaseAttributes;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class WebhookEventFilter {
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

  public static boolean checkIfActionMatches(
      WebhookPayloadData webhookPayloadData, WebhookTriggerSpec webhookTriggerConfigSpec) {
    WebhookBaseAttributes baseAttributes = webhookPayloadData.getWebhookEvent().getBaseAttributes();

    List<WebhookAction> actions = webhookTriggerConfigSpec.getActions();

    // No filter means any actions is valid for trigger invocation
    if (EmptyPredicate.isEmpty(actions)) {
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
}
