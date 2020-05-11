package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.notification.SlackNotificationConfiguration;

import java.util.List;

/**
 * Created by anubhaw on 12/14/16.
 */
@OwnedBy(CDC)
public interface SlackNotificationService {
  /**
   * Send message.
   *
   * @param slackConfig the slack config
   * @param slackChanel the slack chanel
   * @param senderName  the sender name
   * @param message     the message
   */
  void sendMessage(SlackNotificationConfiguration slackConfig, String slackChanel, String senderName, String message,
      String accountId);

  /**
   * This handled json based messages and not YAML based templates.
   * @param message
   * @param slackWebhooks
   */
  void sendJSONMessage(String message, List<String> slackWebhooks);
}
