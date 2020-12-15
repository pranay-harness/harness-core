package io.harness.notification.channeldetails;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.NotificationRequest;
import io.harness.Team;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SlackChannel extends NotificationChannel {
  List<String> slackWebHookURLs;

  @Builder
  public SlackChannel(String accountId, List<String> userGroupIds, String templateId, Map<String, String> templateData,
      Team team, List<String> slackWebHookURLs) {
    super(accountId, userGroupIds, templateId, templateData, team);
    this.slackWebHookURLs = slackWebHookURLs;
  }

  @Override
  public NotificationRequest buildNotificationRequest() {
    NotificationRequest.Builder builder = NotificationRequest.newBuilder();
    String notificationId = generateUuid();
    return builder.setId(notificationId)
        .setAccountId(accountId)
        .setTeam(team)
        .setSlack(builder.getSlackBuilder()
                      .addAllSlackWebHookUrls(slackWebHookURLs)
                      .setTemplateId(templateId)
                      .putAllTemplateData(templateData)
                      .addAllUserGroupIds(userGroupIds))
        .build();
  }
}
