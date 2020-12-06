package io.harness.perpetualtask;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsAmiInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  String appId;
  String inframappingId;
  String asgName;
}
