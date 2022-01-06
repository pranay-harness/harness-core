/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.Notification;
import software.wings.common.NotificationMessageResolver.PagerDutyTemplate;
import software.wings.service.intfc.pagerduty.PagerDutyService;

import com.github.dikhan.pagerduty.client.events.PagerDutyEventsClient;
import com.github.dikhan.pagerduty.client.events.domain.EventResult;
import com.github.dikhan.pagerduty.client.events.domain.LinkContext;
import com.github.dikhan.pagerduty.client.events.domain.Payload;
import com.github.dikhan.pagerduty.client.events.domain.ResolveIncident.ResolveIncidentBuilder;
import com.github.dikhan.pagerduty.client.events.domain.Severity;
import com.github.dikhan.pagerduty.client.events.domain.TriggerIncident;
import com.github.dikhan.pagerduty.client.events.domain.TriggerIncident.TriggerIncidentBuilder;
import com.github.dikhan.pagerduty.client.events.exceptions.NotifyEventException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.WordUtils;
import org.json.JSONObject;

@Slf4j
public class PagerDutyServiceImpl implements PagerDutyService {
  private static final String hexRegex = "^[0-9A-Fa-f]+$";
  @Override
  public boolean validateKey(String pagerDutyKey) {
    if (pagerDutyKey.length() != 32) {
      throw new WingsException(ErrorCode.PAGERDUTY_ERROR, WingsException.USER)
          .addParam("message", "Integration Key is invalid, should be 32 digits. Length =  " + pagerDutyKey.length());
    }
    if (pagerDutyKey.matches(hexRegex)) {
      return true;
    }
    throw new WingsException(ErrorCode.PAGERDUTY_ERROR, WingsException.USER)
        .addParam("message", "Integration Key is invalid, Not a valid hexadecimal");
  }

  @Override
  public boolean validateCreateTestEvent(String pagerDutyKey) {
    PagerDutyEventsClient pagerDutyEventsClient = PagerDutyEventsClient.create();
    String errorMsg = "Failed to create test incident";

    String summary = "Test ticket to validate new key ";
    Payload payload = Payload.Builder.newBuilder()
                          .setTimestamp(OffsetDateTime.now())
                          .setSeverity(Severity.INFO)
                          .setSummary(summary)
                          .setSource("HARNESS")
                          .build();
    TriggerIncident incident = TriggerIncidentBuilder.newBuilder(pagerDutyKey, payload).build();
    try {
      EventResult result = pagerDutyEventsClient.trigger(incident);
      log.debug("Event result {} for {}", result, incident);

      if (result != null && result.getErrors() == null) {
        String dedupKey = result.getDedupKey();
        pagerDutyEventsClient.resolve(ResolveIncidentBuilder.newBuilder(pagerDutyKey, dedupKey).build());
        return true;
      } else {
        log.error(errorMsg + "result: {} ", result);
        throw new WingsException(ErrorCode.PAGERDUTY_ERROR, WingsException.USER).addParam("message", errorMsg);
      }
    } catch (NotifyEventException e) {
      throw new WingsException(ErrorCode.PAGERDUTY_ERROR, WingsException.USER, e).addParam("message", errorMsg);
    }
  }

  @Override
  public boolean sendPagerDutyEvent(
      Notification notification, String pagerDutyKey, PagerDutyTemplate pagerDutyTemplate) {
    PagerDutyEventsClient pagerDutyEventsClient = PagerDutyEventsClient.create();
    String summary = getDecoratedNotificationMessage(
        pagerDutyTemplate.getSummary(), notification.getNotificationTemplateVariables());

    List<LinkContext> links = new ArrayList<>();
    Map<String, String> customDetails =
        notification.getNotificationTemplateVariables()
            .keySet()
            .stream()
            .filter(key -> !key.equalsIgnoreCase("START_TS_SECS") && !key.equalsIgnoreCase("END_TS_SECS"))
            .collect(Collectors.toMap(key
                -> WordUtils.capitalizeFully(key.replace("_", " ")),
                key -> notification.getNotificationTemplateVariables().get(key), (a, b) -> b));

    JSONObject jsonObject = new JSONObject(customDetails);
    Payload payload = Payload.Builder.newBuilder()
                          .setTimestamp(OffsetDateTime.now())
                          .setSeverity(Severity.ERROR)
                          .setCustomDetails(jsonObject)
                          .setSummary(summary)
                          .setSource("HARNESS")
                          .build();

    if (pagerDutyTemplate.getLink() != null) {
      String linkHref = getDecoratedNotificationMessage(
          pagerDutyTemplate.getLink().getHref(), notification.getNotificationTemplateVariables());
      String linkText = getDecoratedNotificationMessage(
          pagerDutyTemplate.getLink().getText(), notification.getNotificationTemplateVariables());
      LinkContext linkContext = new LinkContext(linkHref, linkText);
      links.add(linkContext);
    }

    TriggerIncident incident = TriggerIncidentBuilder.newBuilder(pagerDutyKey, payload).setLinks(links).build();
    try {
      EventResult result = pagerDutyEventsClient.trigger(incident);
      log.debug("Event result {} for {}", result, incident);
      if (result != null && result.getErrors() == null) {
        return true;
      } else {
        throw new WingsException(ErrorCode.PAGERDUTY_ERROR, WingsException.USER).addParam("message", result);
      }
    } catch (NotifyEventException e) {
      throw new WingsException(ErrorCode.PAGERDUTY_ERROR, WingsException.USER).addParam("message", e.getMessage());
    }
  }
}
