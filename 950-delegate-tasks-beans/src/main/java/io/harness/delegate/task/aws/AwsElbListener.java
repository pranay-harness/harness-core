package io.harness.delegate.task.aws;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsElbListener {
  private String listenerArn;
  private String loadBalancerArn;
  private Integer port;
  private String protocol;
  private List<AwsElbListenerRuleData> rules;
}
