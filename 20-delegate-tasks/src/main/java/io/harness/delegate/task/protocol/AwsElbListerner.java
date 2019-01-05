package io.harness.delegate.task.protocol;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsElbListerner {
  private String listenerArn;
  private String loadBalancerArn;
  private Integer port;
  private String protocol;
}
