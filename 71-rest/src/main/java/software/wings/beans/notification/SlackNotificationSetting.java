package software.wings.beans.notification;

import lombok.Value;

@Value
public class SlackNotificationSetting implements SlackNotificationConfiguration {
  private String name;
  private String outgoingWebhookUrl;

  public static SlackNotificationSetting emptyConfig() {
    return new SlackNotificationSetting("", "");
  }
}
