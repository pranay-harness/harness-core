/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.service.senders;

import io.harness.notification.beans.NotificationProcessingResponse;

import com.github.dikhan.pagerduty.client.events.PagerDutyEventsClient;
import com.github.dikhan.pagerduty.client.events.domain.EventResult;
import com.github.dikhan.pagerduty.client.events.domain.LinkContext;
import com.github.dikhan.pagerduty.client.events.domain.Payload;
import com.github.dikhan.pagerduty.client.events.domain.TriggerIncident;
import com.github.dikhan.pagerduty.client.events.domain.TriggerIncident.TriggerIncidentBuilder;
import com.github.dikhan.pagerduty.client.events.exceptions.NotifyEventException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PagerDutySenderImpl {
  public NotificationProcessingResponse send(
      List<String> pagerDutyKeys, Payload payload, List<LinkContext> links, String notificationId) {
    List<Boolean> results = new ArrayList<>();
    PagerDutyEventsClient pagerDutyEventsClient = PagerDutyEventsClient.create();
    for (String pagerDutyKey : pagerDutyKeys) {
      TriggerIncident incident = TriggerIncidentBuilder.newBuilder(pagerDutyKey, payload).setLinks(links).build();
      try {
        EventResult result = pagerDutyEventsClient.trigger(incident);
        results.add(result != null && result.getErrors() == null);
      } catch (NotifyEventException e) {
        log.error("Unable to send PagerDuty incident for notification reference id {}", notificationId);
      }
    }
    return NotificationProcessingResponse.builder().result(results).build();
  }
}
