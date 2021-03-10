package io.harness.notification.channelDetails;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.notification.channeldetails.NotificationChannel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = PmsEmailChannel.class, name = NotificationChannelType.EMAIL)
  , @JsonSubTypes.Type(value = PmsSlackChannel.class, name = NotificationChannelType.SLACK),
      @JsonSubTypes.Type(value = PmsPagerDutyChannel.class, name = NotificationChannelType.PAGERDUTY),
      @JsonSubTypes.Type(value = PmsMSTeamChannel.class, name = NotificationChannelType.MSTEAMS)
})
public abstract class PmsNotificationChannel {
  public abstract NotificationChannel toNotificationChannel(
      String accountId, String templateId, Map<String, String> templateData);
}
