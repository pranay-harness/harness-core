package io.harness.pms.notification.channelDetails;

import io.harness.Team;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.channeldetails.PagerDutyChannel;

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
public class PmsPagerDutyChannel extends PmsNotificationChannel {
  List<String> integrationKeys;

  @Builder
  public PmsPagerDutyChannel(List<String> userGroupIds, List<String> integrationKeys) {
    super(userGroupIds);
    this.integrationKeys = integrationKeys;
  }

  @Override
  public NotificationChannel toNotificationChannel(
      String accountId, String templateId, Map<String, String> templateData) {
    return PagerDutyChannel.builder()
        .accountId(accountId)
        .team(Team.PIPELINE)
        .templateId(templateId)
        .integrationKeys(integrationKeys)
        .templateData(templateData)
        .build();
  }
}
