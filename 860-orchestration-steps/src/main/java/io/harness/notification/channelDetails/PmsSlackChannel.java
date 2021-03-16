package io.harness.notification.channelDetails;

import io.harness.Team;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.channeldetails.SlackChannel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.assertj.core.util.Lists;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(NotificationChannelType.SLACK)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PmsSlackChannel extends PmsNotificationChannel {
  List<String> userGroups;
  String webhookUrl;

  @Override
  public NotificationChannel toNotificationChannel(
      String accountId, String templateId, Map<String, String> templateData) {
    return SlackChannel.builder()
        .accountId(accountId)
        .team(Team.PIPELINE)
        .templateData(templateData)
        .templateId(templateId)
        .userGroupIds(userGroups)
        .webhookUrls(Lists.newArrayList(webhookUrl))
        .build();
  }
}
