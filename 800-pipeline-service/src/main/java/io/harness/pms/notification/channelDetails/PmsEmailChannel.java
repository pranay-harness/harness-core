package io.harness.pms.notification.channelDetails;

import io.harness.Team;
import io.harness.notification.channeldetails.EmailChannel;
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
public class PmsEmailChannel extends PmsNotificationChannel {
  List<String> recipients;

  @Builder
  public PmsEmailChannel(List<String> userGroups, List<String> recipients) {
    super(userGroups);
    this.recipients = recipients;
  }

  @Override
  public NotificationChannel toNotificationChannel(
      String accountId, String templateId, Map<String, String> templateData) {
    return EmailChannel.builder()
        .accountId(accountId)
        .recipients(recipients)
        .team(Team.PIPELINE)
        .templateData(templateData)
        .templateId(templateId)
        .build();
  }
}
