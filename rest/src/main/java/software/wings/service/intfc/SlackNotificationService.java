package software.wings.service.intfc;

import software.wings.beans.SlackConfig;

/**
 * Created by anubhaw on 12/14/16.
 */
public interface SlackNotificationService {
  /**
   * Send message.
   *
   * @param slackConfig the slack config
   * @param slackChanel the slack chanel
   * @param senderName  the sender name
   * @param message     the message
   */
  void sendMessage(SlackConfig slackConfig, String slackChanel, String senderName, String message);
}
