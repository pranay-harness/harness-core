package io.harness.spotinst.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ElastiGroupLoadBalancerConfig {
  private List<ElastiGroupLoadBalancer> loadBalancers;
}
