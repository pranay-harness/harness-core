package software.wings.service.impl;

import com.google.common.base.Strings;
import com.google.inject.Singleton;

import allbegray.slack.SlackClientFactory;
import allbegray.slack.type.Payload;
import allbegray.slack.webhook.SlackWebhookClient;
import software.wings.beans.SlackConfig;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.utils.Validator;

/**
 * Created by anubhaw on 12/14/16.
 */

@Singleton
public class SlackNotificationServiceImpl implements SlackNotificationService {
  @Override
  public void sendMessage(SlackConfig slackConfig, String slackChannel, String senderName, String message) {
    Validator.notNullCheck("Slack Config", slackConfig);

    String webhookUrl = slackConfig.getOutgoingWebhookUrl();

    Payload payload = new Payload();
    payload.setText(message);
    if (!Strings.isNullOrEmpty(slackChannel)) {
      payload.setChannel(slackChannel);
    }
    payload.setUsername(senderName);

    SlackWebhookClient webhookClient = getWebhookClient(webhookUrl);
    webhookClient.post(payload);
  }

  public SlackWebhookClient getWebhookClient(String webhookUrl) {
    return SlackClientFactory.createWebhookClient(webhookUrl);
  }
}
