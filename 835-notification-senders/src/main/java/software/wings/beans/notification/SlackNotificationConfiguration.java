package software.wings.beans.notification;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface SlackNotificationConfiguration {
  /**
   * We just need a webhook URL to send a slack message. So, this is optional.
   */
  @Nullable String getName();

  @NotNull String getOutgoingWebhookUrl();
}
