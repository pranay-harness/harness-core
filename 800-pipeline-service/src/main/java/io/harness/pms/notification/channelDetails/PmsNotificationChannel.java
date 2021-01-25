package io.harness.pms.notification.channelDetails;

import io.harness.notification.channeldetails.NotificationChannel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = PmsEmailChannel.class, name = NotificationChannelType.EMAIL)
  , @JsonSubTypes.Type(value = PmsSlackChannel.class, name = NotificationChannelType.SLACK),
      @JsonSubTypes.Type(value = PmsPagerDutyChannel.class, name = NotificationChannelType.PAGERDUTY),
      @JsonSubTypes.Type(value = PmsMSTeamChannel.class, name = NotificationChannelType.MSTEAMS)
})
@Slf4j
public abstract class PmsNotificationChannel {
  List<String> userGroups;

  public abstract NotificationChannel toNotificationChannel(
      String accountId, String templateId, Map<String, String> templateData);
}
