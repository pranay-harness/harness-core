package io.harness.notification.service;

import com.github.dikhan.pagerduty.client.events.PagerDutyEventsClient;
import com.github.dikhan.pagerduty.client.events.domain.EventResult;
import com.github.dikhan.pagerduty.client.events.domain.LinkContext;
import com.github.dikhan.pagerduty.client.events.domain.Payload;
import com.github.dikhan.pagerduty.client.events.domain.TriggerIncident;
import com.github.dikhan.pagerduty.client.events.domain.TriggerIncident.TriggerIncidentBuilder;
import com.github.dikhan.pagerduty.client.events.exceptions.NotifyEventException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PagerDutySenderImpl {
  public boolean send(List<String> pagerDutyKeys, Payload payload, List<LinkContext> links, String notificationId) {
    boolean sent = false;
    PagerDutyEventsClient pagerDutyEventsClient = PagerDutyEventsClient.create();
    for (String pagerDutyKey : pagerDutyKeys) {
      TriggerIncident incident = TriggerIncidentBuilder.newBuilder(pagerDutyKey, payload).setLinks(links).build();
      try {
        EventResult result = pagerDutyEventsClient.trigger(incident);
        sent = sent || (result != null && result.getErrors() == null);
      } catch (NotifyEventException e) {
        log.error("Unable to send PagerDuty incident for notification reference id {}", notificationId);
      }
    }
    return sent;
  }
}
