package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.model.EventType;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.Map;

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
