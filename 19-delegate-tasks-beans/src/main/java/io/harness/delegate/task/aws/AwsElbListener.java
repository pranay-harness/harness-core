package io.harness.delegate.task.aws;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AwsElbListener {
  private String listenerArn;
  private String loadBalancerArn;
  private Integer port;
  private String protocol;
  private List<AwsElbListenerRuleData> rules;
}