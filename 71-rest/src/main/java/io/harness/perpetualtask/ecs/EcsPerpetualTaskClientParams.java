package io.harness.perpetualtask.ecs;

import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class EcsPerpetualTaskClientParams implements PerpetualTaskClientParams {
  private String region;
  private String settingId;
  private String clusterName;
  private String clusterId;
}
