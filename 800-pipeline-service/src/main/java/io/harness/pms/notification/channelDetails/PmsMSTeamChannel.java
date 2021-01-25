package io.harness.pms.notification.channelDetails;

import io.harness.Team;
import io.harness.notification.channeldetails.MSTeamChannel;
import io.harness.notification.channeldetails.NotificationChannel;

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
public class PmsMSTeamChannel extends PmsNotificationChannel {
  List<String> msTeamKeys;

  @Builder
  public PmsMSTeamChannel(List<String> userGroups, List<String> msTeamKeys) {
    super(userGroups);
    this.msTeamKeys = msTeamKeys;
  }

  @Override
  public NotificationChannel toNotificationChannel(
      String accountId, String templateId, Map<String, String> templateData) {
    return MSTeamChannel.builder()
        .msTeamKeys(msTeamKeys)
        .accountId(accountId)
        .team(Team.PIPELINE)
        .templateData(templateData)
        .templateId(templateId)
        .userGroupIds(userGroups)
        .build();
  }
}
