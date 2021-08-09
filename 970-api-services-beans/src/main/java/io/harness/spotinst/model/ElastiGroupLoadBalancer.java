package io.harness.spotinst.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElastiGroupLoadBalancer {
  private String name;
  private String arn;
  private String type;
}
