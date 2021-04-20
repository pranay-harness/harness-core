package io.harness.perpetualtask.instancesync;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.PerpetualTaskClientParams;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class AwsLambdaInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  String appId;
  String inframappingId;
  String functionName;
  String qualifier;
  String startDate;
}
