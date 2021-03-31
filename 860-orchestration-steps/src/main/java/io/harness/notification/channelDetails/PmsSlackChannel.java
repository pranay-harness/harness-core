package io.harness.notification.channelDetails;

import io.harness.Team;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.channeldetails.SlackChannel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.assertj.core.util.Lists;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(NotificationChannelType.SLACK)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PmsSlackChannel extends PmsNotificationChannel {
  List<String> userGroups;
  String webhookUrl;

  @Override
  public NotificationChannel toNotificationChannel(String accountId, String orgIdentifier, String projectIdentifier,
      String templateId, Map<String, String> templateData) {
    return SlackChannel.builder()
        .accountId(accountId)
        .team(Team.PIPELINE)
        .templateData(templateData)
        .templateId(templateId)
        .userGroups(userGroups.stream()
                        .map(e -> NotificationChannelUtils.getUserGroups(e, orgIdentifier, projectIdentifier))
                        .collect(Collectors.toList()))
        .webhookUrls(Lists.newArrayList(webhookUrl))
        .build();
  }
}
