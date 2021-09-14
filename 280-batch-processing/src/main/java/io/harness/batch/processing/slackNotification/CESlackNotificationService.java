/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.slackNotification;

import software.wings.beans.SlackMessage;
import software.wings.beans.notification.SlackNotificationConfiguration;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.service.intfc.SlackMessageSender;

import com.google.inject.Singleton;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class CESlackNotificationService {
  @Autowired private SlackMessageSender slackMessageSender;

  public static final String SLACK_WEBHOOK_URL_PREFIX = "https://hooks.slack.com/services/";

  public void sendMessage(SlackNotificationConfiguration slackConfig, String slackChannel, String senderName,
      String message, String accountId) {
    if (Objects.requireNonNull(slackConfig, "slack Config can't be null")
            .equals(SlackNotificationSetting.emptyConfig())) {
      return;
    }

    String webhookUrl = slackConfig.getOutgoingWebhookUrl();
    if (StringUtils.isEmpty(webhookUrl)) {
      log.error("Webhook URL is empty. No message will be sent. Config: {}, Message: {}", slackConfig, message);
      return;
    }

    log.info("Sending message for account {} via manager", accountId);
    slackMessageSender.send(
        new SlackMessage(slackConfig.getOutgoingWebhookUrl(), slackChannel, senderName, message), false, false);
  }
}
