package io.harness.spotinst.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElastiGroupLoadBalancerConfig {
  private List<ElastiGroupLoadBalancer> loadBalancers;
}
