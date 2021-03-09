package io.harness.notification.channelDetails;

import io.harness.Team;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.channeldetails.PagerDutyChannel;

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
@JsonTypeName(NotificationChannelType.PAGERDUTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PmsPagerDutyChannel extends PmsNotificationChannel {
  List<String> userGroups;
  String integrationKey;

  @Override
  public NotificationChannel toNotificationChannel(
      String accountId, String templateId, Map<String, String> templateData) {
    return PagerDutyChannel.builder()
        .accountId(accountId)
        .team(Team.PIPELINE)
        .templateId(templateId)
        .integrationKeys(Lists.newArrayList(integrationKey))
        .templateData(templateData)
        .build();
  }
}
