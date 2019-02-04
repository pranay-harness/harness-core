package software.wings.beans.notification;

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
  @NotNull private List<String> emailAddresses;
  @NotNull private SlackNotificationSetting slackConfig;

  @Nonnull
  public Map<NotificationChannelType, List<String>> getAddressesByChannelType() {
    return Collections.emptyMap();
  }

  public @NotNull SlackNotificationSetting getSlackConfig() {
    return slackConfig;
  }

  public List<String> getEmailAddresses() {
    return CollectionUtils.emptyIfNull(emailAddresses);
  }
}
