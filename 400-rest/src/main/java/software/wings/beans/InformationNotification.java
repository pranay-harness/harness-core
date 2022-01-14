package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
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
@TargetModule(HarnessModule._957_CG_BEANS)
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
