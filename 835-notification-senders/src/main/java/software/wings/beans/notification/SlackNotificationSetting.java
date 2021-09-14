/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.notification;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@Value
@OwnedBy(PL)
public class SlackNotificationSetting implements SlackNotificationConfiguration {
  private String name;
  private String outgoingWebhookUrl;

  public static SlackNotificationSetting emptyConfig() {
    return new SlackNotificationSetting("", "");
  }

  @Override
  public String toString() {
    return "SlackNotificationSetting{"
        + "name='" + name + '\'' + ", outgoingWebhookUrl='"
        + "<redacted-for-security>" + '\'' + '}';
  }
}
