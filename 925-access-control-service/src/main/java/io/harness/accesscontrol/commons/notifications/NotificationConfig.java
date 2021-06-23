package io.harness.accesscontrol.commons.notifications;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PL)
public class NotificationConfig {
  String slackWebhookUrl;
  String environment;
}
