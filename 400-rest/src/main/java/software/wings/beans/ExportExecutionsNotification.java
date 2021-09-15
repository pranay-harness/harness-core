/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.model.EventType;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@EqualsAndHashCode(callSuper = true)
public class ExportExecutionsNotification extends Notification {
  @Builder
  public ExportExecutionsNotification(String accountId, String appId, String entityId, EntityType entityType,
      EventType eventType, String notificationTemplateId, Map<String, String> notificationTemplateVariables) {
    super(NotificationType.INFORMATION);
    setAccountId(accountId);
    setAppId(appId);
    setEntityId(entityId);
    setEntityType(entityType);
    setEventType(eventType);
    setNotificationTemplateId(notificationTemplateId);
    setNotificationTemplateVariables(notificationTemplateVariables);
  }
}
