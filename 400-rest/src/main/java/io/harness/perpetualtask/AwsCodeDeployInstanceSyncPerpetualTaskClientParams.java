package io.harness.perpetualtask;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsCodeDeployInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  String inframmapingId;
  String appId;
}
