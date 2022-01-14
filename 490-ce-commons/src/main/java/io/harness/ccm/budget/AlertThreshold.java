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
