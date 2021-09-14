/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.service.api;

import io.harness.Team;

import java.util.List;
import java.util.Map;

public interface SlackService extends ChannelService {
  boolean send(
      List<String> slackWebhookUrls, String templateId, Map<String, String> templateData, String notificationId);

  boolean send(List<String> slackWebhookUrls, String templateId, Map<String, String> templateData,
      String notificationId, Team team);
}
