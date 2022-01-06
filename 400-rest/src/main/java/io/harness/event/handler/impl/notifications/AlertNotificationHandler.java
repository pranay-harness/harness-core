/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.handler.impl.notifications;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.event.model.EventType.CLOSE_ALERT;
import static io.harness.event.model.EventType.OPEN_ALERT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.notifications.AlertNotificationRuleChecker;

import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.alert.AlertType;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.service.impl.event.AlertEvent;
import software.wings.service.intfc.AlertNotificationRuleService;
import software.wings.service.intfc.NotificationDispatcherService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;

@OwnedBy(PL)
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AlertNotificationHandler implements EventHandler {
  private static final Pair<Boolean, Alert> INVALID_EVENT = new Pair<>(false, null);

  @Inject private NotificationDispatcherService notificationDispatcher;
  @Inject private AlertNotificationRuleChecker ruleChecker;
  @Inject private AlertNotificationRuleService ruleService;

  public AlertNotificationHandler(EventListener eventListener) {
    eventListener.registerEventHandler(this, Sets.newHashSet(OPEN_ALERT, CLOSE_ALERT));
  }

  private static Map<AlertType, NotificationMessageType> alertTypeNotificationMessageTypeMap = new HashMap<>();
  static {
    alertTypeNotificationMessageTypeMap.put(
        AlertType.DelegatesDown, NotificationMessageType.DELEGATE_DOWN_ALERT_NOTIFICATION);
    alertTypeNotificationMessageTypeMap.put(
        AlertType.CONTINUOUS_VERIFICATION_ALERT, NotificationMessageType.CV_SERVICE_GUARD_NOTIFICATION);
  }

  @Override
  public void handleEvent(Event event) {
    Pair<Boolean, Alert> validEvent = isValidEvent(event);
    if (!validEvent.getKey()) {
      return;
    }

    handleAlert(validEvent.getValue(), event.getEventType());
  }

  private void handleAlert(Alert alert, EventType alertEventType) {
    String accountId = alert.getAccountId();

    log.info("Got an alert event. alertType={} , category={} , accountId={}", alert.getType(), alert.getCategory(),
        accountId);

    List<AlertNotificationRule> allRules = ruleService.getAll(accountId);

    List<AlertNotificationRule> defaultRules = new LinkedList<>();
    List<AlertNotificationRule> nonDefaultRules = new LinkedList<>();

    for (AlertNotificationRule rule : allRules) {
      if (rule.isDefault()) {
        defaultRules.add(rule);
      } else {
        nonDefaultRules.add(rule);
      }
    }

    if (defaultRules.size() != 1) {
      log.error(
          "Every account should have one default rule. accountId={} defaultRules={}", accountId, defaultRules.size());
    }

    boolean notificationSent = false;
    for (AlertNotificationRule rule : nonDefaultRules) {
      if (ruleChecker.doesAlertSatisfyRule(alert, rule)) {
        notificationSent = sendNotification(accountId, alert, rule, alertEventType);
      } else {
        log.debug("Alert did not satisfy rule. accountId={} rule={} alert={}", accountId, rule, alert);
      }
    }

    if (!notificationSent && CollectionUtils.isNotEmpty(defaultRules)) {
      sendNotification(accountId, alert, defaultRules.get(0), alertEventType);
    }
  }

  private boolean sendNotification(
      String accountId, Alert alert, AlertNotificationRule rule, EventType alertEventType) {
    try {
      NotificationMessageType messageType = alertTypeNotificationMessageTypeMap.getOrDefault(
          alert.getType(), NotificationMessageType.GENERIC_ALERT_NOTIFICATION);

      Notification notification =
          InformationNotification.builder()
              .accountId(accountId)
              .notificationTemplateId(messageType.name())
              .notificationTemplateVariables(ImmutableMap.of(
                  "alert_message", CLOSE_ALERT == alertEventType ? alert.getResolutionTitle() : alert.getTitle()))
              .displayText(CLOSE_ALERT == alertEventType ? alert.getResolutionTitle() : alert.getTitle())
              .eventType(alertEventType)
              .build();

      notificationDispatcher.dispatch(notification, Collections.singletonList(rule));
      return true;
    } catch (Exception e) {
      log.error("Error dispatching notification. accountId={} Alert: {} Rule: {}", accountId, alert, rule, e);
      return false;
    }
  }

  /**
   * Checks if the event is valid.
   * @param event
   * @return a pair in which the key tells whether the event is valid or now, and the value is the required event info.
   */
  private Pair<Boolean, Alert> isValidEvent(Event event) {
    EventType eventType = event.getEventType();

    EventData eventData = event.getEventData();
    if (null == eventData || null == eventData.getEventInfo()) {
      log.error("Expected some alert event data. Check with the publisher to see why data was not sent. eventType={}",
          eventType);
      return INVALID_EVENT;
    }

    if (eventData.getEventInfo() instanceof AlertEvent) {
      AlertEvent alertEvent = (AlertEvent) eventData.getEventInfo();
      Alert alert = alertEvent.getAlert();
      String accountId = alert.getAccountId();

      if (StringUtils.isEmpty(accountId)) {
        log.error("Account ID not present in alert. Can't send notification for it. alertType={}, category={}",
            alert.getType(), alert.getCategory());
        return INVALID_EVENT;
      } else {
        return new Pair<>(true, alert);
      }
    } else {
      log.error("Invalid event. Could not cast. eventType={}, class={}", eventType,
          eventData.getEventInfo().getClass().getCanonicalName());
      return INVALID_EVENT;
    }
  }
}
