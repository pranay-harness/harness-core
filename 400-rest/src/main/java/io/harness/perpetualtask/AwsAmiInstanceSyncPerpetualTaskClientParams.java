package io.harness.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class AwsAmiInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  String appId;
  String inframappingId;
  String asgName;
}
