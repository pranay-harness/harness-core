package software.wings.beans.notification;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public interface SlackNotificationConfiguration {
  /**
   * We just need a webhook URL to send a slack message. So, this is optional.
   */
  @Nullable String getName();

  @NotNull String getOutgoingWebhookUrl();
}
