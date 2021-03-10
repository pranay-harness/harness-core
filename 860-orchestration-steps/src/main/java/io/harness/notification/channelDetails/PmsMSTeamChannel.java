package io.harness.notification.channelDetails;

import io.harness.Team;
import io.harness.notification.channeldetails.MSTeamChannel;
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
@JsonTypeName(NotificationChannelType.MSTEAMS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PmsMSTeamChannel extends PmsNotificationChannel {
  List<String> msTeamKeys;
  List<String> userGroups;

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
