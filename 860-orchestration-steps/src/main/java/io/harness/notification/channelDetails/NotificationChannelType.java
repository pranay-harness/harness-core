/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.channelDetails;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationChannelType {
  public final String EMAIL = "Email";
  public final String PAGERDUTY = "PagerDuty";
  public final String SLACK = "Slack";
  public final String MSTEAMS = "MsTeams";
}
