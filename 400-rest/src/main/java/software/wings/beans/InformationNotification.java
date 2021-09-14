/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.event.model.EventType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 7/27/16.
 */
@JsonTypeName("INFORMATION")
@Data
@EqualsAndHashCode(callSuper = true)
public class InformationNotification extends Notification {
  private String displayText;

  @Builder
  public InformationNotification(String accountId, String appId, String environmentId, String entityId,
      EntityType entityType, EventType eventType, String displayText, String notificationTemplateId,
      Map<String, String> notificationTemplateVariables) {
    super(NotificationType.INFORMATION);
    setAccountId(accountId);
    setAppId(appId);
    setEnvironmentId(environmentId);
    setEntityId(entityId);
    setEntityType(entityType);
    setEventType(eventType);
    setDisplayText(displayText);
    setNotificationTemplateId(notificationTemplateId);
    setNotificationTemplateVariables(notificationTemplateVariables);
  }
}
