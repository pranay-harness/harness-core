/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.channeldetails;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationChannelType {
  public static final String EMAIL = "Email";
  public static final String PAGERDUTY = "PagerDuty";
  public static final String SLACK = "Slack";
  public static final String MSTEAMS = "MsTeams";
}
