package io.harness.notification.channelDetails;

import io.harness.Team;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.NotificationChannel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(NotificationChannelType.EMAIL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PmsEmailChannel extends PmsNotificationChannel {
  List<String> userGroups;
  List<String> recipients;

  @Override
  public NotificationChannel toNotificationChannel(
      String accountId, String templateId, Map<String, String> templateData) {
    return EmailChannel.builder()
        .accountId(accountId)
        .recipients(recipients)
        .team(Team.PIPELINE)
        .templateData(templateData)
        .templateId(templateId)
        .build();
  }
}
