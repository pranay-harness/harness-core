package io.harness.perpetualtask.ecs;

import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.Value;

@Value
public class EcsPerpetualTaskClientParams implements PerpetualTaskClientParams {
  private String region;
  private String settingId;
  private String clusterName;

  public EcsPerpetualTaskClientParams(String region, String settingId, String clusterName) {
    this.region = region;
    this.settingId = settingId;
    this.clusterName = clusterName;
  }
}
