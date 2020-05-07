package software.wings.beans.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.data.structure.CollectionUtils;
import lombok.Value;
import software.wings.beans.NotificationChannelType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

@Value
public class NotificationSettings {
  private boolean useIndividualEmails;
  private boolean sendMailToNewMembers;
  @NotNull private List<String> emailAddresses;
  @NotNull private SlackNotificationSetting slackConfig;
  private String pagerDutyIntegrationKey;
  private String microsoftTeamsWebhookUrl;

  @Nonnull
  @JsonIgnore
  public Map<NotificationChannelType, List<String>> getAddressesByChannelType() {
    return Collections.emptyMap();
  }

  @NotNull
  public SlackNotificationSetting getSlackConfig() {
    return slackConfig;
  }

  public List<String> getEmailAddresses() {
    return CollectionUtils.emptyIfNull(emailAddresses);
  }
}
