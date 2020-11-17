package io.harness.channeldetails;

import io.harness.NotificationRequest;
import io.harness.Team;
import lombok.*;

import java.util.List;
import java.util.Map;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmailChannel extends NotificationChannel {
  List<String> recipients;

  @Builder
  public EmailChannel(String accountId, List<String> userGroupIds, String templateId, Map<String, String> templateData,
      Team team, List<String> recipients) {
    super(accountId, userGroupIds, templateId, templateData, team);
    this.recipients = recipients;
  }

  @Override
  public NotificationRequest buildNotificationRequest() {
    NotificationRequest.Builder builder = NotificationRequest.newBuilder();
    String notificationId = generateUuid();
    return builder.setId(notificationId)
        .setAccountId(accountId)
        .setTeam(team)
        .setEmail(builder.getEmailBuilder()
                      .addAllEmailIds(recipients)
                      .setTemplateId(templateId)
                      .putAllTemplateData(templateData)
                      .addAllUserGroupIds(userGroupIds))
        .build();
  }
}
