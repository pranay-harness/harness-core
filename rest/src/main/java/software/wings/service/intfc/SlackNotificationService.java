package software.wings.service.intfc;

/**
 * Created by anubhaw on 12/14/16.
 */
public interface SlackNotificationService {
  void sendMessage(String slackConfigId, String slackChanel, String message);
}
