package io.harness.spotinst.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElastiGroupLoadBalancerConfig {
  private String targetGroupName;
  private String targetGroupArn;
  private String type;
}
