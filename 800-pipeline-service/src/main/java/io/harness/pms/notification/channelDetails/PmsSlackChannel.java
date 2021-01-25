package io.harness.pms.notification.channelDetails;

import io.harness.Team;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.channeldetails.SlackChannel;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@EqualsAndHashCode(callSuper = true)
public class PmsSlackChannel extends PmsNotificationChannel {
  List<String> webhookUrls;

  @Builder
  public PmsSlackChannel(List<String> userGroups, List<String> webhookUrls) {
    super(userGroups);
    this.webhookUrls = webhookUrls;
  }

  @Override
  public NotificationChannel toNotificationChannel(
      String accountId, String templateId, Map<String, String> templateData) {
    return SlackChannel.builder()
        .accountId(accountId)
        .team(Team.PIPELINE)
        .templateData(templateData)
        .templateId(templateId)
        .userGroupIds(userGroups)
        .webhookUrls(webhookUrls)
        .build();
  }
}
