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
public class PagerDutyChannel extends NotificationChannel {
  List<String> pagerDutyIntegrationKeys;

  @Builder
  public PagerDutyChannel(String accountId, List<String> userGroupIds, String templateId,
      Map<String, String> templateData, Team team, List<String> pagerDutyIntegrationKeys) {
    super(accountId, userGroupIds, templateId, templateData, team);
    this.pagerDutyIntegrationKeys = pagerDutyIntegrationKeys;
  }

  @Override
  public NotificationRequest buildNotificationRequest() {
    NotificationRequest.Builder builder = NotificationRequest.newBuilder();
    String notificationId = generateUuid();
    return builder.setId(notificationId)
        .setAccountId(accountId)
        .setTeam(team)
        .setPagerDuty(builder.getPagerDutyBuilder()
                          .addAllPagerDutyIntegrationKeys(pagerDutyIntegrationKeys)
                          .setTemplateId(templateId)
                          .putAllTemplateData(templateData)
                          .addAllUserGroupIds(userGroupIds))
        .build();
  }
}
