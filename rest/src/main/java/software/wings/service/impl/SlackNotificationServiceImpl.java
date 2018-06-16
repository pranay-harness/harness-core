package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static software.wings.common.Constants.ABORTED_COLOR;
import static software.wings.common.Constants.COMPLETED_COLOR;
import static software.wings.common.Constants.FAILED_COLOR;
import static software.wings.common.Constants.PAUSED_COLOR;
import static software.wings.common.Constants.RESUMED_COLOR;
import static software.wings.common.Constants.WHITE_COLOR;

import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;

import allbegray.slack.SlackClientFactory;
import allbegray.slack.type.Attachment;
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

    if (message.contains("||")) {
      Attachment attachment = new Attachment();
      String[] parts = message.split("\\|\\|");
      payload.setText(processText(parts[0]));
      attachment.setText(processText(parts[1]));
      attachment.setFooter(processText(parts[2]));
      attachment.setColor(getColor(parts[3]));
      attachment.setFooter_icon(format("https://s3.amazonaws.com/wings-assets/slackicons/%s.png", parts[3]));
      attachment.setMrkdwn_in(ImmutableList.of("text"));
      payload.setAttachments(ImmutableList.of(attachment));
    } else {
      payload.setText(processText(message));
    }

    if (isNotEmpty(slackChannel)) {
      if (slackChannel.charAt(0) != '#') {
        slackChannel = "#" + slackChannel;
      }
      payload.setChannel(slackChannel);
    }
    payload.setUsername(senderName);
    payload.setIcon_url("https://s3.amazonaws.com/wings-assets/slackicons/logo-slack.png");

    SlackWebhookClient webhookClient = getWebhookClient(webhookUrl);
    webhookClient.post(payload);
  }

  public SlackWebhookClient getWebhookClient(String webhookUrl) {
    return SlackClientFactory.createWebhookClient(webhookUrl);
  }

  private String processText(String message) {
    return message.replaceAll("<<<", "*<")
        .replaceAll("\\|-\\|", "|")
        .replaceAll(">>>", ">*")
        .replaceAll("\\\\\\*", "*")
        .replaceAll("\\|\\*\\|", "\\*");
  }

  private String getColor(String status) {
    switch (status) {
      case "completed":
        return COMPLETED_COLOR;
      case "expired":
      case "rejected":
      case "failed":
        return FAILED_COLOR;
      case "paused":
        return PAUSED_COLOR;
      case "resumed":
        return RESUMED_COLOR;
      case "aborted":
        return ABORTED_COLOR;
      default:
        unhandled(status);
    }
    return WHITE_COLOR;
  }
}
