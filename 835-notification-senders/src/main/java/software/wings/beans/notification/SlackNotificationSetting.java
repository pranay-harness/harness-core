package software.wings.beans.notification;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@Value
@OwnedBy(PL)
public class SlackNotificationSetting implements SlackNotificationConfiguration {
  private String name;
  private String outgoingWebhookUrl;

  public static SlackNotificationSetting emptyConfig() {
    return new SlackNotificationSetting("", "");
  }

  @Override
  public String toString() {
    return "SlackNotificationSetting{"
        + "name='" + name + '\'' + ", outgoingWebhookUrl='"
        + "<redacted-for-security>" + '\'' + '}';
  }
}
