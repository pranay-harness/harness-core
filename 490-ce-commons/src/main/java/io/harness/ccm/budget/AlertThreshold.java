/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.budget;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CE)
@Data
@Builder
@Schema(description = "A description of a single Alert")
public class AlertThreshold {
  double percentage;
  AlertThresholdBase basedOn;
  String[] emailAddresses;
  String[] userGroupIds; // reference
  String[] slackWebhooks;
  int alertsSent;
  long crossedAt;
}
