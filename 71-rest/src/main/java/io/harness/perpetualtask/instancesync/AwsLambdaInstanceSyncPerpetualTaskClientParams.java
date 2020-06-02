package io.harness.perpetualtask.instancesync;

import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsLambdaInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  String appId;
  String inframappingId;
  String functionName;
  String qualifier;
  String startDate;
}
