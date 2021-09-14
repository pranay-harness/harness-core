/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.budget;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CE)
@Data
@Builder
public class AlertThreshold {
  double percentage;
  AlertThresholdBase basedOn;
  String[] emailAddresses;
  String[] userGroupIds; // reference
  String[] slackWebhooks;
  int alertsSent;
  long crossedAt;
}
