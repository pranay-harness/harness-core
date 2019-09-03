package io.harness.perpetualtask.ecs;

import lombok.Value;

@Value
public class EcsPerpetualTaskClientParams {
  private String region;
  private String settingId;
  private String clusterName;

  public EcsPerpetualTaskClientParams(String region, String settingId, String clusterName) {
    this.region = region;
    this.settingId = settingId;
    this.clusterName = clusterName;
  }
}
