package io.harness.delegate.task.aws;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsLoadBalancerDetails {
  private String arn;
  private String name;
  private String type;
  private String dNSName;
  private String scheme;
  private String vpcId;
  private String ipAddressType;
}
