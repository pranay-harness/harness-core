package software.wings.infra;

import lombok.Data;

import java.util.List;

@Data
public class AwsAmiInfrastructure implements CloudProviderInfrastructure {
  private String cloudProviderId;
  private String region;
  private String autoScalingGroupName;
  private List<String> classicLoadBalancers;
  private List<String> targetGroupArns;
  private String hostNameConvention;

  // Variables for B/G type Ami deployment
  private List<String> stageClassicLoadBalancers;
  private List<String> stageTargetGroupArns;
}
